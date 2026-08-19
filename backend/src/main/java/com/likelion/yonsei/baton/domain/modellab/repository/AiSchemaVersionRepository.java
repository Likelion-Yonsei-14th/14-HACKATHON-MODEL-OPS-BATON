package com.likelion.yonsei.baton.domain.modellab.repository;

import com.likelion.yonsei.baton.domain.modellab.entity.AiSchemaVersion;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiSchemaVersionRepository extends JpaRepository<AiSchemaVersion, Long> {

	List<AiSchemaVersion> findByTaskTypeOrderByVersionDesc(ModelLabTaskType taskType);

	Optional<AiSchemaVersion> findTopByTaskTypeOrderByVersionDesc(ModelLabTaskType taskType);
}
