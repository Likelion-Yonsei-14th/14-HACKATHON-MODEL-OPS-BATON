package com.likelion.yonsei.baton.domain.modellab.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A single Classification test fixture within a scenario (spec section 6/7): one or more
 * consecutive recipient reply messages (reply_messages_json, always a JSON array — a single-message
 * reply is just an array of length 1, per spec section 7's multi-message requirement) plus the
 * expected classification labels.
 */
@Entity
@Table(name = "eval_reply_cases")
public class EvalReplyCase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "scenario_id", nullable = false)
	private Long scenarioId;

	@Column(name = "reply_messages_json", nullable = false, columnDefinition = "TEXT")
	private String replyMessagesJson;

	/** Local branch key (matches an id/name in the scenario's golden_branches_json), or null for ambiguous/no-match. */
	@Column(name = "expected_branch_key", length = 100)
	private String expectedBranchKey;

	@Column(name = "expected_ambiguous", nullable = false)
	private boolean expectedAmbiguous;

	@Column(name = "expected_new_question", nullable = false)
	private boolean expectedNewQuestion;

	@Column(name = "expected_out_of_scope", nullable = false)
	private boolean expectedOutOfScope;

	@Column(name = "expected_no_match", nullable = false)
	private boolean expectedNoMatch;

	@Column(name = "expected_guardrail_json", columnDefinition = "TEXT")
	private String expectedGuardrailJson;

	@Column(name = "tags_json", columnDefinition = "TEXT")
	private String tagsJson;

	@Column(length = 1000)
	private String notes;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected EvalReplyCase() {
	}

	public EvalReplyCase(
			Long scenarioId,
			String replyMessagesJson,
			String expectedBranchKey,
			boolean expectedAmbiguous,
			boolean expectedNewQuestion,
			boolean expectedOutOfScope,
			boolean expectedNoMatch,
			String expectedGuardrailJson,
			String tagsJson,
			String notes
	) {
		this.scenarioId = scenarioId;
		this.replyMessagesJson = replyMessagesJson;
		this.expectedBranchKey = expectedBranchKey;
		this.expectedAmbiguous = expectedAmbiguous;
		this.expectedNewQuestion = expectedNewQuestion;
		this.expectedOutOfScope = expectedOutOfScope;
		this.expectedNoMatch = expectedNoMatch;
		this.expectedGuardrailJson = expectedGuardrailJson;
		this.tagsJson = tagsJson;
		this.notes = notes;
	}

	public Long getId() {
		return id;
	}

	public Long getScenarioId() {
		return scenarioId;
	}

	public String getReplyMessagesJson() {
		return replyMessagesJson;
	}

	public String getExpectedBranchKey() {
		return expectedBranchKey;
	}

	public boolean isExpectedAmbiguous() {
		return expectedAmbiguous;
	}

	public boolean isExpectedNewQuestion() {
		return expectedNewQuestion;
	}

	public boolean isExpectedOutOfScope() {
		return expectedOutOfScope;
	}

	public boolean isExpectedNoMatch() {
		return expectedNoMatch;
	}

	public String getExpectedGuardrailJson() {
		return expectedGuardrailJson;
	}

	public String getTagsJson() {
		return tagsJson;
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
