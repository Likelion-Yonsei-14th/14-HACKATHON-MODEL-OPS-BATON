package com.likelion.yonsei.baton.domain.modellab.repository;

import com.likelion.yonsei.baton.domain.modellab.entity.GenerationHumanReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenerationHumanReviewRepository extends JpaRepository<GenerationHumanReview, Long> {

	Optional<GenerationHumanReview> findByEvalResultId(Long evalResultId);
}
