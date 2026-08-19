package com.likelion.yonsei.baton.domain.modellab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A human's 1-5 scoring of one Generation eval result (spec section 4.3/23). Scores are nullable —
 * a reviewer can save partial progress — but the MVP never substitutes an LLM judge for this.
 */
@Entity
@Table(name = "generation_human_reviews")
public class GenerationHumanReview {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "eval_result_id", nullable = false)
	private Long evalResultId;

	@Column(name = "coverage_score")
	private Integer coverageScore;

	@Column(name = "separation_score")
	private Integer separationScore;

	@Column(name = "granularity_score")
	private Integer granularityScore;

	@Column(name = "predecidability_score")
	private Integer predecidabilityScore;

	@Column(name = "naturalness_score")
	private Integer naturalnessScore;

	@Column(name = "safety_score")
	private Integer safetyScore;

	@Column(name = "overall_score")
	private Integer overallScore;

	@Column(length = 2000)
	private String note;

	@Column(name = "reviewer_id", nullable = false)
	private Long reviewerId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected GenerationHumanReview() {
	}

	public GenerationHumanReview(
			Long evalResultId,
			Integer coverageScore,
			Integer separationScore,
			Integer granularityScore,
			Integer predecidabilityScore,
			Integer naturalnessScore,
			Integer safetyScore,
			Integer overallScore,
			String note,
			Long reviewerId
	) {
		this.evalResultId = evalResultId;
		this.coverageScore = coverageScore;
		this.separationScore = separationScore;
		this.granularityScore = granularityScore;
		this.predecidabilityScore = predecidabilityScore;
		this.naturalnessScore = naturalnessScore;
		this.safetyScore = safetyScore;
		this.overallScore = overallScore;
		this.note = note;
		this.reviewerId = reviewerId;
	}

	public void update(
			Integer coverageScore,
			Integer separationScore,
			Integer granularityScore,
			Integer predecidabilityScore,
			Integer naturalnessScore,
			Integer safetyScore,
			Integer overallScore,
			String note
	) {
		if (coverageScore != null) this.coverageScore = coverageScore;
		if (separationScore != null) this.separationScore = separationScore;
		if (granularityScore != null) this.granularityScore = granularityScore;
		if (predecidabilityScore != null) this.predecidabilityScore = predecidabilityScore;
		if (naturalnessScore != null) this.naturalnessScore = naturalnessScore;
		if (safetyScore != null) this.safetyScore = safetyScore;
		if (overallScore != null) this.overallScore = overallScore;
		if (note != null) this.note = note;
	}

	public Long getId() {
		return id;
	}

	public Long getEvalResultId() {
		return evalResultId;
	}

	public Integer getCoverageScore() {
		return coverageScore;
	}

	public Integer getSeparationScore() {
		return separationScore;
	}

	public Integer getGranularityScore() {
		return granularityScore;
	}

	public Integer getPredecidabilityScore() {
		return predecidabilityScore;
	}

	public Integer getNaturalnessScore() {
		return naturalnessScore;
	}

	public Integer getSafetyScore() {
		return safetyScore;
	}

	public Integer getOverallScore() {
		return overallScore;
	}

	public String getNote() {
		return note;
	}

	public Long getReviewerId() {
		return reviewerId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
