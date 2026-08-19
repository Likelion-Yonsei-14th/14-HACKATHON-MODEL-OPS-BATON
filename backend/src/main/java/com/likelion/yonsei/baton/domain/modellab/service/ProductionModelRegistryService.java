package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.domain.modellab.entity.AiModelConfig;
import com.likelion.yonsei.baton.domain.modellab.entity.DeploymentAction;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelConfigStatus;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelDeploymentHistory;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.modellab.repository.AiModelConfigRepository;
import com.likelion.yonsei.baton.domain.modellab.repository.ModelDeploymentHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The seam production BATON code is meant to call instead of hardcoding a model/prompt/threshold
 * (spec section 14): {@code getProductionConfig(REPLY_CLASSIFICATION)} /
 * {@code getProductionConfig(BRANCH_GENERATION)}. Also owns Promote/Rollback (spec section 15),
 * which is the only place allowed to flip a config's status to/from PRODUCTION.
 *
 * <p>Caching: a plain {@link ConcurrentHashMap} keyed by task type, invalidated synchronously inside
 * the same transaction as promote/rollback. This is intentionally not a distributed cache (spec:
 * "성능을 위해 DB 조회 결과를 캐싱할 수 있다... 없어도 무방") — Model Lab runs as a single instance for now.
 *
 * <p>Note: production BATON services (BranchGenerationService / ClassificationService) do not yet
 * call this registry — they remain hardwired to the LlmRouter/OpenAiProperties model as before this
 * change (see task description's own instruction not to rewrite Slack/Execution flows). Wiring
 * production inference to actually consult getProductionConfig(...) is flagged as a follow-up in the
 * final report; this service is complete and ready for that integration.
 */
@Service
public class ProductionModelRegistryService {

	private final AiModelConfigRepository modelConfigRepository;
	private final ModelDeploymentHistoryRepository deploymentHistoryRepository;
	private final ConcurrentHashMap<ModelLabTaskType, AiModelConfig> productionCache = new ConcurrentHashMap<>();

	public ProductionModelRegistryService(AiModelConfigRepository modelConfigRepository, ModelDeploymentHistoryRepository deploymentHistoryRepository) {
		this.modelConfigRepository = modelConfigRepository;
		this.deploymentHistoryRepository = deploymentHistoryRepository;
	}

	@Transactional(readOnly = true)
	public Optional<AiModelConfig> getProductionConfig(ModelLabTaskType taskType) {
		AiModelConfig cached = productionCache.get(taskType);
		if (cached != null) {
			return Optional.of(cached);
		}
		Optional<AiModelConfig> fromDb = modelConfigRepository.findByTaskTypeAndStatus(taskType, ModelConfigStatus.PRODUCTION);
		fromDb.ifPresent(config -> productionCache.put(taskType, config));
		return fromDb;
	}

	public List<ModelDeploymentHistory> history(ModelLabTaskType taskType) {
		return taskType != null
				? deploymentHistoryRepository.findByTaskTypeOrderByCreatedAtDesc(taskType)
				: deploymentHistoryRepository.findAllByOrderByCreatedAtDesc();
	}

	@Transactional
	public synchronized ModelDeploymentHistory promote(Long targetConfigId, Long performedBy, String note) {
		AiModelConfig target = modelConfigRepository.findById(targetConfigId)
				.orElseThrow(() -> new BusinessException(ModelLabErrorCode.MODEL_CONFIG_NOT_FOUND));

		Optional<AiModelConfig> current = modelConfigRepository.findByTaskTypeAndStatus(target.getTaskType(), ModelConfigStatus.PRODUCTION);
		current.ifPresent(c -> c.moveTo(ModelConfigStatus.ARCHIVED));
		target.moveTo(ModelConfigStatus.PRODUCTION);

		ModelDeploymentHistory entry = new ModelDeploymentHistory(
				target.getTaskType(),
				DeploymentAction.PROMOTE,
				current.map(AiModelConfig::getId).orElse(null),
				target.getId(),
				performedBy,
				note
		);
		deploymentHistoryRepository.save(entry);
		productionCache.put(target.getTaskType(), target);
		return entry;
	}

	@Transactional
	public synchronized ModelDeploymentHistory rollback(ModelLabTaskType taskType, Long performedBy, String note) {
		AiModelConfig current = modelConfigRepository.findByTaskTypeAndStatus(taskType, ModelConfigStatus.PRODUCTION)
				.orElseThrow(() -> new BusinessException(ModelLabErrorCode.NO_PRODUCTION_CONFIG));

		// The previous production config is whatever the promotion that installed `current` demoted.
		Long previousConfigId = deploymentHistoryRepository.findByTaskTypeOrderByCreatedAtDesc(taskType).stream()
				.filter(h -> h.getAction() == DeploymentAction.PROMOTE && h.getToConfigId().equals(current.getId()))
				.findFirst()
				.map(ModelDeploymentHistory::getFromConfigId)
				.orElse(null);

		if (previousConfigId == null) {
			throw new BusinessException(ModelLabErrorCode.NO_PREVIOUS_PRODUCTION_CONFIG);
		}

		AiModelConfig previous = modelConfigRepository.findById(previousConfigId)
				.orElseThrow(() -> new BusinessException(ModelLabErrorCode.NO_PREVIOUS_PRODUCTION_CONFIG));

		current.moveTo(ModelConfigStatus.ARCHIVED);
		previous.moveTo(ModelConfigStatus.PRODUCTION);

		ModelDeploymentHistory entry = new ModelDeploymentHistory(
				taskType, DeploymentAction.ROLLBACK, current.getId(), previous.getId(), performedBy, note
		);
		deploymentHistoryRepository.save(entry);
		productionCache.put(taskType, previous);
		return entry;
	}
}
