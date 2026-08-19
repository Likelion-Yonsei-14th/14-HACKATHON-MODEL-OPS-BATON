package com.likelion.yonsei.baton.domain.modellab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Metadata-only record of a fine-tuning job (spec section 13/16). BATON never stores model weights;
 * only the provider's job id, resulting fine-tuned model id, and status/metrics live here. Actually
 * submitting the job to OpenAI is not wired up yet — see FineTuningService for the explicit stub.
 */
@Entity
@Table(name = "fine_tuning_jobs")
public class FineTuningJob {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_type", nullable = false, length = 30)
	private ModelLabTaskType taskType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ModelLabProvider provider;

	@Column(name = "base_model", nullable = false, length = 100)
	private String baseModel;

	@Column(name = "training_dataset_id")
	private Long trainingDatasetId;

	@Column(name = "training_file_ref")
	private String trainingFileRef;

	@Column(name = "provider_job_id")
	private String providerJobId;

	@Column(name = "fine_tuned_model_id")
	private String fineTunedModelId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private FineTuningJobStatus status;

	@Column(name = "metrics_json", columnDefinition = "TEXT")
	private String metricsJson;

	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected FineTuningJob() {
	}

	public FineTuningJob(
			ModelLabTaskType taskType,
			ModelLabProvider provider,
			String baseModel,
			Long trainingDatasetId,
			Long createdBy
	) {
		this.taskType = taskType;
		this.provider = provider;
		this.baseModel = baseModel;
		this.trainingDatasetId = trainingDatasetId;
		this.status = FineTuningJobStatus.NOT_STARTED;
		this.createdBy = createdBy;
	}

	public void markQueued(String trainingFileRef, String providerJobId) {
		this.trainingFileRef = trainingFileRef;
		this.providerJobId = providerJobId;
		this.status = FineTuningJobStatus.QUEUED;
	}

	public void updateStatus(FineTuningJobStatus status, String fineTunedModelId, String metricsJson) {
		this.status = status;
		if (fineTunedModelId != null) this.fineTunedModelId = fineTunedModelId;
		if (metricsJson != null) this.metricsJson = metricsJson;
	}

	public Long getId() {
		return id;
	}

	public ModelLabTaskType getTaskType() {
		return taskType;
	}

	public ModelLabProvider getProvider() {
		return provider;
	}

	public String getBaseModel() {
		return baseModel;
	}

	public Long getTrainingDatasetId() {
		return trainingDatasetId;
	}

	public String getTrainingFileRef() {
		return trainingFileRef;
	}

	public String getProviderJobId() {
		return providerJobId;
	}

	public String getFineTunedModelId() {
		return fineTunedModelId;
	}

	public FineTuningJobStatus getStatus() {
		return status;
	}

	public String getMetricsJson() {
		return metricsJson;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
