package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.DatasetSplit;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalScenario;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public record EvalScenarioResponse(
		Long id,
		Long datasetId,
		String externalKey,
		String title,
		DatasetSplit split,
		String question,
		JsonNode context,
		JsonNode tags,
		JsonNode goldenBranches,
		String notes,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static EvalScenarioResponse from(EvalScenario s, ObjectMapper objectMapper) {
		return new EvalScenarioResponse(
				s.getId(), s.getDatasetId(), s.getExternalKey(), s.getTitle(), s.getSplit(), s.getQuestion(),
				readTree(objectMapper, s.getContextJson()), readTree(objectMapper, s.getTagsJson()),
				readTree(objectMapper, s.getGoldenBranchesJson()), s.getNotes(), s.getCreatedAt(), s.getUpdatedAt()
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
