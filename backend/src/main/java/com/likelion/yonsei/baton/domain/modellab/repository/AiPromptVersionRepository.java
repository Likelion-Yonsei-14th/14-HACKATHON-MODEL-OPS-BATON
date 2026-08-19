package com.likelion.yonsei.baton.domain.modellab.repository;

import com.likelion.yonsei.baton.domain.modellab.entity.AiPromptVersion;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiPromptVersionRepository extends JpaRepository<AiPromptVersion, Long> {

	List<AiPromptVersion> findByTaskTypeOrderByVersionDesc(ModelLabTaskType taskType);

	Optional<AiPromptVersion> findTopByTaskTypeOrderByVersionDesc(ModelLabTaskType taskType);

	Optional<AiPromptVersion> findByTaskTypeAndVersion(ModelLabTaskType taskType, int version);
}
