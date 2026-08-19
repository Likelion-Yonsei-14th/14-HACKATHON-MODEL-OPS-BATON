package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.domain.modellab.entity.AiSchemaVersion;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.modellab.repository.AiSchemaVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Structured-output JSON Schema versions, append-only for the same reason as prompt versions. */
@Service
@Transactional(readOnly = true)
public class SchemaVersionService {

	private final AiSchemaVersionRepository repository;

	public SchemaVersionService(AiSchemaVersionRepository repository) {
		this.repository = repository;
	}

	public List<AiSchemaVersion> list(ModelLabTaskType taskType) {
		return repository.findByTaskTypeOrderByVersionDesc(taskType);
	}

	public AiSchemaVersion getById(Long id) {
		return repository.findById(id).orElseThrow(() -> new BusinessException(ModelLabErrorCode.SCHEMA_VERSION_NOT_FOUND));
	}

	@Transactional
	public AiSchemaVersion create(ModelLabTaskType taskType, String jsonSchema, String notes) {
		int nextVersion = repository.findTopByTaskTypeOrderByVersionDesc(taskType)
				.map(v -> v.getVersion() + 1)
				.orElse(1);
		return repository.save(new AiSchemaVersion(taskType, nextVersion, jsonSchema, notes));
	}
}
