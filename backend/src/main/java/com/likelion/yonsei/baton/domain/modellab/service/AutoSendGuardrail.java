package com.likelion.yonsei.baton.domain.modellab.service;

import java.math.BigDecimal;

/**
 * Implements the full auto-send eligibility conjunction from spec section 10. Confidence crossing
 * the threshold is only ever a necessary condition here — never treated as sufficient on its own.
 *
 * <p>Production's full conjunction (spec section 10) is:
 * <pre>
 * confidence >= threshold
 * AND is_ambiguous = false
 * AND contains_new_question = false
 * AND contains_out_of_scope_content = false
 * AND prompt_injection_suspected = false
 * AND selected branch is valid and approved
 * AND execution_mode = AUTO
 * AND no human intervention
 * AND message is not BATON-generated
 * AND execution not already processed
 * </pre>
 *
 * Model Lab's eval harness evaluates a single fixture case in isolation, so three of those
 * conjuncts are not meaningfully "checked" per case — they are structurally guaranteed by how the
 * harness runs rather than simulated:
 * <ul>
 *   <li>{@code no human intervention} — an eval run never has a human in the loop.</li>
 *   <li>{@code message is not BATON-generated} — eval fixtures are hand-authored recipient replies,
 *       never BATON's own output.</li>
 *   <li>{@code execution not already processed} — each reply case is scored exactly once; there is
 *       no persistent execution/dedup state to violate.</li>
 * </ul>
 * {@code execution_mode = AUTO} is likewise not modeled per-branch in the eval dataset shape (spec
 * section 6's golden branch JSON has no execution_mode field), so this method assumes AUTO for every
 * golden branch — i.e. it evaluates the guardrail as if every branch were eligible for auto-send,
 * which is the conservative "worst case for False Auto-Send" assumption: it can only make the
 * measured False Auto-Send Rate higher (more honest) than production, never lower.
 *
 * <p>Everything else — confidence vs. threshold, ambiguity, new question, out-of-scope content,
 * prompt injection suspicion, and branch validity — is exactly what this method checks, using the
 * model's actual structured output for the case.
 */
public final class AutoSendGuardrail {

	private AutoSendGuardrail() {
	}

	public static boolean isAutoSendEligible(
			BigDecimal confidence,
			BigDecimal threshold,
			boolean isAmbiguous,
			boolean containsNewQuestion,
			boolean containsOutOfScopeContent,
			boolean promptInjectionSuspected,
			boolean selectedBranchValid
	) {
		if (confidence == null || threshold == null) {
			return false;
		}
		if (confidence.compareTo(threshold) < 0) {
			return false;
		}
		return !isAmbiguous
				&& !containsNewQuestion
				&& !containsOutOfScopeContent
				&& !promptInjectionSuspected
				&& selectedBranchValid;
	}
}
