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

import java.time.LocalDateTime;

/** Audit trail entry for a Promote-to-Production or Rollback action (spec section 15). */
@Entity
@Table(name = "model_deployment_history")
public class ModelDeploymentHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_type", nullable = false, length = 30)
	private ModelLabTaskType taskType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DeploymentAction action;

	@Column(name = "from_config_id")
	private Long fromConfigId;

	@Column(name = "to_config_id", nullable = false)
	private Long toConfigId;

	@Column(name = "performed_by", nullable = false)
	private Long performedBy;

	@Column(length = 1000)
	private String note;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected ModelDeploymentHistory() {
	}

	public ModelDeploymentHistory(ModelLabTaskType taskType, DeploymentAction action, Long fromConfigId, Long toConfigId, Long performedBy, String note) {
		this.taskType = taskType;
		this.action = action;
		this.fromConfigId = fromConfigId;
		this.toConfigId = toConfigId;
		this.performedBy = performedBy;
		this.note = note;
	}

	public Long getId() {
		return id;
	}

	public ModelLabTaskType getTaskType() {
		return taskType;
	}

	public DeploymentAction getAction() {
		return action;
	}

	public Long getFromConfigId() {
		return fromConfigId;
	}

	public Long getToConfigId() {
		return toConfigId;
	}

	public Long getPerformedBy() {
		return performedBy;
	}

	public String getNote() {
		return note;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
