package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.AiSchemaVersion;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;

import java.time.LocalDateTime;

public record SchemaVersionResponse(
		Long id,
		ModelLabTaskType taskType,
		int version,
		String jsonSchema,
		String notes,
		LocalDateTime createdAt
) {
	public static SchemaVersionResponse from(AiSchemaVersion v) {
		return new SchemaVersionResponse(v.getId(), v.getTaskType(), v.getVersion(), v.getJsonSchema(), v.getNotes(), v.getCreatedAt());
	}
}
