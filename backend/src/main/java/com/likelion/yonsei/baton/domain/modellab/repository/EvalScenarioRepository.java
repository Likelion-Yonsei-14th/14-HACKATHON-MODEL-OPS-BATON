package com.likelion.yonsei.baton.domain.modellab.repository;

import com.likelion.yonsei.baton.domain.modellab.entity.DatasetSplit;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalScenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvalScenarioRepository extends JpaRepository<EvalScenario, Long> {

	List<EvalScenario> findByDatasetIdOrderByIdAsc(Long datasetId);

	List<EvalScenario> findByDatasetIdAndSplitOrderByIdAsc(Long datasetId, DatasetSplit split);

	long countByDatasetId(Long datasetId);
}
