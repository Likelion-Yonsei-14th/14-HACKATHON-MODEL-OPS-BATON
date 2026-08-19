package com.likelion.yonsei.baton.domain.modellab.entity;

/**
 * The two independently-managed AI tasks in Model Lab. Their datasets, prompts, schemas, and eval
 * logic must never be mixed (spec section 3) — every Model Lab entity that scopes to a task carries
 * this enum instead of relying on table separation alone.
 */
public enum ModelLabTaskType {
	BRANCH_GENERATION,
	REPLY_CLASSIFICATION
}
