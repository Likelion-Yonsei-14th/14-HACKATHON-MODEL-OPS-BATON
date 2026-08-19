package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.domain.modellab.entity.AiModelConfig;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelConfigStatus;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabProvider;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.modellab.repository.AiModelConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * CRUD + lifecycle for {@link AiModelConfig}. Edits are only ever allowed while status is DRAFT
 * (spec section 13/15); anything else must go through {@code ProductionModelRegistryService} for
 * promotion, or be cloned into a fresh DRAFT via {@link #cloneAsDraft}.
 */
@Service
@Transactional(readOnly = true)
public class ModelConfigService {

	private final AiModelConfigRepository repository;

	public ModelConfigService(AiModelConfigRepository repository) {
		this.repository = repository;
	}

	public List<AiModelConfig> list(ModelLabTaskType taskType) {
		return repository.findByTaskTypeOrderByCreatedAtDesc(taskType);
	}

	public AiModelConfig getById(Long id) {
		return repository.findById(id).orElseThrow(() -> new BusinessException(ModelLabErrorCode.MODEL_CONFIG_NOT_FOUND));
	}

	@Transactional
	public AiModelConfig create(
			String name,
			ModelLabTaskType taskType,
			ModelLabProvider provider,
			String baseModel,
			String fineTunedModelId,
			Long promptVersionId,
			Long schemaVersionId,
			BigDecimal temperature,
			BigDecimal confidenceThreshold,
			Long createdBy
	) {
		AiModelConfig config = new AiModelConfig(
				name, taskType, provider, baseModel, fineTunedModelId,
				promptVersionId, schemaVersionId, temperature, confidenceThreshold, createdBy
		);
		return repository.save(config);
	}

	@Transactional
	public AiModelConfig updateDraft(
			Long id,
			String name,
			String baseModel,
			String fineTunedModelId,
			Long promptVersionId,
			Long schemaVersionId,
			BigDecimal temperature,
			BigDecimal confidenceThreshold
	) {
		AiModelConfig config = getById(id);
		if (config.getStatus() != ModelConfigStatus.DRAFT) {
			throw new BusinessException(ModelLabErrorCode.MODEL_CONFIG_NOT_DRAFT);
		}
		config.updateDraft(name, baseModel, fineTunedModelId, promptVersionId, schemaVersionId, temperature, confidenceThreshold);
		return config;
	}

	/** Creates a new DRAFT config seeded from an existing one's values — the only sanctioned way to "edit" a non-DRAFT config. */
	@Transactional
	public AiModelConfig cloneAsDraft(Long sourceId, String newName, Long createdBy) {
		AiModelConfig source = getById(sourceId);
		AiModelConfig clone = new AiModelConfig(
				newName != null ? newName : source.getName() + " (copy)",
				source.getTaskType(),
				source.getProvider(),
				source.getBaseModel(),
				source.getFineTunedModelId(),
				source.getPromptVersionId(),
				source.getSchemaVersionId(),
				source.getTemperature(),
				source.getConfidenceThreshold(),
				createdBy
		);
		return repository.save(clone);
	}

	@Transactional
	public void moveTo(Long id, ModelConfigStatus status) {
		getById(id).moveTo(status);
	}
}
