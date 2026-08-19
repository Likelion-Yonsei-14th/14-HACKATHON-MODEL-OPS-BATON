package com.likelion.yonsei.baton.domain.modellab.dto;

public record GenerationHumanReviewRequest(
		Integer coverageScore,
		Integer separationScore,
		Integer granularityScore,
		Integer predecidabilityScore,
		Integer naturalnessScore,
		Integer safetyScore,
		Integer overallScore,
		String note
) {
}
