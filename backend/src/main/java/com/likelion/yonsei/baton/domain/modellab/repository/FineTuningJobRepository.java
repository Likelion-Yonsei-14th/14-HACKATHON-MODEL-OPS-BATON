package com.likelion.yonsei.baton.domain.modellab.repository;

import com.likelion.yonsei.baton.domain.modellab.entity.FineTuningJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FineTuningJobRepository extends JpaRepository<FineTuningJob, Long> {

	List<FineTuningJob> findAllByOrderByCreatedAtDesc();
}
