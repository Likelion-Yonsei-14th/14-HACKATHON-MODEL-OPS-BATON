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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One executable AI configuration: a (task, provider, model, prompt version, schema version,
 * temperature, threshold) tuple plus a lifecycle status (spec section 13).
 *
 * <p>Mutability rule: only a DRAFT config may be edited via {@link #updateDraft}. Once a config
 * leaves DRAFT (EVALUATING/STAGING/PRODUCTION/ARCHIVED) it is treated as immutable everywhere else
 * in the codebase — {@link ModelConfigService} enforces this by refusing edits and instead cloning
 * a new DRAFT config when the caller wants to change something (spec section 15: "Production
 * config는 immutable하게 취급한다").
 */
@Entity
@Table(name = "ai_model_configs")
public class AiModelConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_type", nullable = false, length = 30)
	private ModelLabTaskType taskType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ModelLabProvider provider;

	@Column(name = "base_model", nullable = false, length = 100)
	private String baseModel;

	@Column(name = "fine_tuned_model_id", length = 150)
	private String fineTunedModelId;

	@Column(name = "prompt_version_id", nullable = false)
	private Long promptVersionId;

	@Column(name = "schema_version_id")
	private Long schemaVersionId;

	@Column(nullable = false, precision = 3, scale = 2)
	private BigDecimal temperature;

	/** Only meaningful for REPLY_CLASSIFICATION configs; null for BRANCH_GENERATION. */
	@Column(name = "confidence_threshold", precision = 4, scale = 3)
	private BigDecimal confidenceThreshold;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ModelConfigStatus status;

	@Column(name = "created_by")
	private Long createdBy;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected AiModelConfig() {
	}

	public AiModelConfig(
			String name,
			ModelLabTaskType taskType,
			ModelLabProvider provider,
			String baseModel,
			String fineTunedModelId,
			Long promptVersionId,
			Long schemaVersionId,
			BigDecimal temperature,
			BigDecimal confidenceThreshold,
			Long createdBy
	) {
		this.name = name;
		this.taskType = taskType;
		this.provider = provider;
		this.baseModel = baseModel;
		this.fineTunedModelId = fineTunedModelId;
		this.promptVersionId = promptVersionId;
		this.schemaVersionId = schemaVersionId;
		this.temperature = temperature;
		this.confidenceThreshold = confidenceThreshold;
		this.status = ModelConfigStatus.DRAFT;
		this.createdBy = createdBy;
	}

	/** Only callable while status is DRAFT — the service layer is responsible for enforcing that. */
	public void updateDraft(
			String name,
			String baseModel,
			String fineTunedModelId,
			Long promptVersionId,
			Long schemaVersionId,
			BigDecimal temperature,
			BigDecimal confidenceThreshold
	) {
		if (name != null) this.name = name;
		if (baseModel != null) this.baseModel = baseModel;
		if (fineTunedModelId != null) this.fineTunedModelId = fineTunedModelId;
		if (promptVersionId != null) this.promptVersionId = promptVersionId;
		if (schemaVersionId != null) this.schemaVersionId = schemaVersionId;
		if (temperature != null) this.temperature = temperature;
		if (confidenceThreshold != null) this.confidenceThreshold = confidenceThreshold;
	}

	public void moveTo(ModelConfigStatus newStatus) {
		this.status = newStatus;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
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

	public String getFineTunedModelId() {
		return fineTunedModelId;
	}

	public Long getPromptVersionId() {
		return promptVersionId;
	}

	public Long getSchemaVersionId() {
		return schemaVersionId;
	}

	public BigDecimal getTemperature() {
		return temperature;
	}

	public BigDecimal getConfidenceThreshold() {
		return confidenceThreshold;
	}

	public ModelConfigStatus getStatus() {
		return status;
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
