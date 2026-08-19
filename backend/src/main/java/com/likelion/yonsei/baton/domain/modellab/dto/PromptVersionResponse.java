package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.AiPromptVersion;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;

import java.time.LocalDateTime;

public record PromptVersionResponse(
		Long id,
		ModelLabTaskType taskType,
		int version,
		String systemPrompt,
		String developerPromptOrTemplate,
		String notes,
		Long createdBy,
		LocalDateTime createdAt
) {
	public static PromptVersionResponse from(AiPromptVersion v) {
		return new PromptVersionResponse(v.getId(), v.getTaskType(), v.getVersion(), v.getSystemPrompt(), v.getDeveloperPromptOrTemplate(), v.getNotes(), v.getCreatedBy(), v.getCreatedAt());
	}
}
