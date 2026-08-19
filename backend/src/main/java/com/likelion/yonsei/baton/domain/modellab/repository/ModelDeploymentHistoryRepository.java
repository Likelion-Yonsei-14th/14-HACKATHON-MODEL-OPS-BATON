package com.likelion.yonsei.baton.domain.modellab.repository;

import com.likelion.yonsei.baton.domain.modellab.entity.ModelDeploymentHistory;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelDeploymentHistoryRepository extends JpaRepository<ModelDeploymentHistory, Long> {

	List<ModelDeploymentHistory> findByTaskTypeOrderByCreatedAtDesc(ModelLabTaskType taskType);

	List<ModelDeploymentHistory> findAllByOrderByCreatedAtDesc();
}
