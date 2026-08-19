package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabProvider;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FineTuningJobCreateRequest(
		@NotNull ModelLabTaskType taskType,
		@NotNull ModelLabProvider provider,
		@NotBlank String baseModel,
		Long trainingDatasetId
) {
}
