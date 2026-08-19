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

/**
 * An immutable, versioned system prompt for one task type. Never updated after creation — a new
 * prompt is always a new row with the next version number (spec section 13: "Prompt 변경 이력이
 * 사라지면 안 된다"). No setters are exposed on purpose.
 */
@Entity
@Table(name = "ai_prompt_versions")
public class AiPromptVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_type", nullable = false, length = 30)
	private ModelLabTaskType taskType;

	@Column(nullable = false)
	private int version;

	@Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
	private String systemPrompt;

	@Column(name = "developer_prompt_or_template", columnDefinition = "TEXT")
	private String developerPromptOrTemplate;

	@Column(length = 1000)
	private String notes;

	@Column(name = "created_by")
	private Long createdBy;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected AiPromptVersion() {
	}

	public AiPromptVersion(
			ModelLabTaskType taskType,
			int version,
			String systemPrompt,
			String developerPromptOrTemplate,
			String notes,
			Long createdBy
	) {
		this.taskType = taskType;
		this.version = version;
		this.systemPrompt = systemPrompt;
		this.developerPromptOrTemplate = developerPromptOrTemplate;
		this.notes = notes;
		this.createdBy = createdBy;
	}

	public Long getId() {
		return id;
	}

	public ModelLabTaskType getTaskType() {
		return taskType;
	}

	public int getVersion() {
		return version;
	}

	public String getSystemPrompt() {
		return systemPrompt;
	}

	public String getDeveloperPromptOrTemplate() {
		return developerPromptOrTemplate;
	}

	public String getNotes() {
		return notes;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
