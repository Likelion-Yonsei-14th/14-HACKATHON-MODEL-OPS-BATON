package com.likelion.yonsei.baton.domain.modellab.repository;

import com.likelion.yonsei.baton.domain.modellab.entity.AiModelConfig;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelConfigStatus;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiModelConfigRepository extends JpaRepository<AiModelConfig, Long> {

	List<AiModelConfig> findByTaskTypeOrderByCreatedAtDesc(ModelLabTaskType taskType);

	Optional<AiModelConfig> findByTaskTypeAndStatus(ModelLabTaskType taskType, ModelConfigStatus status);
}
