package com.likelion.yonsei.baton.domain.modellab.entity;

/**
 * Lifecycle of an {@link AiModelConfig}. Only a DRAFT config may be edited in place; every other
 * status is immutable and edits must create a new config row (spec section 13/15).
 */
public enum ModelConfigStatus {
	DRAFT,
	EVALUATING,
	STAGING,
	PRODUCTION,
	ARCHIVED
}
