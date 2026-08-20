package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.domain.modellab.dto.GoldenBranch;
import com.likelion.yonsei.baton.domain.modellab.entity.AiModelConfig;
import com.likelion.yonsei.baton.domain.modellab.entity.AiPromptVersion;
import com.likelion.yonsei.baton.domain.modellab.entity.DatasetSplit;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalDataset;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalReplyCase;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalResult;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalRun;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalScenario;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalReplyCaseRepository;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalResultRepository;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalRunRepository;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabProvider;
import com.likelion.yonsei.baton.integration.localllm.LocalLlmClient;
import com.likelion.yonsei.baton.integration.openai.OpenAiClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The Classification Eval Runner (spec section 18): loads a dataset split, replays every reply
 * case through the shared {@link OpenAiClient} using an arbitrary {@link AiModelConfig} snapshot,
 * validates the branch id server-side, applies the auto-send guardrail conjunction, and persists
 * per-case {@link EvalResult} rows plus run-level aggregate metrics.
 *
 * <p>Runs synchronously within the HTTP request. Given the seed dataset size (a handful of cases)
 * this is fine for the MVP; a genuinely large (~50 scenario / hundreds of case) dataset would want
 * an async job + polling instead, which is flagged as a follow-up rather than built speculatively.
 */
@Service
public class ClassificationEvalRunnerService {

	/** Rough USD-per-1K-token estimate for cost display only — not wired to real OpenAI billing. */
	private static final BigDecimal INPUT_COST_PER_1K = new BigDecimal("0.00015");
	private static final BigDecimal OUTPUT_COST_PER_1K = new BigDecimal("0.0006");
	private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("0.70");

	private final EvalDatasetService datasetService;
	private final ModelConfigService modelConfigService;
	private final PromptVersionService promptVersionService;
	private final EvalReplyCaseRepository replyCaseRepository;
	private final EvalRunRepository evalRunRepository;
	private final EvalResultRepository evalResultRepository;
	private final OpenAiClient openAiClient;
	private final LocalLlmClient localLlmClient;
	private final ObjectMapper objectMapper;

	public ClassificationEvalRunnerService(
			EvalDatasetService datasetService,
			ModelConfigService modelConfigService,
			PromptVersionService promptVersionService,
			EvalReplyCaseRepository replyCaseRepository,
			EvalRunRepository evalRunRepository,
			EvalResultRepository evalResultRepository,
			OpenAiClient openAiClient,
			LocalLlmClient localLlmClient,
			ObjectMapper objectMapper
	) {
		this.datasetService = datasetService;
		this.modelConfigService = modelConfigService;
		this.promptVersionService = promptVersionService;
		this.replyCaseRepository = replyCaseRepository;
		this.evalRunRepository = evalRunRepository;
		this.evalResultRepository = evalResultRepository;
		this.openAiClient = openAiClient;
		this.localLlmClient = localLlmClient;
		this.objectMapper = objectMapper;
	}

	/**
	 * Routes to whichever {@link ModelLabProvider} the config declares. Ollama's OpenAI-compat API
	 * has no usage accounting, so local runs report null input/output tokens (treated as "unknown",
	 * not zero, by the cost/metrics aggregation below).
	 */
	private OpenAiClient.ChatJsonResult callModel(ModelLabProvider provider, String modelName, Double temperature, String systemPrompt, String userPrompt) {
		if (provider == ModelLabProvider.OLLAMA) {
			String content = localLlmClient.chatJsonWithConfig(modelName, temperature, systemPrompt, userPrompt);
			return new OpenAiClient.ChatJsonResult(content, null, null);
		}
		return openAiClient.chatJsonWithConfig(modelName, temperature, systemPrompt, userPrompt);
	}

	public long previewCaseCount(Long datasetId, DatasetSplit split) {
		return datasetService.previewCaseCount(datasetId, split);
	}

