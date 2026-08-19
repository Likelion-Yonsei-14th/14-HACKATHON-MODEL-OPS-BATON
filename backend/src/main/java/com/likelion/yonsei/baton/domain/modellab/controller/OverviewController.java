package com.likelion.yonsei.baton.domain.modellab.controller;

import com.likelion.yonsei.baton.common.response.ApiResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.DeploymentHistoryResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.EvalRunResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.FineTuningJobResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.ModelConfigResponse;
import com.likelion.yonsei.baton.domain.modellab.dto.OverviewResponse;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;
import com.likelion.yonsei.baton.domain.modellab.repository.EvalRunRepository;
import com.likelion.yonsei.baton.domain.modellab.service.FineTuningService;
import com.likelion.yonsei.baton.domain.modellab.service.ProductionModelRegistryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/** Model Lab landing page data (spec section 21). */
@RestController
@RequestMapping("/api/model-lab/overview")
public class OverviewController {

	private final ProductionModelRegistryService registryService;
	private final EvalRunRepository evalRunRepository;
	private final FineTuningService fineTuningService;
	private final ObjectMapper objectMapper;

	public OverviewController(
			ProductionModelRegistryService registryService,
			EvalRunRepository evalRunRepository,
			FineTuningService fineTuningService,
			ObjectMapper objectMapper
	) {
		this.registryService = registryService;
		this.evalRunRepository = evalRunRepository;
		this.fineTuningService = fineTuningService;
		this.objectMapper = objectMapper;
	}

	@GetMapping
	public ApiResponse<OverviewResponse> get() {
		var response = new OverviewResponse(
				registryService.getProductionConfig(ModelLabTaskType.REPLY_CLASSIFICATION).map(ModelConfigResponse::from).orElse(null),
				registryService.getProductionConfig(ModelLabTaskType.BRANCH_GENERATION).map(ModelConfigResponse::from).orElse(null),
				evalRunRepository.findTop20ByOrderByCreatedAtDesc().stream().limit(10).map(r -> EvalRunResponse.from(r, objectMapper)).toList(),
				fineTuningService.list().stream().limit(5).map(FineTuningJobResponse::from).toList(),
				registryService.history(null).stream().limit(10).map(DeploymentHistoryResponse::from).toList()
		);
		return ApiResponse.success(response);
	}
}
