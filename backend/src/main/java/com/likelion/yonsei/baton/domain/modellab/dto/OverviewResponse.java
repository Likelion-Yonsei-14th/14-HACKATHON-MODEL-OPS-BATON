package com.likelion.yonsei.baton.domain.modellab.dto;

import java.util.List;

public record OverviewResponse(
		ModelConfigResponse productionClassificationConfig,
		ModelConfigResponse productionGenerationConfig,
		List<EvalRunResponse> recentEvalRuns,
		List<FineTuningJobResponse> recentFineTuningJobs,
		List<DeploymentHistoryResponse> recentDeployments
) {
}
