package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.domain.modellab.entity.AiModelConfig;
import com.likelion.yonsei.baton.domain.modellab.entity.AiPromptVersion;
import com.likelion.yonsei.baton.domain.modellab.entity.DatasetSplit;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalDataset;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalReplyCase;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalResult;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalRun;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalScenario;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabProvider;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalReplyCaseRepository;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalResultRepository;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalRunRepository;
import com.likelion.yonsei.baton.integration.openai.OpenAiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassificationEvalRunnerServiceTest {

	@Mock
	private EvalDatasetService datasetService;
	@Mock
	private ModelConfigService modelConfigService;
	@Mock
	private PromptVersionService promptVersionService;
	@Mock
	private EvalReplyCaseRepository replyCaseRepository;
	@Mock
	private EvalRunRepository evalRunRepository;
	@Mock
	private EvalResultRepository evalResultRepository;
	@Mock
	private OpenAiClient openAiClient;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final Long DATASET_ID = 1L;
	private static final Long MODEL_CONFIG_ID = 2L;
	private static final Long SCENARIO_ID = 3L;
	private static final String GOLDEN_BRANCHES_JSON =
			"[{\"id\":\"1\",\"name\":\"On time\",\"condition_text\":\"c\",\"decision_text\":\"d\",\"response_text\":\"r\"}]";
	private static final String MULTI_MESSAGE_REPLY_JSON = "[\"20일은 조금 어렵고요\",\"아마 27일쯤이면 될 것 같습니다\",\"그런데 QA 환경은 누가 준비하나요?\"]";

	private EvalDataset dataset;
	private EvalScenario scenario;
	private EvalReplyCase replyCase;
	private AiPromptVersion promptVersion;

	@BeforeEach
	void setUp() {
		dataset = new EvalDataset("classification-seed", ModelLabTaskType.REPLY_CLASSIFICATION, 1, null);
		scenario = new EvalScenario(DATASET_ID, "CLS-001", "title", DatasetSplit.CORE, "question?", "[]", "[]", GOLDEN_BRANCHES_JSON, null);
		// Multi-message fixture per spec section 7: the reply is 3 consecutive messages, not one string.
		replyCase = new EvalReplyCase(SCENARIO_ID, MULTI_MESSAGE_REPLY_JSON, "1", false, true, false, false, null, "[\"MULTI_MESSAGE\"]", null);
		promptVersion = new AiPromptVersion(ModelLabTaskType.REPLY_CLASSIFICATION, 1, "system prompt", null, null, 1L);

		when(datasetService.getDataset(DATASET_ID)).thenReturn(dataset);
		when(datasetService.listScenariosForSplit(DATASET_ID, DatasetSplit.CORE)).thenReturn(List.of(scenario));
		when(promptVersionService.getById(1L)).thenReturn(promptVersion);
		when(replyCaseRepository.findByScenarioIdOrderByIdAsc(any())).thenReturn(List.of(replyCase));
		when(evalRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
	}

	private AiModelConfig configWithThreshold(BigDecimal threshold) {
		return new AiModelConfig("CLS-test", ModelLabTaskType.REPLY_CLASSIFICATION, ModelLabProvider.OPENAI, "gpt-4o-mini", null, 1L, null, new BigDecimal("0.2"), threshold, 1L);
	}

	/** confidence=0.85, is_ambiguous=false, contains_new_question=false, etc. — an otherwise clean match. */
	private static final String LLM_RESPONSE_HIGH_CONFIDENCE =
			"{\"selected_branch_id\":\"1\",\"branch_match_confidence\":0.85,\"is_ambiguous\":false,\"contains_new_question\":false,\"contains_out_of_scope_content\":false,\"prompt_injection_suspected\":false,\"result_status\":\"MATCHED\",\"reasoning_summary\":\"ok\"}";

	@Test
	void raisingTheThresholdCanFlipAutoSendActualForTheSameModelOutput() {
		when(modelConfigService.getById(MODEL_CONFIG_ID)).thenReturn(configWithThreshold(new BigDecimal("0.70")));
		when(openAiClient.chatJsonWithConfig(anyString(), anyDouble(), anyString(), anyString()))
				.thenReturn(new OpenAiClient.ChatJsonResult(LLM_RESPONSE_HIGH_CONFIDENCE, 100, 20));

		ClassificationEvalRunnerService runner = new ClassificationEvalRunnerService(
				datasetService, modelConfigService, promptVersionService, replyCaseRepository, evalRunRepository, evalResultRepository, openAiClient, objectMapper);

		ArgumentCaptor<EvalResult> lowThresholdCaptor = ArgumentCaptor.forClass(EvalResult.class);
		runner.run(DATASET_ID, DatasetSplit.CORE, MODEL_CONFIG_ID, 1L);
		org.mockito.Mockito.verify(evalResultRepository).save(lowThresholdCaptor.capture());
		// threshold 0.70 <= confidence 0.85, but this fixture also has expected_new_question=true fed
		// into the golden label — the model output above says contains_new_question=false, so this
		// specific case is scored as a mismatch on that dimension while still being auto-send-actual-eligible.
		assertThat(lowThresholdCaptor.getValue().getAutoSendActual()).isTrue();

		org.mockito.Mockito.reset(evalResultRepository, evalRunRepository);
		when(evalRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(modelConfigService.getById(MODEL_CONFIG_ID)).thenReturn(configWithThreshold(new BigDecimal("0.90")));

		ArgumentCaptor<EvalResult> highThresholdCaptor = ArgumentCaptor.forClass(EvalResult.class);
		runner.run(DATASET_ID, DatasetSplit.CORE, MODEL_CONFIG_ID, 1L);
		org.mockito.Mockito.verify(evalResultRepository).save(highThresholdCaptor.capture());
		// Same model output, but confidence 0.85 < threshold 0.90 now -> never eligible.
		assertThat(highThresholdCaptor.getValue().getAutoSendActual()).isFalse();
	}

	@Test
	void allThreeReplyMessagesAreSentToTheModelAsOneRecipientTurn() {
		when(modelConfigService.getById(MODEL_CONFIG_ID)).thenReturn(configWithThreshold(new BigDecimal("0.70")));
		when(openAiClient.chatJsonWithConfig(anyString(), anyDouble(), anyString(), anyString()))
				.thenReturn(new OpenAiClient.ChatJsonResult(LLM_RESPONSE_HIGH_CONFIDENCE, 100, 20));

		ClassificationEvalRunnerService runner = new ClassificationEvalRunnerService(
				datasetService, modelConfigService, promptVersionService, replyCaseRepository, evalRunRepository, evalResultRepository, openAiClient, objectMapper);
		runner.run(DATASET_ID, DatasetSplit.CORE, MODEL_CONFIG_ID, 1L);

		ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
		org.mockito.Mockito.verify(openAiClient).chatJsonWithConfig(anyString(), anyDouble(), eq("system prompt"), userPromptCaptor.capture());
		String userPrompt = userPromptCaptor.getValue();

		assertThat(userPrompt).contains("20일은 조금 어렵고요", "아마 27일쯤이면 될 것 같습니다", "그런데 QA 환경은 누가 준비하나요?");
	}

	@Test
	void falseAutoSendRateCountsOnlyCasesThatShouldNotHaveAutoSentButDid() {
		// expected_new_question=true means this case must NOT be auto-sendable (spec section 8), but
		// the model output above claims contains_new_question=false, so it wrongly qualifies —
		// this is exactly a False Auto-Send.
		when(modelConfigService.getById(MODEL_CONFIG_ID)).thenReturn(configWithThreshold(new BigDecimal("0.70")));
		when(openAiClient.chatJsonWithConfig(anyString(), anyDouble(), anyString(), anyString()))
				.thenReturn(new OpenAiClient.ChatJsonResult(LLM_RESPONSE_HIGH_CONFIDENCE, 100, 20));

		ClassificationEvalRunnerService runner = new ClassificationEvalRunnerService(
				datasetService, modelConfigService, promptVersionService, replyCaseRepository, evalRunRepository, evalResultRepository, openAiClient, objectMapper);
		EvalRun run = runner.run(DATASET_ID, DatasetSplit.CORE, MODEL_CONFIG_ID, 1L);

		var metrics = objectMapper.readTree(run.getAggregateMetricsJson());
		assertThat(metrics.path("false_auto_send_rate").asDouble()).isEqualTo(1.0);
	}
}
