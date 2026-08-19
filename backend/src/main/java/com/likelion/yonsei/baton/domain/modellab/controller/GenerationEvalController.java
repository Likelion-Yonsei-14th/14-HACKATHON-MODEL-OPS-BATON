package com.likelion.yonsei.baton.domain.modellab.controller;

import com.likelion.yonsei.baton.common.exception.BusinessException;
import com.likelion.yonsei.baton.common.response.ApiResponse;
import com.likelion.yonsei.baton.common.web.CurrentUserId;
import com.likelion.yonsei.baton.domain.modellab.dto.EvalResultResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.EvalRunCreateRequest;
import com.likelion.yonsei.baton.domain.modellab.dto.EvalRunResponse;
import com.likelion.yonsei.baton.domain.modellab.entity.EvalRun;
import com.likelion.yonsei.baton.domain.modellab.exception.ModelLabErrorCode;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalResultRepository;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalRunRepository;
import com.likelion.yonsei.baton.domain.modellab.service.GenerationEvalRunnerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Branch Generation Eval Runner endpoints (spec section 18/23) — phase 3. */
@RestController
@RequestMapping("/api/model-lab/generation-eval-runs")
public class GenerationEvalController {

	private final GenerationEvalRunnerService runnerService;
	private final EvalRunRepository evalRunRepository;
	private final EvalResultRepository evalResultRepository;
	private final ObjectMapper objectMapper;

	public GenerationEvalController(
			GenerationEvalRunnerService runnerService,
			EvalRunRepository evalRunRepository,
			EvalResultRepository evalResultRepository,
			ObjectMapper objectMapper
	) {
		this.runnerService = runnerService;
		this.evalRunRepository = evalRunRepository;
		this.evalResultRepository = evalResultRepository;
		this.objectMapper = objectMapper;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<EvalRunResponse> run(@CurrentUserId Long userId, @Valid @RequestBody EvalRunCreateRequest request) {
		EvalRun run = runnerService.run(request.datasetId(), request.split(), request.modelConfigId(), userId);
		return ApiResponse.success(EvalRunResponse.from(run, objectMapper));
	}

	@GetMapping("/{id}")
	public ApiResponse<EvalRunResponse> get(@PathVariable Long id) {
		EvalRun run = evalRunRepository.findById(id).orElseThrow(() -> new BusinessException(ModelLabErrorCode.EVAL_RUN_NOT_FOUND));
		return ApiResponse.success(EvalRunResponse.from(run, objectMapper));
	}

	@GetMapping("/{id}/results")
	public ApiResponse<List<EvalResultResponse>> results(@PathVariable Long id) {
		return ApiResponse.success(evalResultRepository.findByRunIdOrderByIdAsc(id).stream().map(r -> EvalResultResponse.from(r, objectMapper)).toList());
	}
}
