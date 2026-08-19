package com.likelion.yonsei.baton.domain.modellab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One case-level result within an {@link EvalRun}. reply_case_id is null for Generation runs (spec section 13). */
@Entity
@Table(name = "eval_results")
public class EvalResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "run_id", nullable = false)
	private Long runId;

	@Column(name = "scenario_id", nullable = false)
	private Long scenarioId;

	@Column(name = "reply_case_id")
	private Long replyCaseId;

	@Column(name = "input_snapshot_json", nullable = false, columnDefinition = "TEXT")
	private String inputSnapshotJson;

	@Column(name = "expected_json", columnDefinition = "TEXT")
	private String expectedJson;

	@Column(name = "actual_json", columnDefinition = "TEXT")
	private String actualJson;

	@Column(nullable = false)
	private boolean passed;

	@Column(name = "auto_send_expected")
	private Boolean autoSendExpected;

	@Column(name = "auto_send_actual")
	private Boolean autoSendActual;

	@Column(name = "latency_ms")
	private Long latencyMs;

	@Column(name = "input_tokens")
	private Integer inputTokens;

	@Column(name = "output_tokens")
	private Integer outputTokens;

	@Column(name = "estimated_cost", precision = 10, scale = 6)
	private BigDecimal estimatedCost;

	@Column(name = "error_message", length = 2000)
	private String errorMessage;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected EvalResult() {
	}

	public EvalResult(
			Long runId,
			Long scenarioId,
			Long replyCaseId,
			String inputSnapshotJson,
			String expectedJson,
			String actualJson,
			boolean passed,
			Boolean autoSendExpected,
			Boolean autoSendActual,
			Long latencyMs,
			Integer inputTokens,
			Integer outputTokens,
			BigDecimal estimatedCost,
			String errorMessage
	) {
		this.runId = runId;
		this.scenarioId = scenarioId;
		this.replyCaseId = replyCaseId;
		this.inputSnapshotJson = inputSnapshotJson;
		this.expectedJson = expectedJson;
		this.actualJson = actualJson;
		this.passed = passed;
		this.autoSendExpected = autoSendExpected;
		this.autoSendActual = autoSendActual;
		this.latencyMs = latencyMs;
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
		this.estimatedCost = estimatedCost;
		this.errorMessage = errorMessage;
	}

	public Long getId() {
		return id;
	}

	public Long getRunId() {
		return runId;
	}

	public Long getScenarioId() {
		return scenarioId;
	}

	public Long getReplyCaseId() {
		return replyCaseId;
	}

	public String getInputSnapshotJson() {
		return inputSnapshotJson;
	}

	public String getExpectedJson() {
		return expectedJson;
	}

	public String getActualJson() {
		return actualJson;
	}

	public boolean isPassed() {
		return passed;
	}

	public Boolean getAutoSendExpected() {
		return autoSendExpected;
	}

	public Boolean getAutoSendActual() {
		return autoSendActual;
	}

	public Long getLatencyMs() {
		return latencyMs;
	}

	public Integer getInputTokens() {
		return inputTokens;
	}

	public Integer getOutputTokens() {
		return outputTokens;
	}

	public BigDecimal getEstimatedCost() {
		return estimatedCost;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
