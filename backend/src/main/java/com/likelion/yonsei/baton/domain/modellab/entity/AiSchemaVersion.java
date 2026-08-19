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

/** An immutable, versioned JSON Schema for a task's structured LLM output (spec section 9/13). */
@Entity
@Table(name = "ai_schema_versions")
public class AiSchemaVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "task_type", nullable = false, length = 30)
	private ModelLabTaskType taskType;

	@Column(nullable = false)
	private int version;

	@Column(name = "json_schema", nullable = false, columnDefinition = "TEXT")
	private String jsonSchema;

	@Column(length = 1000)
	private String notes;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected AiSchemaVersion() {
	}

	public AiSchemaVersion(ModelLabTaskType taskType, int version, String jsonSchema, String notes) {
		this.taskType = taskType;
		this.version = version;
		this.jsonSchema = jsonSchema;
		this.notes = notes;
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

	public String getJsonSchema() {
		return jsonSchema;
	}

	public String getNotes() {
		return notes;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
