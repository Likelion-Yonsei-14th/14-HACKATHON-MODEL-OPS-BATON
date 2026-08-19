package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.DeploymentAction;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelDeploymentHistory;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;

import java.time.LocalDateTime;

public record DeploymentHistoryResponse(
		Long id,
		ModelLabTaskType taskType,
		DeploymentAction action,
		Long fromConfigId,
		Long toConfigId,
		Long performedBy,
		String note,
		LocalDateTime createdAt
) {
	public static DeploymentHistoryResponse from(ModelDeploymentHistory h) {
		return new DeploymentHistoryResponse(h.getId(), h.getTaskType(), h.getAction(), h.getFromConfigId(), h.getToConfigId(), h.getPerformedBy(), h.getNote(), h.getCreatedAt());
	}
}
