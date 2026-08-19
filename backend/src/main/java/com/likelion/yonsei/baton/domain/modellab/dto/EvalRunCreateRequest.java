package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.DatasetSplit;
import jakarta.validation.constraints.NotNull;

public record EvalRunCreateRequest(
		@NotNull Long datasetId,
		@NotNull DatasetSplit split,
		@NotNull Long modelConfigId
) {
}
