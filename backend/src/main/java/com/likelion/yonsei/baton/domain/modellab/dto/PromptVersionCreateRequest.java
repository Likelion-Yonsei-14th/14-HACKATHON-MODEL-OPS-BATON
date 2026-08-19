package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PromptVersionCreateRequest(
		@NotNull ModelLabTaskType taskType,
		@NotBlank String systemPrompt,
		String developerPromptOrTemplate,
		String notes
) {
}