	@Transactional
	public EvalRun run(Long datasetId, DatasetSplit split, Long modelConfigId, Long userId) {
		EvalDataset dataset = datasetService.getDataset(datasetId);
		if (dataset.getTaskType() != ModelLabTaskType.REPLY_CLASSIFICATION) {
			throw new BusinessException(ModelLabErrorCode.TASK_TYPE_MISMATCH);
		}
		AiModelConfig modelConfig = modelConfigService.getById(modelConfigId);
		if (modelConfig.getTaskType() != ModelLabTaskType.REPLY_CLASSIFICATION) {
			throw new BusinessException(ModelLabErrorCode.TASK_TYPE_MISMATCH);
		}
		AiPromptVersion promptVersion = promptVersionService.getById(modelConfig.getPromptVersionId());
		AiPromptVersion stage2Prompt = modelConfig.getStage2PromptVersionId() != null
				? promptVersionService.getById(modelConfig.getStage2PromptVersionId())
				: null;

		List<EvalScenario> scenarios = datasetService.listScenariosForSplit(datasetId, split);
		if (scenarios.isEmpty()) {
			throw new BusinessException(ModelLabErrorCode.EMPTY_DATASET_SPLIT);
		}

		BigDecimal threshold = modelConfig.getConfidenceThreshold() != null ? modelConfig.getConfidenceThreshold() : DEFAULT_THRESHOLD;
		String modelName = modelConfig.getFineTunedModelId() != null ? modelConfig.getFineTunedModelId() : modelConfig.getBaseModel();

		EvalRun run = new EvalRun(
				ModelLabTaskType.REPLY_CLASSIFICATION,
				datasetId,
				split,
				modelConfigId,
				modelConfig.getPromptVersionId(),
				modelConfig.getSchemaVersionId(),
				threshold,
				buildModelSnapshot(modelConfig, promptVersion),
				userId
		);
		run = evalRunRepository.save(run);
		run.markRunning();

		int total = 0;
		int schemaValid = 0;
		int branchExpectedCount = 0;
		int branchCorrect = 0;
		int ambiguousExpectedCount = 0;
		int ambiguousRecalled = 0;
		int newQuestionExpectedCount = 0;
		int newQuestionRecalled = 0;
		int outOfScopeExpectedCount = 0;
		int outOfScopeRecalled = 0;
		int noMatchExpectedCount = 0;
		int noMatchRecalled = 0;
		int autoSendExpectedTrueCount = 0;
		int autoSendExpectedTrueAndActual = 0;
		int autoSendExpectedFalseCount = 0;
		int falseAutoSendCount = 0;
		long latencySumMs = 0;
		long inputTokenSum = 0;
		long outputTokenSum = 0;
		BigDecimal costSum = BigDecimal.ZERO;

		for (EvalScenario scenario : scenarios) {
			List<GoldenBranch> goldenBranches = parseGoldenBranches(scenario.getGoldenBranchesJson());
			List<EvalReplyCase> replyCases = replyCaseRepository.findByScenarioIdOrderByIdAsc(scenario.getId());

			for (EvalReplyCase replyCase : replyCases) {
				total++;
				List<String> replyMessages = parseReplyMessages(replyCase.getReplyMessagesJson());
				String userPrompt = buildUserPrompt(scenario, goldenBranches, replyMessages);

				ObjectNode expectedNode = buildExpectedNode(replyCase);
				boolean autoSendExpected = !replyCase.isExpectedAmbiguous() && !replyCase.isExpectedNewQuestion()
						&& !replyCase.isExpectedOutOfScope() && !replyCase.isExpectedNoMatch()
						&& replyCase.getExpectedBranchKey() != null;
				if (autoSendExpected) autoSendExpectedTrueCount++; else autoSendExpectedFalseCount++;

				long startedAt = System.currentTimeMillis();
				try {
					ClassificationOutcome outcome = stage2Prompt != null
							? classifyTwoStage(modelConfig, modelName, promptVersion, stage2Prompt, userPrompt)
							: classifySingleCall(modelConfig, modelName, promptVersion, userPrompt);
					long latencyMs = System.currentTimeMillis() - startedAt;
					latencySumMs += latencyMs;
					if (outcome.inputTokens() != null) inputTokenSum += outcome.inputTokens();
					if (outcome.outputTokens() != null) outputTokenSum += outcome.outputTokens();
					BigDecimal caseCost = estimateCost(outcome.inputTokens(), outcome.outputTokens());
					costSum = costSum.add(caseCost);
					schemaValid++;

					String selectedBranchKey = outcome.selectedBranchKey();
					BigDecimal confidence = outcome.confidence();
					boolean isAmbiguous = outcome.isAmbiguous();
					boolean containsNewQuestion = outcome.containsNewQuestion();
					boolean containsOutOfScope = outcome.containsOutOfScope();
					boolean promptInjection = outcome.promptInjection();

					// Deterministic correction (docs/QWEN_TUNING.md Follow-up 5): only overrides when every
					// golden branch parses as a clean calendar-date range AND the reply contains an
					// unambiguous date mention — the model's own branch/ambiguity call passes through
					// untouched otherwise. Never turns an ambiguous flag off, only on (a keyword-level date
					// clash the model missed is still a real clash even if it also spotted something else).
					Optional<DateRangeGuardrail.BranchOverride> dateOverride = DateRangeGuardrail.resolve(goldenBranches, replyMessages);
					if (dateOverride.isPresent()) {
						DateRangeGuardrail.BranchOverride ov = dateOverride.get();
						if (ov.ambiguous()) {
							isAmbiguous = true;
							selectedBranchKey = null;
						} else {
							selectedBranchKey = ov.branchKey();
						}
					}
					String finalSelectedBranchKey = selectedBranchKey;
					boolean selectedBranchValid = finalSelectedBranchKey != null
							&& goldenBranches.stream().anyMatch(b -> b.key().equals(finalSelectedBranchKey));

					if (replyCase.isExpectedAmbiguous()) {
						ambiguousExpectedCount++;
						if (isAmbiguous) ambiguousRecalled++;
					}
					if (replyCase.isExpectedNewQuestion()) {
						newQuestionExpectedCount++;
						if (containsNewQuestion) newQuestionRecalled++;
					}
					if (replyCase.isExpectedOutOfScope()) {
						outOfScopeExpectedCount++;
						if (containsOutOfScope) outOfScopeRecalled++;
					}
					if (replyCase.isExpectedNoMatch()) {
						noMatchExpectedCount++;
						if (!selectedBranchValid) noMatchRecalled++;
					}
					boolean branchKeyMatches = Objects.equals(replyCase.getExpectedBranchKey(), selectedBranchValid ? selectedBranchKey : null);
					if (replyCase.getExpectedBranchKey() != null) {
						branchExpectedCount++;
						if (branchKeyMatches) branchCorrect++;
					}

					boolean autoSendActual = AutoSendGuardrail.isAutoSendEligible(
							confidence, threshold, isAmbiguous, containsNewQuestion, containsOutOfScope, promptInjection, selectedBranchValid);
					if (autoSendExpected && autoSendActual) autoSendExpectedTrueAndActual++;
					if (!autoSendExpected && autoSendActual) falseAutoSendCount++;

					boolean passed = branchKeyMatches
							&& isAmbiguous == replyCase.isExpectedAmbiguous()
							&& containsNewQuestion == replyCase.isExpectedNewQuestion()
							&& containsOutOfScope == replyCase.isExpectedOutOfScope();

					EvalResult result = new EvalResult(
							run.getId(), scenario.getId(), replyCase.getId(),
							buildInputSnapshot(scenario, goldenBranches, replyMessages),
							objectMapper.writeValueAsString(expectedNode),
							outcome.rawContent(),
							passed,
							autoSendExpected,
							autoSendActual,
							latencyMs,
							outcome.inputTokens(),
							outcome.outputTokens(),
							caseCost,
							null
					);
					evalResultRepository.save(result);
				} catch (BusinessException e) {
					throw e;
				} catch (Exception e) {
					// Unparseable/errored model output is scored as a schema failure, never as a silent pass —
					// mirrors production's "never auto-execute on malformed AI output" stance.
					long latencyMs = System.currentTimeMillis() - startedAt;
					latencySumMs += latencyMs;
					EvalResult result = new EvalResult(
							run.getId(), scenario.getId(), replyCase.getId(),
							buildInputSnapshot(scenario, goldenBranches, replyMessages),
							objectMapper.writeValueAsString(expectedNode),
							null,
							false,
							autoSendExpected,
							false,
							latencyMs,
							null,
							null,
							BigDecimal.ZERO,
							String.valueOf(e.getMessage())
					);
					evalResultRepository.save(result);
				}
			}
		}

		ObjectNode metrics = objectMapper.createObjectNode();
		metrics.put("total_cases", total);
		metrics.put("schema_validity", ratio(schemaValid, total));
		metrics.put("branch_match_accuracy", ratio(branchCorrect, branchExpectedCount));
		metrics.put("ambiguous_detection_recall", ratio(ambiguousRecalled, ambiguousExpectedCount));
		metrics.put("new_question_detection_recall", ratio(newQuestionRecalled, newQuestionExpectedCount));
		metrics.put("out_of_scope_detection_recall", ratio(outOfScopeRecalled, outOfScopeExpectedCount));
		metrics.put("no_match_detection_recall", ratio(noMatchRecalled, noMatchExpectedCount));
		// The headline metric (spec section 11/35): of cases that must NOT auto-send, what fraction did anyway.
		metrics.put("false_auto_send_rate", ratio(falseAutoSendCount, autoSendExpectedFalseCount));
		metrics.put("auto_send_coverage", ratio(autoSendExpectedTrueAndActual, autoSendExpectedTrueCount));
		metrics.put("average_latency_ms", total > 0 ? latencySumMs / total : 0);
		metrics.put("average_input_tokens", total > 0 ? inputTokenSum / total : 0);
		metrics.put("average_output_tokens", total > 0 ? outputTokenSum / total : 0);
		metrics.put("estimated_cost_total", costSum.setScale(6, RoundingMode.HALF_UP).doubleValue());

		run.markCompleted(objectMapper.writeValueAsString(metrics));
		return run;
	}

