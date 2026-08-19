package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.DatasetSplit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvalScenarioCreateRequest(
		@NotBlank String externalKey,
		@NotBlank String title,
		@NotNull DatasetSplit split,
		@NotBlank String question,
		Object context,
		Object tags,
		Object goldenBranches,
		String notes
) {
}
