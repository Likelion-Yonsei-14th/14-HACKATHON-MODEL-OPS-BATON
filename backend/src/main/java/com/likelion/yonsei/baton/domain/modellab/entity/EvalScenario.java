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
 * One evaluation unit (spec section 6): a trigger question, its golden branches (JSON, Classification)
 * or reference branches (JSON, Generation), and — for Classification — a set of {@link EvalReplyCase}
 * rows hanging off it. golden_branches_json uses small integer/string "local branch keys" scoped to
 * the scenario, not production Branch primary keys (spec section 13 guidance), so a dataset is fully
 * self-contained and never depends on the production branches table.
 */
@Entity
@Table(name = "eval_scenarios")
public class EvalScenario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "dataset_id", nullable = false)
	private Long datasetId;

	@Column(name = "external_key", nullable = false, length = 100)
	private String externalKey;

	@Column(nullable = false)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DatasetSplit split;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String question;

	@Column(name = "context_json", columnDefinition = "TEXT")
	private String contextJson;

	@Column(name = "tags_json", columnDefinition = "TEXT")
	private String tagsJson;

	@Column(name = "golden_branches_json", columnDefinition = "TEXT")
	private String goldenBranchesJson;

	@Column(length = 1000)
	private String notes;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected EvalScenario() {
	}

	public EvalScenario(
			Long datasetId,
			String externalKey,
			String title,
			DatasetSplit split,
			String question,
			String contextJson,
			String tagsJson,
			String goldenBranchesJson,
			String notes
	) {
		this.datasetId = datasetId;
		this.externalKey = externalKey;
		this.title = title;
		this.split = split;
		this.question = question;
		this.contextJson = contextJson;
		this.tagsJson = tagsJson;
		this.goldenBranchesJson = goldenBranchesJson;
		this.notes = notes;
	}

	public void update(String title, DatasetSplit split, String question, String contextJson, String tagsJson, String goldenBranchesJson, String notes) {
		if (title != null) this.title = title;
		if (split != null) this.split = split;
		if (question != null) this.question = question;
		if (contextJson != null) this.contextJson = contextJson;
		if (tagsJson != null) this.tagsJson = tagsJson;
		if (goldenBranchesJson != null) this.goldenBranchesJson = goldenBranchesJson;
		if (notes != null) this.notes = notes;
	}

	public Long getId() {
		return id;
	}

	public Long getDatasetId() {
		return datasetId;
	}

	public String getExternalKey() {
		return externalKey;
	}

	public String getTitle() {
		return title;
	}

	public DatasetSplit getSplit() {
		return split;
	}

	public String getQuestion() {
		return question;
	}

	public String getContextJson() {
		return contextJson;
	}

	public String getTagsJson() {
		return tagsJson;
	}

	public String getGoldenBranchesJson() {
		return goldenBranchesJson;
	}

	public String getNotes() {
		return notes;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