	private double ratio(int numerator, int denominator) {
		return denominator == 0 ? 0.0 : Math.round((double) numerator / denominator * 10000.0) / 10000.0;
	}

	private BigDecimal estimateCost(Integer inputTokens, Integer outputTokens) {
		BigDecimal in = inputTokens != null ? INPUT_COST_PER_1K.multiply(BigDecimal.valueOf(inputTokens)).divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP) : BigDecimal.ZERO;
		BigDecimal out = outputTokens != null ? OUTPUT_COST_PER_1K.multiply(BigDecimal.valueOf(outputTokens)).divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP) : BigDecimal.ZERO;
		return in.add(out);
	}

	private String buildModelSnapshot(AiModelConfig config, AiPromptVersion promptVersion) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("model_config_id", config.getId());
		node.put("model_config_name", config.getName());
		node.put("provider", config.getProvider().name());
		node.put("base_model", config.getBaseModel());
		node.put("fine_tuned_model_id", config.getFineTunedModelId());
		node.put("temperature", config.getTemperature());
		node.put("confidence_threshold", config.getConfidenceThreshold());
		node.put("prompt_version_id", promptVersion.getId());
		node.put("prompt_version", promptVersion.getVersion());
		node.put("system_prompt", promptVersion.getSystemPrompt());
		return objectMapper.writeValueAsString(node);
	}

	private ObjectNode buildExpectedNode(EvalReplyCase replyCase) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("expected_branch_key", replyCase.getExpectedBranchKey());
		node.put("expected_ambiguous", replyCase.isExpectedAmbiguous());
		node.put("expected_new_question", replyCase.isExpectedNewQuestion());
		node.put("expected_out_of_scope", replyCase.isExpectedOutOfScope());
		node.put("expected_no_match", replyCase.isExpectedNoMatch());
		return node;
	}

	private String buildInputSnapshot(EvalScenario scenario, List<GoldenBranch> branches, List<String> replyMessages) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("scenario_id", scenario.getId());
		node.put("question", scenario.getQuestion());
		node.putPOJO("golden_branches", branches);
		node.putPOJO("reply_messages", replyMessages);
		return objectMapper.writeValueAsString(node);
	}

	/** Normalized result of classifying one reply case, regardless of whether it took one model call
	 * or two (docs/QWEN_TUNING.md Follow-up 3: task decomposition). {@code rawContent} is whatever
	 * gets stored as the audit trail in {@code eval_results.actual_json} — the single call's raw JSON,
	 * or both stages' JSON concatenated for the two-stage path. */
	private record ClassificationOutcome(
			String rawContent,
			Integer inputTokens,
			Integer outputTokens,
			String selectedBranchKey,
			BigDecimal confidence,
			boolean isAmbiguous,
			boolean containsNewQuestion,
			boolean containsOutOfScope,
			boolean promptInjection
	) {
	}

	private ClassificationOutcome classifySingleCall(AiModelConfig modelConfig, String modelName, AiPromptVersion promptVersion, String userPrompt) {
		OpenAiClient.ChatJsonResult llmResult = callModel(
				modelConfig.getProvider(), modelName, modelConfig.getTemperature().doubleValue(), promptVersion.getSystemPrompt(), userPrompt);
		JsonNode actual = objectMapper.readTree(llmResult.content());

		// Two output schemas are supported: the full multi-boolean v1 shape (selected_branch_id /
		// is_ambiguous / contains_new_question / contains_out_of_scope_content / branch_match_confidence),
		// and a compact single-"state"-enum shape (state / branch_id / confidence) aimed at small
		// local models that reliably lose track of 4+ independent boolean judgments in one call
		// (see docs/QWEN_TUNING.md) — a mutually-exclusive state collapses "is this reply safe to
		// auto-send" into one categorical choice instead. Detected by presence of the "state" key.
		String selectedBranchKey;
		BigDecimal confidence;
		boolean isAmbiguous;
		boolean containsNewQuestion;
		boolean containsOutOfScope;
		boolean promptInjection;
		if (actual.has("state")) {
			String state = actual.path("state").asText("");
			JsonNode branchIdNode = actual.path("branch_id");
			selectedBranchKey = (branchIdNode.isMissingNode() || branchIdNode.isNull()) ? null : branchIdNode.asText();
			confidence = actual.has("confidence") ? new BigDecimal(actual.path("confidence").asText("0")) : null;
			isAmbiguous = "AMBIGUOUS".equals(state);
			containsNewQuestion = "NEW_QUESTION".equals(state);
			containsOutOfScope = "OUT_OF_SCOPE".equals(state);
			promptInjection = false;
			if ("NO_MATCH".equals(state)) {
				selectedBranchKey = null;
			}
		} else {
			JsonNode selectedBranchIdNode = actual.path("selected_branch_id");
			selectedBranchKey = (selectedBranchIdNode.isMissingNode() || selectedBranchIdNode.isNull())
					? null : selectedBranchIdNode.asText();
			confidence = actual.has("branch_match_confidence")
					? new BigDecimal(actual.path("branch_match_confidence").asText("0"))
					: null;
			isAmbiguous = actual.path("is_ambiguous").asBoolean(false);
			containsNewQuestion = actual.path("contains_new_question").asBoolean(false);
			containsOutOfScope = actual.path("contains_out_of_scope_content").asBoolean(false);
			promptInjection = actual.path("prompt_injection_suspected").asBoolean(false);
		}
		return new ClassificationOutcome(llmResult.content(), llmResult.inputTokens(), llmResult.outputTokens(),
				selectedBranchKey, confidence, isAmbiguous, containsNewQuestion, containsOutOfScope, promptInjection);
	}

	/**
	 * Task decomposition (docs/QWEN_TUNING.md Follow-up 3): stage 1 makes ONLY the safety judgment
	 * (SAFE / AMBIGUOUS / NEW_QUESTION / OUT_OF_SCOPE / NO_MATCH, no branch id) using the same
	 * compact-state schema as {@link #classifySingleCall}; stage 2 — fired only when stage 1 said
	 * SAFE — makes ONLY the branch selection, on the hypothesis (from the single-call experiments in
	 * docs/QWEN_TUNING.md) that a small model handles each judgment far more reliably in isolation
	 * than bundled together. If stage 1 says unsafe, stage 2 is skipped entirely — there is no branch
	 * to pick when the reply isn't going to auto-send regardless.
	 */
	private ClassificationOutcome classifyTwoStage(
			AiModelConfig modelConfig, String modelName, AiPromptVersion stage1Prompt, AiPromptVersion stage2Prompt, String userPrompt
	) {
		double temperature = modelConfig.getTemperature().doubleValue();
		OpenAiClient.ChatJsonResult stage1Result = callModel(modelConfig.getProvider(), modelName, temperature, stage1Prompt.getSystemPrompt(), userPrompt);
		JsonNode stage1 = objectMapper.readTree(stage1Result.content());
		String state = stage1.path("state").asText("");
		BigDecimal confidence = stage1.has("confidence") ? new BigDecimal(stage1.path("confidence").asText("0")) : null;
		boolean isAmbiguous = "AMBIGUOUS".equals(state);
		boolean containsNewQuestion = "NEW_QUESTION".equals(state);
		boolean containsOutOfScope = "OUT_OF_SCOPE".equals(state);
		boolean safe = "SAFE_MATCH".equals(state);

		if (!safe) {
			return new ClassificationOutcome(stage1Result.content(), stage1Result.inputTokens(), stage1Result.outputTokens(),
					null, confidence, isAmbiguous, containsNewQuestion, containsOutOfScope, false);
		}

		OpenAiClient.ChatJsonResult stage2Result = callModel(modelConfig.getProvider(), modelName, temperature, stage2Prompt.getSystemPrompt(), userPrompt);
		JsonNode stage2 = objectMapper.readTree(stage2Result.content());
		JsonNode branchIdNode = stage2.path("branch_id");
		String selectedBranchKey = (branchIdNode.isMissingNode() || branchIdNode.isNull()) ? null : branchIdNode.asText();
		if (stage2.has("confidence")) {
			confidence = new BigDecimal(stage2.path("confidence").asText("0"));
		}

		Integer inputTokens = sumNullable(stage1Result.inputTokens(), stage2Result.inputTokens());
		Integer outputTokens = sumNullable(stage1Result.outputTokens(), stage2Result.outputTokens());
		String combinedRaw = "{\"stage1\":" + stage1Result.content() + ",\"stage2\":" + stage2Result.content() + "}";
		return new ClassificationOutcome(combinedRaw, inputTokens, outputTokens,
				selectedBranchKey, confidence, false, false, false, false);
	}

	private Integer sumNullable(Integer a, Integer b) {
		if (a == null && b == null) return null;
		return (a == null ? 0 : a) + (b == null ? 0 : b);
	}

	private String buildUserPrompt(EvalScenario scenario, List<GoldenBranch> branches, List<String> replyMessages) {
		StringBuilder sb = new StringBuilder();
		sb.append("Trigger question: ").append(scenario.getQuestion()).append("\n\n");
		sb.append("Approved branches:\n");
		for (GoldenBranch branch : branches) {
			sb.append("- id=").append(branch.key())
					.append(", name=\"").append(branch.name())
					.append("\", condition=\"").append(branch.conditionText())
					.append("\", decision=\"").append(branch.decisionText())
					.append("\"\n");
		}
		sb.append("\nRecipient reply (as one turn, possibly several consecutive messages):\n");
		for (String message : replyMessages) {
			sb.append("- ").append(message).append('\n');
		}
		return sb.toString();
	}

	private List<GoldenBranch> parseGoldenBranches(String json) {
		List<GoldenBranch> result = new ArrayList<>();
		if (json == null || json.isBlank()) {
			return result;
		}
		JsonNode array = objectMapper.readTree(json);
		for (JsonNode node : array) {
			String key = node.hasNonNull("id") ? node.path("id").asText() : node.path("key").asText();
			result.add(new GoldenBranch(
					key,
					node.path("name").asText(null),
					node.path("condition_text").asText(null),
					node.path("decision_text").asText(null),
					node.path("response_text").asText(null)
			));
		}
		return result;
	}

	private List<String> parseReplyMessages(String json) {
		List<String> result = new ArrayList<>();
		JsonNode array = objectMapper.readTree(json);
		for (JsonNode node : array) {
			result.add(node.asText());
		}
		return result;
	}
}
