package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.EvalResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EvalResultResponse(
		Long id,
		Long runId,
		Long scenarioId,
		Long replyCaseId,
		JsonNode inputSnapshot,
		JsonNode expected,
		JsonNode actual,
		boolean passed,
		Boolean autoSendExpected,
		Boolean autoSendActual,
		Long latencyMs,
		Integer inputTokens,
		Integer outputTokens,
		BigDecimal estimatedCost,
		String errorMessage,
		LocalDateTime createdAt
) {
	public static EvalResultResponse from(EvalResult r, ObjectMapper objectMapper) {
		return new EvalResultResponse(
				r.getId(), r.getRunId(), r.getScenarioId(), r.getReplyCaseId(),
				readTree(objectMapper, r.getInputSnapshotJson()), readTree(objectMapper, r.getExpectedJson()), readTree(objectMapper, r.getActualJson()),
				r.isPassed(), r.getAutoSendExpected(), r.getAutoSendActual(), r.getLatencyMs(), r.getInputTokens(), r.getOutputTokens(),
				r.getEstimatedCost(), r.getErrorMessage(), r.getCreatedAt()
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
