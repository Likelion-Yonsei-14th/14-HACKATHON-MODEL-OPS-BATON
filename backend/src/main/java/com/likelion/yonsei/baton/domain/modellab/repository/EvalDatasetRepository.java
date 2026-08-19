package com.likelion.yonsei.baton.domain.modellab.repository;

import com.likelion.yonsei.baton.domain.modellab.entity.EvalDataset;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvalDatasetRepository extends JpaRepository<EvalDataset, Long> {

	List<EvalDataset> findByTaskTypeOrderByCreatedAtDesc(ModelLabTaskType taskType);
}
