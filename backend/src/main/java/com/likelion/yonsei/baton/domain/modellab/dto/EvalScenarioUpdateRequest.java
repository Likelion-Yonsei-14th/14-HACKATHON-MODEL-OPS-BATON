package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.DatasetSplit;

public record EvalScenarioUpdateRequest(
		String title,
		DatasetSplit split,
		String question,
		Object context,
		Object tags,
		Object goldenBranches,
		String notes
) {
}
