package com.likelion.yonsei.baton.domain.modellab.controller;

import com.likelion.yonsei.baton.common.response.ApiResponse;
import com.likelion.yonsei.baton.common.web.CurrentUserId;
import com.likelion.yonsei.baton.domain.modellab.dto.GenerationHumanReviewRequest;
import com.likelion.yonsei.baton.domain.modellab.dto.GenerationHumanReviewResponse;
import com.likelion.yonsei.baton.domain.modellab.service.GenerationHumanReviewService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/model-lab/eval-results/{evalResultId}/review")
public class GenerationHumanReviewController {

	private final GenerationHumanReviewService service;

	public GenerationHumanReviewController(GenerationHumanReviewService service) {
		this.service = service;
	}

	@PostMapping
	public ApiResponse<GenerationHumanReviewResponse> upsert(
			@CurrentUserId Long userId,
			@PathVariable Long evalResultId,
			@RequestBody GenerationHumanReviewRequest request
	) {
		var review = service.upsert(
				evalResultId, request.coverageScore(), request.separationScore(), request.granularityScore(),
				request.predecidabilityScore(), request.naturalnessScore(), request.safetyScore(), request.overallScore(),
				request.note(), userId
		);
		return ApiResponse.success(GenerationHumanReviewResponse.from(review));
	}
}
