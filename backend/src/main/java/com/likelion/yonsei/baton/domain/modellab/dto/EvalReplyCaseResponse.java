package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.EvalReplyCase;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public record EvalReplyCaseResponse(
		Long id,
		Long scenarioId,
		JsonNode replyMessages,
		String expectedBranchKey,
		boolean expectedAmbiguous,
		boolean expectedNewQuestion,
		boolean expectedOutOfScope,
		boolean expectedNoMatch,
		JsonNode expectedGuardrail,
		JsonNode tags,
		String notes,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static EvalReplyCaseResponse from(EvalReplyCase c, ObjectMapper objectMapper) {
		return new EvalReplyCaseResponse(
				c.getId(), c.getScenarioId(), readTree(objectMapper, c.getReplyMessagesJson()), c.getExpectedBranchKey(),
				c.isExpectedAmbiguous(), c.isExpectedNewQuestion(), c.isExpectedOutOfScope(), c.isExpectedNoMatch(),
				readTree(objectMapper, c.getExpectedGuardrailJson()), readTree(objectMapper, c.getTagsJson()),
				c.getNotes(), c.getCreatedAt(), c.getUpdatedAt()
		);
	}

	private static JsonNode readTree(ObjectMapper objectMapper, String json) {
		if (json == null) return null;
		try {
			return objectMapper.readTree(json);
		} catch (Exception e) {
			return null;
		}
	}
}
