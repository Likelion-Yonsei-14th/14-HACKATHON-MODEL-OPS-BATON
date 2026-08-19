package com.likelion.yonsei.baton.domain.modellab.repository;

import com.likelion.yonsei.baton.domain.modellab.entity.EvalResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvalResultRepository extends JpaRepository<EvalResult, Long> {

	List<EvalResult> findByRunIdOrderByIdAsc(Long runId);

	List<EvalResult> findByRunIdAndPassedFalseOrderByIdAsc(Long runId);
}
