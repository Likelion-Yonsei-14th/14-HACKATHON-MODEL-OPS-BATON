package com.likelion.yonsei.baton.domain.modellab.entity;

/**
 * Model Lab's own copy of the production {@code ClassificationResultStatus} enum (see
 * {@code domain.classification.entity.ClassificationResultStatus}), plus GUARDRAIL_REJECTED which
 * the current production enum defines but the current production service never actually sets.
 * Kept as a separate type per the hard rule that eval code must not depend on production entities.
 */
public enum ModelLabClassificationResultStatus {
	MATCHED,
	LOW_CONFIDENCE,
	NO_MATCH,
	AMBIGUOUS,
	GUARDRAIL_REJECTED
}
