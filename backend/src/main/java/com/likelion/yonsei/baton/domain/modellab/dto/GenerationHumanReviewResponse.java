package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.GenerationHumanReview;

import java.time.LocalDateTime;

public record GenerationHumanReviewResponse(
		Long id,
		Long evalResultId,
		Integer coverageScore,
		Integer separationScore,
		Integer granularityScore,
		Integer predecidabilityScore,
		Integer naturalnessScore,
		Integer safetyScore,
		Integer overallScore,
		String note,
		Long reviewerId,
		LocalDateTime createdAt
) {
	public static GenerationHumanReviewResponse from(GenerationHumanReview r) {
		return new GenerationHumanReviewResponse(
				r.getId(), r.getEvalResultId(), r.getCoverageScore(), r.getSeparationScore(), r.getGranularityScore(),
				r.getPredecidabilityScore(), r.getNaturalnessScore(), r.getSafetyScore(), r.getOverallScore(),
				r.getNote(), r.getReviewerId(), r.getCreatedAt()
		);
	}
}
