package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.domain.modellab.entity.AiModelConfig;
import com.likelion.yonsei.baton.domain.modellab.entity.AiPromptVersion;
import com.likelion.yonsei.baton.domain.modellab.entity.DatasetSplit;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalDataset;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalResult;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalRun;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalScenario;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabProvider;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalResultRepository;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalRunRepository;
import com.likelion.yonsei.baton.integration.localllm.LocalLlmClient;
import com.likelion.yonsei.baton.integration.openai.OpenAiClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Branch Generation Eval Runner (spec section 4.3/18/23). Generates branches for every scenario in
 * a dataset split through the shared {@link OpenAiClient}, applies the automatable hard rules, and
 * stores the raw generation as an {@link EvalResult} awaiting {@link GenerationHumanReview}. This
 * runner never scores generation quality itself — coverage/separation/naturalness/etc. remain a
 * human judgment per spec section 4.3 ("Human Review 결과를 LLM Judge로 대체하지 않는다").
 */
@Service
public class GenerationEvalRunnerService {

	private static final int MAX_REASONABLE_BRANCH_COUNT = 6;

	private final EvalDatasetService datasetService;
	private final ModelConfigService modelConfigService;
	private final PromptVersionService promptVersionService;
	private final EvalRunRepository evalRunRepository;
	private final EvalResultRepository evalResultRepository;
	private final OpenAiClient openAiClient;
	private final LocalLlmClient localLlmClient;
	private final ObjectMapper objectMapper;

	public GenerationEvalRunnerService(
			EvalDatasetService datasetService,
			ModelConfigService modelConfigService,
			PromptVersionService promptVersionService,
			EvalRunRepository evalRunRepository,
			EvalResultRepository evalResultRepository,
			OpenAiClient openAiClient,
			LocalLlmClient localLlmClient,
			ObjectMapper objectMapper
	) {
		this.datasetService = datasetService;
		this.modelConfigService = modelConfigService;
		this.promptVersionService = promptVersionService;
		this.evalRunRepository = evalRunRepository;
		this.evalResultRepository = evalResultRepository;
		this.openAiClient = openAiClient;
		this.localLlmClient = localLlmClient;
		this.objectMapper = objectMapper;
	}

	/** Mirrors ClassificationEvalRunnerService.callModel — same shared-inference-path requirement
	 * (spec section 17), same provider dispatch. */
	private OpenAiClient.ChatJsonResult callModel(ModelLabProvider provider, String modelName, Double temperature, String systemPrompt, String userPrompt) {
		if (provider == ModelLabProvider.OLLAMA) {
			String content = localLlmClient.chatJsonWithConfig(modelName, temperature, systemPrompt, userPrompt);
			return new OpenAiClient.ChatJsonResult(content, null, null);
		}
		return openAiClient.chatJsonWithConfig(modelName, temperature, systemPrompt, userPrompt);
	}

