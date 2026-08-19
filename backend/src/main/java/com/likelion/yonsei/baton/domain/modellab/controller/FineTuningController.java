package com.likelion.yonsei.baton.domain.modellab.controller;

import com.likelion.yonsei.baton.common.response.ApiResponse;
import com.likelion.yonsei.baton.common.web.CurrentUserId;
import com.likelion.yonsei.baton.domain.modellab.dto.FineTuningJobCreateRequest;
import com.likelion.yonsei.baton.domain.modellab.dto.FineTuningJobResponse;
import com.likelion.yonsei.baton.domain.modellab.service.FineTuningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Phase 5 scaffolding (spec section 16). {@code /submit} deliberately 501s — see FineTuningService. */
@RestController
@RequestMapping("/api/model-lab/fine-tuning-jobs")
public class FineTuningController {

	private final FineTuningService service;

	public FineTuningController(FineTuningService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse<List<FineTuningJobResponse>> list() {
		return ApiResponse.success(service.list().stream().map(FineTuningJobResponse::from).toList());
	}

	@GetMapping("/{id}")
	public ApiResponse<FineTuningJobResponse> get(@PathVariable Long id) {
		return ApiResponse.success(FineTuningJobResponse.from(service.getById(id)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<FineTuningJobResponse> create(@CurrentUserId Long userId, @Valid @RequestBody FineTuningJobCreateRequest request) {
		var job = service.create(request.taskType(), request.provider(), request.baseModel(), request.trainingDatasetId(), userId);
		return ApiResponse.success(FineTuningJobResponse.from(job));
	}

	@PostMapping("/{id}/submit")
	public ApiResponse<FineTuningJobResponse> submit(@PathVariable Long id) {
		return ApiResponse.success(FineTuningJobResponse.from(service.submit(id)));
	}
}
