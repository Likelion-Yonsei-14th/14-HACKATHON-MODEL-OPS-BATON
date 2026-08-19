package com.likelion.yonsei.baton.domain.modellab.dto;

/**
 * One entry of a scenario's {@code golden_branches_json}. {@code key} is a dataset-local branch
 * identifier (spec section 13 guidance: "Golden Branch의 DB PK에 지나치게 결합하지 말고... 안정적인 local
 * branch key를 사용") — it is whatever string/number the fixture author wrote (e.g. "1", "2", "on_time"),
 * never a production {@code branches.id}.
 */
public record GoldenBranch(
		String key,
		String name,
		String conditionText,
		String decisionText,
		String responseText
) {
}