	@Transactional
	public EvalRun run(Long datasetId, DatasetSplit split, Long modelConfigId, Long userId) {
		EvalDataset dataset = datasetService.getDataset(datasetId);
		if (dataset.getTaskType() != ModelLabTaskType.BRANCH_GENERATION) {
			throw new BusinessException(ModelLabErrorCode.TASK_TYPE_MISMATCH);
		}
		AiModelConfig modelConfig = modelConfigService.getById(modelConfigId);
		if (modelConfig.getTaskType() != ModelLabTaskType.BRANCH_GENERATION) {
			throw new BusinessException(ModelLabErrorCode.TASK_TYPE_MISMATCH);
		}
		AiPromptVersion promptVersion = promptVersionService.getById(modelConfig.getPromptVersionId());

		List<EvalScenario> scenarios = datasetService.listScenariosForSplit(datasetId, split);
		if (scenarios.isEmpty()) {
			throw new BusinessException(ModelLabErrorCode.EMPTY_DATASET_SPLIT);
		}

		String modelName = modelConfig.getFineTunedModelId() != null ? modelConfig.getFineTunedModelId() : modelConfig.getBaseModel();

		EvalRun run = new EvalRun(
				ModelLabTaskType.BRANCH_GENERATION, datasetId, split, modelConfigId,
				modelConfig.getPromptVersionId(), modelConfig.getSchemaVersionId(), null,
				buildModelSnapshot(modelConfig, promptVersion), userId
		);
		run = evalRunRepository.save(run);
		run.markRunning();

		int total = 0;
		int hardRulePassed = 0;

		for (EvalScenario scenario : scenarios) {
			total++;
			String userPrompt = buildUserPrompt(scenario);
			ObjectNode inputSnapshot = objectMapper.createObjectNode();
			inputSnapshot.put("scenario_id", scenario.getId());
			inputSnapshot.put("question", scenario.getQuestion());

			long startedAt = System.currentTimeMillis();
			try {
				OpenAiClient.ChatJsonResult llmResult = callModel(
						modelConfig.getProvider(), modelName, modelConfig.getTemperature().doubleValue(), promptVersion.getSystemPrompt(), userPrompt);
				long latencyMs = System.currentTimeMillis() - startedAt;

				HardRuleOutcome outcome = applyHardRules(llmResult.content());
				if (outcome.passed()) hardRulePassed++;

				EvalResult result = new EvalResult(
						run.getId(), scenario.getId(), null,
						objectMapper.writeValueAsString(inputSnapshot),
						scenario.getGoldenBranchesJson(),
						llmResult.content(),
						outcome.passed(),
						null, null,
						latencyMs, llmResult.inputTokens(), llmResult.outputTokens(),
						BigDecimal.ZERO,
						outcome.passed() ? null : outcome.failureReason()
				);
				evalResultRepository.save(result);
			} catch (Exception e) {
				long latencyMs = System.currentTimeMillis() - startedAt;
				EvalResult result = new EvalResult(
						run.getId(), scenario.getId(), null,
						objectMapper.writeValueAsString(inputSnapshot),
						scenario.getGoldenBranchesJson(),
						null, false, null, null,
						latencyMs, null, null, BigDecimal.ZERO, String.valueOf(e.getMessage())
				);
				evalResultRepository.save(result);
			}
		}

		ObjectNode metrics = objectMapper.createObjectNode();
		metrics.put("total_scenarios", total);
		metrics.put("hard_rule_pass_rate", total == 0 ? 0.0 : Math.round((double) hardRulePassed / total * 10000.0) / 10000.0);
		run.markCompleted(objectMapper.writeValueAsString(metrics));
		return run;
	}

	private record HardRuleOutcome(boolean passed, String failureReason) {
	}

	/** Spec section 4.3's automatable hard rules — not a substitute for human review, just a first-pass sanity filter. */
	private HardRuleOutcome applyHardRules(String content) {
		JsonNode root;
		try {
			root = objectMapper.readTree(content);
		} catch (Exception e) {
			return new HardRuleOutcome(false, "JSON schema invalid: " + e.getMessage());
		}
		JsonNode branches = root.path("branches");
		if (!branches.isArray() || branches.isEmpty()) {
			return new HardRuleOutcome(false, "Branch 0개");
		}
		if (branches.size() > MAX_REASONABLE_BRANCH_COUNT) {
			return new HardRuleOutcome(false, "Branch 수 과도 (" + branches.size() + ")");
		}
		Set<String> seenNames = new HashSet<>();
		for (JsonNode branch : branches) {
			String name = branch.path("name").asText(null);
			if (name == null || name.isBlank()) {
				return new HardRuleOutcome(false, "Branch name 누락");
			}
			if (!seenNames.add(name)) {
				return new HardRuleOutcome(false, "중복 Branch name: " + name);
			}
			if (isBlank(branch.path("condition_text"))) {
				return new HardRuleOutcome(false, "condition_text 누락 (" + name + ")");
			}
			if (isBlank(branch.path("decision_text"))) {
				return new HardRuleOutcome(false, "decision_text 누락 (" + name + ")");
			}
		}
		return new HardRuleOutcome(true, null);
	}

	private boolean isBlank(JsonNode node) {
		return node.isMissingNode() || node.isNull() || node.asText("").isBlank();
	}

	private String buildModelSnapshot(AiModelConfig config, AiPromptVersion promptVersion) {
		ObjectNode node = objectMapper.createObjectNode();
		node.put("model_config_id", config.getId());
		node.put("model_config_name", config.getName());
		node.put("provider", config.getProvider().name());
		node.put("base_model", config.getBaseModel());
		node.put("fine_tuned_model_id", config.getFineTunedModelId());
		node.put("temperature", config.getTemperature());
		node.put("prompt_version_id", promptVersion.getId());
		node.put("prompt_version", promptVersion.getVersion());
		node.put("system_prompt", promptVersion.getSystemPrompt());
		return objectMapper.writeValueAsString(node);
	}

	private String buildUserPrompt(EvalScenario scenario) {
		StringBuilder sb = new StringBuilder();
		sb.append("Message the user is about to send: ").append(scenario.getQuestion()).append('\n');
		if (scenario.getContextJson() != null) {
			sb.append("Context: ").append(scenario.getContextJson()).append('\n');
		}
		return sb.toString();
	}
}
