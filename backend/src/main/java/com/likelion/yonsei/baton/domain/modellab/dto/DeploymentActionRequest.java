package com.likelion.yonsei.baton.domain.modellab.dto;

/** Body for both promote ({@code targetConfigId} required) and rollback ({@code taskType} required, see controller). */
public record DeploymentActionRequest(
		Long targetConfigId,
		com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType taskType,
		String note
) {
}
