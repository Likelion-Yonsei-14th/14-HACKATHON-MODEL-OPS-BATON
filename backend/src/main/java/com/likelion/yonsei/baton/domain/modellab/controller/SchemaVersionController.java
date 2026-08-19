package com.likelion.yonsei.baton.domain.modellab.controller;

import com.likelion.yonsei.baton.common.response.ApiResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.SchemaVersionCreateRequest;
import com.likelion.yonsei.baton.domain.modellab.dto.SchemaVersionResponse;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.service.SchemaVersionService;
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
@RequestMapping("/api/model-lab/schema-versions")
public class SchemaVersionController {

	private final SchemaVersionService service;

	public SchemaVersionController(SchemaVersionService service) {
		this.service = service;
	}

	@GetMapping
	public ApiResponse<List<SchemaVersionResponse>> list(@RequestParam ModelLabTaskType taskType) {
		return ApiResponse.success(service.list(taskType).stream().map(SchemaVersionResponse::from).toList());
	}

	@GetMapping("/{id}")
	public ApiResponse<SchemaVersionResponse> get(@PathVariable Long id) {
		return ApiResponse.success(SchemaVersionResponse.from(service.getById(id)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SchemaVersionResponse> create(@Valid @RequestBody SchemaVersionCreateRequest request) {
		var created = service.create(request.taskType(), request.jsonSchema(), request.notes());
		return ApiResponse.success(SchemaVersionResponse.from(created));
	}
}
