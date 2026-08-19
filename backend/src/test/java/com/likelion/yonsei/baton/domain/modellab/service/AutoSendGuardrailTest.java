package com.likelion.yonsei.baton.domain.modellab.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AutoSendGuardrailTest {

	private static final BigDecimal THRESHOLD = new BigDecimal("0.70");

	@Test
	void confidenceAboveThresholdAloneIsNotSufficient() {
		// The hard rule from spec section 10: confidence >= threshold must never be treated as
		// sufficient on its own. Every other guardrail flag defaults to the "unsafe" value here.
		boolean eligible = AutoSendGuardrail.isAutoSendEligible(
				new BigDecimal("0.99"), THRESHOLD,
				true, false, false, false, true // is_ambiguous = true
		);
		assertThat(eligible).isFalse();
	}

	@Test
	void allGuardrailsClearAndConfidenceAtThresholdIsEligible() {
		boolean eligible = AutoSendGuardrail.isAutoSendEligible(
				new BigDecimal("0.70"), THRESHOLD,
				false, false, false, false, true
		);
		assertThat(eligible).isTrue();
	}

	@Test
	void confidenceBelowThresholdIsNeverEligible() {
		boolean eligible = AutoSendGuardrail.isAutoSendEligible(
				new BigDecimal("0.69"), THRESHOLD,
				false, false, false, false, true
		);
		assertThat(eligible).isFalse();
	}

	@Test
	void newQuestionBlocksAutoSendEvenWithHighConfidence() {
		boolean eligible = AutoSendGuardrail.isAutoSendEligible(
				new BigDecimal("0.95"), THRESHOLD,
				false, true, false, false, true
		);
		assertThat(eligible).isFalse();
	}

	@Test
	void outOfScopeContentBlocksAutoSend() {
		boolean eligible = AutoSendGuardrail.isAutoSendEligible(
				new BigDecimal("0.95"), THRESHOLD,
				false, false, true, false, true
		);
		assertThat(eligible).isFalse();
	}

	@Test
	void promptInjectionSuspicionBlocksAutoSend() {
		boolean eligible = AutoSendGuardrail.isAutoSendEligible(
				new BigDecimal("0.95"), THRESHOLD,
				false, false, false, true, true
		);
		assertThat(eligible).isFalse();
	}

	@Test
	void invalidBranchBlocksAutoSendRegardlessOfConfidence() {
		boolean eligible = AutoSendGuardrail.isAutoSendEligible(
				new BigDecimal("0.99"), THRESHOLD,
				false, false, false, false, false // selectedBranchValid = false
		);
		assertThat(eligible).isFalse();
	}

	@Test
	void nullConfidenceOrThresholdIsNeverEligible() {
		assertThat(AutoSendGuardrail.isAutoSendEligible(null, THRESHOLD, false, false, false, false, true)).isFalse();
		assertThat(AutoSendGuardrail.isAutoSendEligible(new BigDecimal("0.9"), null, false, false, false, false, true)).isFalse();
	}

	@Test
	void raisingThresholdCanFlipAnOtherwiseEligibleCaseToIneligible() {
		BigDecimal confidence = new BigDecimal("0.75");
		boolean eligibleAt70 = AutoSendGuardrail.isAutoSendEligible(confidence, new BigDecimal("0.70"), false, false, false, false, true);
		boolean eligibleAt80 = AutoSendGuardrail.isAutoSendEligible(confidence, new BigDecimal("0.80"), false, false, false, false, true);

		assertThat(eligibleAt70).isTrue();
		assertThat(eligibleAt80).isFalse();
	}
}
