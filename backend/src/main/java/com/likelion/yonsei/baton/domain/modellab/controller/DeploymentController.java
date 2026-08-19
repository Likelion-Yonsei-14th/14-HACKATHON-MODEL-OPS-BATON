package com.likelion.yonsei.baton.domain.modellab.controller;

import com.likelion.yonsei.baton.common.response.ApiResponse;
import com.likelion.yonsei.baton.common.web.CurrentUserId;
import com.likelion.yonsei.baton.domain.modellab.dto.DeploymentActionRequest;
import com.likelion.yonsei.baton.domain.modellab.dto.DeploymentHistoryResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.ModelConfigResponse;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.service.ProductionModelRegistryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Promote-to-Production / Rollback (spec section 15/26). */
@RestController
@RequestMapping("/api/model-lab/deployment")
public class DeploymentController {

	private final ProductionModelRegistryService registryService;

	public DeploymentController(ProductionModelRegistryService registryService) {
		this.registryService = registryService;
	}

	@GetMapping("/production")
	public ApiResponse<ModelConfigResponse> currentProduction(@RequestParam ModelLabTaskType taskType) {
		return registryService.getProductionConfig(taskType)
				.map(c -> ApiResponse.success(ModelConfigResponse.from(c)))
				.orElse(ApiResponse.success(null));
	}

	@GetMapping("/history")
	public ApiResponse<List<DeploymentHistoryResponse>> history(@RequestParam(required = false) ModelLabTaskType taskType) {
		return ApiResponse.success(registryService.history(taskType).stream().map(DeploymentHistoryResponse::from).toList());
	}

	@PostMapping("/promote")
	public ApiResponse<DeploymentHistoryResponse> promote(@CurrentUserId Long userId, @RequestBody DeploymentActionRequest request) {
		var entry = registryService.promote(request.targetConfigId(), userId, request.note());
		return ApiResponse.success(DeploymentHistoryResponse.from(entry));
	}

	@PostMapping("/rollback")
	public ApiResponse<DeploymentHistoryResponse> rollback(@CurrentUserId Long userId, @RequestBody DeploymentActionRequest request) {
		var entry = registryService.rollback(request.taskType(), userId, request.note());
		return ApiResponse.success(DeploymentHistoryResponse.from(entry));
	}
}
