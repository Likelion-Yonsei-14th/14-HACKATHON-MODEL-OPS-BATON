package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.DatasetSplit;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalRun;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalRunStatus;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EvalRunResponse(
		Long id,
		ModelLabTaskType taskType,
		Long datasetId,
		DatasetSplit split,
		Long modelConfigId,
		Long promptVersionId,
		Long schemaVersionId,
		BigDecimal thresholdSnapshot,
		JsonNode modelSnapshot,
		EvalRunStatus status,
		LocalDateTime startedAt,
		LocalDateTime finishedAt,
		JsonNode aggregateMetrics,
		String errorMessage,
		LocalDateTime createdAt
) {
	public static EvalRunResponse from(EvalRun r, ObjectMapper objectMapper) {
		return new EvalRunResponse(
				r.getId(), r.getTaskType(), r.getDatasetId(), r.getSplit(), r.getModelConfigId(), r.getPromptVersionId(),
				r.getSchemaVersionId(), r.getThresholdSnapshot(), readTree(objectMapper, r.getModelSnapshotJson()), r.getStatus(),
				r.getStartedAt(), r.getFinishedAt(), readTree(objectMapper, r.getAggregateMetricsJson()), r.getErrorMessage(), r.getCreatedAt()
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
