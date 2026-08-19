package com.likelion.yonsei.baton.domain.modellab.dto;

import com.likelion.yonsei.baton.domain.modellab.entity.FineTuningJob;
import com.likelion.yonsei.baton.domain.modellab.entity.FineTuningJobStatus;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabProvider;
import com.likelion.yonsei.baton.domain.modellab.entity.ModelLabTaskType;

import java.time.LocalDateTime;

public record FineTuningJobResponse(
		Long id,
		ModelLabTaskType taskType,
		ModelLabProvider provider,
		String baseModel,
		Long trainingDatasetId,
		String trainingFileRef,
		String providerJobId,
		String fineTunedModelId,
		FineTuningJobStatus status,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static FineTuningJobResponse from(FineTuningJob j) {
		return new FineTuningJobResponse(
				j.getId(), j.getTaskType(), j.getProvider(), j.getBaseModel(), j.getTrainingDatasetId(),
				j.getTrainingFileRef(), j.getProviderJobId(), j.getFineTunedModelId(), j.getStatus(), j.getCreatedAt(), j.getUpdatedAt()
		);
	}
}
