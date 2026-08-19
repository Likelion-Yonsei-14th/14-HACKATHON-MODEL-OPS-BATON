package com.likelion.yonsei.baton.domain.modellab.controller;

import com.likelion.yonsei.baton.common.response.ApiResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.EvalDatasetResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.EvalReplyCaseCreateRequest;
import com.likelion.yonsei.baton.domain.modellab.dto.EvalReplyCaseResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.EvalScenarioCreateRequest;
import com.likelion.yonsei.baton.domain.modellab.dto.EvalScenarioResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.EvalScenarioUpdateRequest;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.service.EvalDatasetService;
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
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@RestController
@RequestMapping("/api/model-lab")
public class EvalDatasetController {

	private final EvalDatasetService service;
	private final ObjectMapper objectMapper;

	public EvalDatasetController(EvalDatasetService service, ObjectMapper objectMapper) {
		this.service = service;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/datasets")
	public ApiResponse<List<EvalDatasetResponse>> listDatasets(@RequestParam ModelLabTaskType taskType) {
		return ApiResponse.success(service.listDatasets(taskType).stream().map(EvalDatasetResponse::from).toList());
	}

	@GetMapping("/datasets/{id}")
	public ApiResponse<EvalDatasetResponse> getDataset(@PathVariable Long id) {
		return ApiResponse.success(EvalDatasetResponse.from(service.getDataset(id)));
	}

	@PostMapping("/datasets")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<EvalDatasetResponse> createDataset(@RequestParam String name, @RequestParam ModelLabTaskType taskType, @RequestParam(required = false) String description) {
		return ApiResponse.success(EvalDatasetResponse.from(service.createDataset(name, taskType, description)));
	}

	@GetMapping("/datasets/{datasetId}/scenarios")
	public ApiResponse<List<EvalScenarioResponse>> listScenarios(@PathVariable Long datasetId) {
		return ApiResponse.success(service.listScenarios(datasetId).stream().map(s -> EvalScenarioResponse.from(s, objectMapper)).toList());
	}

	@GetMapping("/scenarios/{id}")
	public ApiResponse<EvalScenarioResponse> getScenario(@PathVariable Long id) {
		return ApiResponse.success(EvalScenarioResponse.from(service.getScenario(id), objectMapper));
	}

	@PostMapping("/datasets/{datasetId}/scenarios")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<EvalScenarioResponse> createScenario(@PathVariable Long datasetId, @Valid @RequestBody EvalScenarioCreateRequest request) {
		var created = service.createScenario(
				datasetId, request.externalKey(), request.title(), request.split(), request.question(),
				writeJson(request.context()), writeJson(request.tags()), writeJson(request.goldenBranches()), request.notes()
		);
		return ApiResponse.success(EvalScenarioResponse.from(created, objectMapper));
	}

	@PatchMapping("/scenarios/{id}")
	public ApiResponse<EvalScenarioResponse> updateScenario(@PathVariable Long id, @RequestBody EvalScenarioUpdateRequest request) {
		var updated = service.updateScenario(
				id, request.title(), request.split(), request.question(),
				writeJson(request.context()), writeJson(request.tags()), writeJson(request.goldenBranches()), request.notes()
		);
		return ApiResponse.success(EvalScenarioResponse.from(updated, objectMapper));
	}

	@GetMapping("/scenarios/{scenarioId}/reply-cases")
	public ApiResponse<List<EvalReplyCaseResponse>> listReplyCases(@PathVariable Long scenarioId) {
		return ApiResponse.success(service.listReplyCases(scenarioId).stream().map(c -> EvalReplyCaseResponse.from(c, objectMapper)).toList());
	}

	@PostMapping("/scenarios/{scenarioId}/reply-cases")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<EvalReplyCaseResponse> createReplyCase(@PathVariable Long scenarioId, @Valid @RequestBody EvalReplyCaseCreateRequest request) {
		var created = service.createReplyCase(
				scenarioId, writeJson(request.replyMessages()), request.expectedBranchKey(),
				request.expectedAmbiguous(), request.expectedNewQuestion(), request.expectedOutOfScope(), request.expectedNoMatch(),
				writeJson(request.expectedGuardrail()), writeJson(request.tags()), request.notes()
		);
		return ApiResponse.success(EvalReplyCaseResponse.from(created, objectMapper));
	}

	private String writeJson(Object value) {
		return value != null ? objectMapper.writeValueAsString(value) : null;
	}
}
