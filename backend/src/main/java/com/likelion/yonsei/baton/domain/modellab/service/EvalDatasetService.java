package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.domain.modellab.entity.DatasetSplit;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalDataset;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalReplyCase;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalScenario;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalDatasetRepository;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalReplyCaseRepository;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD for Dataset / Scenario / Reply Case (spec section 24). Datasets themselves are edited in place (no immutability requirement in the spec for datasets, only for prompts/configs). */
@Service
@Transactional(readOnly = true)
public class EvalDatasetService {

	private final EvalDatasetRepository datasetRepository;
	private final EvalScenarioRepository scenarioRepository;
	private final EvalReplyCaseRepository replyCaseRepository;

	public EvalDatasetService(EvalDatasetRepository datasetRepository, EvalScenarioRepository scenarioRepository, EvalReplyCaseRepository replyCaseRepository) {
		this.datasetRepository = datasetRepository;
		this.scenarioRepository = scenarioRepository;
		this.replyCaseRepository = replyCaseRepository;
	}

	public List<EvalDataset> listDatasets(ModelLabTaskType taskType) {
		return datasetRepository.findByTaskTypeOrderByCreatedAtDesc(taskType);
	}

	public EvalDataset getDataset(Long id) {
		return datasetRepository.findById(id).orElseThrow(() -> new BusinessException(ModelLabErrorCode.DATASET_NOT_FOUND));
	}

	@Transactional
	public EvalDataset createDataset(String name, ModelLabTaskType taskType, String description) {
		return datasetRepository.save(new EvalDataset(name, taskType, 1, description));
	}

	public List<EvalScenario> listScenarios(Long datasetId) {
		getDataset(datasetId);
		return scenarioRepository.findByDatasetIdOrderByIdAsc(datasetId);
	}

	public List<EvalScenario> listScenariosForSplit(Long datasetId, DatasetSplit split) {
		return scenarioRepository.findByDatasetIdAndSplitOrderByIdAsc(datasetId, split);
	}

	public EvalScenario getScenario(Long id) {
		return scenarioRepository.findById(id).orElseThrow(() -> new BusinessException(ModelLabErrorCode.SCENARIO_NOT_FOUND));
	}

	@Transactional
	public EvalScenario createScenario(
			Long datasetId, String externalKey, String title, DatasetSplit split, String question,
			String contextJson, String tagsJson, String goldenBranchesJson, String notes
	) {
		getDataset(datasetId);
		EvalScenario scenario = new EvalScenario(datasetId, externalKey, title, split, question, contextJson, tagsJson, goldenBranchesJson, notes);
		return scenarioRepository.save(scenario);
	}

	@Transactional
	public EvalScenario updateScenario(
			Long id, String title, DatasetSplit split, String question,
			String contextJson, String tagsJson, String goldenBranchesJson, String notes
	) {
		EvalScenario scenario = getScenario(id);
		scenario.update(title, split, question, contextJson, tagsJson, goldenBranchesJson, notes);
		return scenario;
	}

	public List<EvalReplyCase> listReplyCases(Long scenarioId) {
		getScenario(scenarioId);
		return replyCaseRepository.findByScenarioIdOrderByIdAsc(scenarioId);
	}

	@Transactional
	public EvalReplyCase createReplyCase(
			Long scenarioId, String replyMessagesJson, String expectedBranchKey,
			boolean expectedAmbiguous, boolean expectedNewQuestion, boolean expectedOutOfScope, boolean expectedNoMatch,
			String expectedGuardrailJson, String tagsJson, String notes
	) {
		getScenario(scenarioId);
		EvalReplyCase replyCase = new EvalReplyCase(
				scenarioId, replyMessagesJson, expectedBranchKey, expectedAmbiguous, expectedNewQuestion,
				expectedOutOfScope, expectedNoMatch, expectedGuardrailJson, tagsJson, notes
		);
		return replyCaseRepository.save(replyCase);
	}

	/** Case count a Classification eval run for this dataset+split would actually execute — used for the pre-run cost preview (spec section 37). */
	public long previewCaseCount(Long datasetId, DatasetSplit split) {
		List<Long> scenarioIds = listScenariosForSplit(datasetId, split).stream().map(EvalScenario::getId).toList();
		if (scenarioIds.isEmpty()) {
			return 0;
		}
		return replyCaseRepository.countByScenarioIdIn(scenarioIds);
	}
}
