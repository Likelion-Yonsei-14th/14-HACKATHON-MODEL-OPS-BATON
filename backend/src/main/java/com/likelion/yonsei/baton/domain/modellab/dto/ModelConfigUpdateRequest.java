package com.likelion.yonsei.baton.domain.modellab.dto;

import java.math.BigDecimal;

/** Only applicable while the target config is still DRAFT (service enforces). */
public record ModelConfigUpdateRequest(
		String name,
		String baseModel,
		String fineTunedModelId,
		Long promptVersionId,
		Long schemaVersionId,
		BigDecimal temperature,
		BigDecimal confidenceThreshold
) {
}
