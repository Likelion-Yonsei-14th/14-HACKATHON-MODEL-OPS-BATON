package com.likelion.yonsei.baton.domain.modellab.repository;

import com.likelion.yonsei.baton.domain.modellab.entity.EvalReplyCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvalReplyCaseRepository extends JpaRepository<EvalReplyCase, Long> {

	List<EvalReplyCase> findByScenarioIdOrderByIdAsc(Long scenarioId);

	List<EvalReplyCase> findByScenarioIdIn(List<Long> scenarioIds);

	long countByScenarioIdIn(List<Long> scenarioIds);

	void deleteByScenarioId(Long scenarioId);
}
