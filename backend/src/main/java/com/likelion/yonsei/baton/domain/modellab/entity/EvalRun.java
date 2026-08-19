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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One execution of the Eval Runner against a dataset split. Snapshots the model/prompt/threshold
 * actually used at creation time (spec section 13: "Run 당시 설정 Snapshot을 반드시 남긴다") so that a
 * later edit to the referenced ModelConfig — which shouldn't happen once it leaves DRAFT anyway —
 * can never silently change what a historical run means. model_snapshot_json additionally captures
 * the full resolved config (model name, provider, prompt text, schema text, temperature, threshold)
 * as one immutable JSON blob, independent of whether the FK rows themselves stay stable.
 */
@Entity
@Table(name = "eval_runs")
public class EvalRun {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_type", nullable = false, length = 30)
	private ModelLabTaskType taskType;

	@Column(name = "dataset_id", nullable = false)
	private Long datasetId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DatasetSplit split;

	@Column(name = "model_config_id", nullable = false)
	private Long modelConfigId;

	@Column(name = "prompt_version_id", nullable = false)
	private Long promptVersionId;

	@Column(name = "schema_version_id")
	private Long schemaVersionId;

	@Column(name = "threshold_snapshot", precision = 4, scale = 3)
	private BigDecimal thresholdSnapshot;

	@Column(name = "model_snapshot_json", nullable = false, columnDefinition = "TEXT")
	private String modelSnapshotJson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EvalRunStatus status;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "finished_at")
	private LocalDateTime finishedAt;

	@Column(name = "aggregate_metrics_json", columnDefinition = "TEXT")
	private String aggregateMetricsJson;

	@Column(name = "error_message", length = 1000)
	private String errorMessage;

	@Column(name = "created_by")
	private Long createdBy;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected EvalRun() {
	}

	public EvalRun(
			ModelLabTaskType taskType,
			Long datasetId,
			DatasetSplit split,
			Long modelConfigId,
			Long promptVersionId,
			Long schemaVersionId,
			BigDecimal thresholdSnapshot,
			String modelSnapshotJson,
			Long createdBy
	) {
		this.taskType = taskType;
		this.datasetId = datasetId;
		this.split = split;
		this.modelConfigId = modelConfigId;
		this.promptVersionId = promptVersionId;
		this.schemaVersionId = schemaVersionId;
		this.thresholdSnapshot = thresholdSnapshot;
		this.modelSnapshotJson = modelSnapshotJson;
		this.status = EvalRunStatus.PENDING;
		this.createdBy = createdBy;
	}

	public void markRunning() {
		this.status = EvalRunStatus.RUNNING;
		this.startedAt = LocalDateTime.now();
	}

	public void markCompleted(String aggregateMetricsJson) {
		this.status = EvalRunStatus.COMPLETED;
		this.aggregateMetricsJson = aggregateMetricsJson;
		this.finishedAt = LocalDateTime.now();
	}

	public void markFailed(String errorMessage) {
		this.status = EvalRunStatus.FAILED;
		this.errorMessage = errorMessage;
		this.finishedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public ModelLabTaskType getTaskType() {
		return taskType;
	}

	public Long getDatasetId() {
		return datasetId;
	}

	public DatasetSplit getSplit() {
		return split;
	}

	public Long getModelConfigId() {
		return modelConfigId;
	}

	public Long getPromptVersionId() {
		return promptVersionId;
	}

	public Long getSchemaVersionId() {
		return schemaVersionId;
	}

	public BigDecimal getThresholdSnapshot() {
		return thresholdSnapshot;
	}

	public String getModelSnapshotJson() {
		return modelSnapshotJson;
	}

	public EvalRunStatus getStatus() {
		return status;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public LocalDateTime getFinishedAt() {
		return finishedAt;
	}

	public String getAggregateMetricsJson() {
		return aggregateMetricsJson;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
