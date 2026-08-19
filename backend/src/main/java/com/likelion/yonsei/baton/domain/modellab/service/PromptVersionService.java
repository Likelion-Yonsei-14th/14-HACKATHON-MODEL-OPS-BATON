package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.domain.modellab.entity.AiPromptVersion;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.modellab.repository.AiPromptVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Prompt versions are append-only (spec section 13: "Prompt 변경 이력이 사라지면 안 된다") — there is
 * deliberately no update/delete method here. {@link AiPromptVersion} itself exposes no setters, so
 * even direct entity access could not mutate a persisted row; version numbers auto-increment per
 * task type starting at 1.
 */
@Service
@Transactional(readOnly = true)
public class PromptVersionService {

	private final AiPromptVersionRepository repository;

	public PromptVersionService(AiPromptVersionRepository repository) {
		this.repository = repository;
	}

	public List<AiPromptVersion> list(ModelLabTaskType taskType) {
		return repository.findByTaskTypeOrderByVersionDesc(taskType);
	}

	public AiPromptVersion getById(Long id) {
		return repository.findById(id).orElseThrow(() -> new BusinessException(ModelLabErrorCode.PROMPT_VERSION_NOT_FOUND));
	}

	@Transactional
	public AiPromptVersion create(ModelLabTaskType taskType, String systemPrompt, String developerPromptOrTemplate, String notes, Long createdBy) {
		int nextVersion = repository.findTopByTaskTypeOrderByVersionDesc(taskType)
				.map(v -> v.getVersion() + 1)
				.orElse(1);
		AiPromptVersion version = new AiPromptVersion(taskType, nextVersion, systemPrompt, developerPromptOrTemplate, notes, createdBy);
		return repository.save(version);
	}
}
