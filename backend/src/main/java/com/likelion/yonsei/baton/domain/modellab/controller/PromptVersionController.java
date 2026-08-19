package com.likelion.yonsei.baton.domain.modellab.controller;

import com.likelion.yonsei.baton.common.response.ApiResponse;
import com.likelion.yonsei.baton.common.web.CurrentUserId;
import com.likelion.yonsei.baton.domain.modellab.dto.PromptVersionCreateRequest;
import com.likelion.yonsei.baton.domain.modellab.dto.PromptVersionResponse;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.service.PromptVersionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/model-lab/prompt-versions")
public class PromptVersionController {

	private final PromptVersionService service;

	public PromptVersionController(PromptVersionService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse<List<PromptVersionResponse>> list(@RequestParam ModelLabTaskType taskType) {
		return ApiResponse.success(service.list(taskType).stream().map(PromptVersionResponse::from).toList());
	}

	@GetMapping("/{id}")
	public ApiResponse<PromptVersionResponse> get(@PathVariable Long id) {
		return ApiResponse.success(PromptVersionResponse.from(service.getById(id)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<PromptVersionResponse> create(@CurrentUserId Long userId, @Valid @RequestBody PromptVersionCreateRequest request) {
		var created = service.create(request.taskType(), request.systemPrompt(), request.developerPromptOrTemplate(), request.notes(), userId);
		return ApiResponse.success(PromptVersionResponse.from(created));
	}
}
