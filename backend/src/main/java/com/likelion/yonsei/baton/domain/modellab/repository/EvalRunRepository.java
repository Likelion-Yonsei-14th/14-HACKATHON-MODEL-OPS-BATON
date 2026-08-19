package com.likelion.yonsei.baton.domain.modellab.repository;

import com.likelion.yonsei.baton.domain.modellab.entity.EvalRun;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvalRunRepository extends JpaRepository<EvalRun, Long> {

	List<EvalRun> findByTaskTypeOrderByCreatedAtDesc(ModelLabTaskType taskType);

	List<EvalRun> findTop20ByOrderByCreatedAtDesc();
}
