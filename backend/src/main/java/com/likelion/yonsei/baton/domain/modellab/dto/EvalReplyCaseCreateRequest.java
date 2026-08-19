package com.likelion.yonsei.baton.domain.modellab.dto;

import jakarta.validation.constraints.NotNull;

public record EvalReplyCaseCreateRequest(
		@NotNull Object replyMessages,
		String expectedBranchKey,
		boolean expectedAmbiguous,
		boolean expectedNewQuestion,
		boolean expectedOutOfScope,
		boolean expectedNoMatch,
		Object expectedGuardrail,
		Object tags,
		String notes
) {
}
