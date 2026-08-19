package com.likelion.yonsei.baton.domain.modellab.controller;

import com.likelion.yonsei.baton.common.response.ApiResponse;
import com.likelion.yonsei.baton.common.web.CurrentUserId;
import com.likelion.yonsei.baton.domain.modellab.dto.ModelConfigCreateRequest;
import com.likelion.yonsei.baton.domain.modellab.dto.ModelConfigResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.ModelConfigUpdateRequest;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.service.ModelConfigService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/model-lab/model-configs")
public class ModelConfigController {

	private final ModelConfigService service;

	public ModelConfigController(ModelConfigService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse<List<ModelConfigResponse>> list(@RequestParam ModelLabTaskType taskType) {
		return ApiResponse.success(service.list(taskType).stream().map(ModelConfigResponse::from).toList());
	}

	@GetMapping("/{id}")
	public ApiResponse<ModelConfigResponse> get(@PathVariable Long id) {
		return ApiResponse.success(ModelConfigResponse.from(service.getById(id)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ModelConfigResponse> create(@CurrentUserId Long userId, @Valid @RequestBody ModelConfigCreateRequest request) {
		var created = service.create(
				request.name(), request.taskType(), request.provider(), request.baseModel(), request.fineTunedModelId(),
				request.promptVersionId(), request.schemaVersionId(), request.temperature(), request.confidenceThreshold(), userId
		);
		return ApiResponse.success(ModelConfigResponse.from(created));
	}

	@PatchMapping("/{id}")
	public ApiResponse<ModelConfigResponse> update(@PathVariable Long id, @RequestBody ModelConfigUpdateRequest request) {
		var updated = service.updateDraft(
				id, request.name(), request.baseModel(), request.fineTunedModelId(),
				request.promptVersionId(), request.schemaVersionId(), request.temperature(), request.confidenceThreshold()
		);
		return ApiResponse.success(ModelConfigResponse.from(updated));
	}

	@PostMapping("/{id}/clone")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ModelConfigResponse> clone(@CurrentUserId Long userId, @PathVariable Long id, @RequestParam(required = false) String name) {
		var cloned = service.cloneAsDraft(id, name, userId);
		return ApiResponse.success(ModelConfigResponse.from(cloned));
	}
}
