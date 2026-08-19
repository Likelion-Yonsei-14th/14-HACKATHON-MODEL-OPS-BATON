package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.EvalDataset;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;

import java.time.LocalDateTime;

public record EvalDatasetResponse(
		Long id,
		String name,
		ModelLabTaskType taskType,
		int version,
		String description,
		LocalDateTime createdAt
) {
	public static EvalDatasetResponse from(EvalDataset d) {
		return new EvalDatasetResponse(d.getId(), d.getName(), d.getTaskType(), d.getVersion(), d.getDescription(), d.getCreatedAt());
	}
}
