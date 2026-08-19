package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabProvider;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ModelConfigCreateRequest(
		@NotBlank String name,
		@NotNull ModelLabTaskType taskType,
		@NotNull ModelLabProvider provider,
		@NotBlank String baseModel,
		String fineTunedModelId,
		@NotNull Long promptVersionId,
		Long schemaVersionId,
		@NotNull BigDecimal temperature,
		BigDecimal confidenceThreshold
) {
}
