package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.AiModelConfig;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelConfigStatus;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabProvider;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ModelConfigResponse(
		Long id,
		String name,
		ModelLabTaskType taskType,
		ModelLabProvider provider,
		String baseModel,
		String fineTunedModelId,
		Long promptVersionId,
		Long schemaVersionId,
		BigDecimal temperature,
		BigDecimal confidenceThreshold,
		ModelConfigStatus status,
		Long createdBy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static ModelConfigResponse from(AiModelConfig c) {
		return new ModelConfigResponse(
				c.getId(), c.getName(), c.getTaskType(), c.getProvider(), c.getBaseModel(), c.getFineTunedModelId(),
				c.getPromptVersionId(), c.getSchemaVersionId(), c.getTemperature(), c.getConfidenceThreshold(),
				c.getStatus(), c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt()
		);
	}
}
