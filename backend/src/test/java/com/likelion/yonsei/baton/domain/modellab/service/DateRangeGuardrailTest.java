package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.domain.modellab.dto.GoldenBranch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DateRangeGuardrailTest {

	private static final List<GoldenBranch> BATON_001_BRANCHES = List.of(
			new GoldenBranch("ON_TIME", "ON_TIME", "3월 20일까지 가능", "d", "r"),
			new GoldenBranch("LATE_MARCH", "LATE_MARCH", "3월 21~31일 가능", "d", "r"),
			new GoldenBranch("APRIL", "APRIL", "4월 이후 가능", "d", "r"),
			new GoldenBranch("UNKNOWN", "UNKNOWN", "일정 확정 불가", "d", "r")
	);

	@Test
	void bareDayInReplyResolvesAgainstTheDominantMonth() {
		// The model repeatedly got this exact case wrong across every prompt version (docs/QWEN_TUNING.md) —
		// picking ON_TIME or APRIL instead of LATE_MARCH for "27일".
		Optional<DateRangeGuardrail.BranchOverride> result = DateRangeGuardrail.resolve(BATON_001_BRANCHES, List.of("27일 정도면 가능합니다."));
		assertThat(result).isPresent();
		assertThat(result.get().branchKey()).isEqualTo("LATE_MARCH");
		assertThat(result.get().ambiguous()).isFalse();
	}

	@Test
	void dayJustBeforeARangeDoesNotFalsePositiveIntoIt() {
		// The model repeatedly claimed "20일" was "within 21~31일" (docs/QWEN_TUNING.md eval_results
		// id 417, 473) — 20 < 21, it isn't.
		Optional<DateRangeGuardrail.BranchOverride> result = DateRangeGuardrail.resolve(BATON_001_BRANCHES, List.of("20일까지 최대한 해보겠습니다."));
		assertThat(result).isPresent();
		assertThat(result.get().branchKey()).isEqualTo("ON_TIME");
	}

	@Test
	void twoDatesInDifferentBranchesIsDeterministicallyAmbiguous() {
		Optional<DateRangeGuardrail.BranchOverride> result = DateRangeGuardrail.resolve(
				BATON_001_BRANCHES, List.of("20일까지 최대한 해보겠습니다.", "안 되면 27일 정도가 될 수도 있습니다."));
		assertThat(result).isPresent();
		assertThat(result.get().ambiguous()).isTrue();
		assertThat(result.get().branchKey()).isNull();
	}

	@Test
	void explicitMonthInReplyOverridesTheDominantMonthGuess() {
		// The model repeatedly claimed "4월 첫째 주" fell inside "3월 21~31일" (eval_results id 415, 471).
		Optional<DateRangeGuardrail.BranchOverride> result = DateRangeGuardrail.resolve(BATON_001_BRANCHES, List.of("4월 첫째 주쯤 가능합니다."));
		assertThat(result).isPresent();
		assertThat(result.get().branchKey()).isEqualTo("APRIL");
	}

	@Test
	void noDateMentionedLeavesTheDecisionToTheModel() {
		Optional<DateRangeGuardrail.BranchOverride> result = DateRangeGuardrail.resolve(
				BATON_001_BRANCHES, List.of("아직 개발팀 확인이 안 돼 날짜를 확답하기 어렵습니다."));
		assertThat(result).isEmpty();
	}

	private static final List<GoldenBranch> SINGAPORE_PAYMENT_BRANCHES = List.of(
			new GoldenBranch("BEFORE", "BEFORE", "Before Oct 1", "d", "r"),
			new GoldenBranch("EARLY_OCT", "EARLY_OCT", "Oct 1~7", "d", "r"),
			new GoldenBranch("LATE", "LATE", "After Oct 7", "d", "r"),
			new GoldenBranch("DEPENDENCY", "DEPENDENCY", "확정 불가", "d", "r")
	);

	@Test
	void englishBeforeDateResolvesCorrectly() {
		// eval_results run 18: model missed this multi-message English ambiguity entirely.
		Optional<DateRangeGuardrail.BranchOverride> result = DateRangeGuardrail.resolve(SINGAPORE_PAYMENT_BRANCHES, List.of("We can have it live by Sep 28."));
		assertThat(result).isPresent();
		assertThat(result.get().branchKey()).isEqualTo("BEFORE");
	}

	@Test
	void englishRangeResolvesCorrectly() {
		Optional<DateRangeGuardrail.BranchOverride> result = DateRangeGuardrail.resolve(SINGAPORE_PAYMENT_BRANCHES, List.of("Oct 3 is the earliest."));
		assertThat(result).isPresent();
		assertThat(result.get().branchKey()).isEqualTo("EARLY_OCT");
	}

	@Test
	void englishAfterDateResolvesCorrectly() {
		Optional<DateRangeGuardrail.BranchOverride> result = DateRangeGuardrail.resolve(SINGAPORE_PAYMENT_BRANCHES, List.of("It will likely be mid-October."));
		assertThat(result).isPresent();
		assertThat(result.get().branchKey()).isEqualTo("LATE");
	}

	@Test
	void englishTwoDatesInDifferentBranchesIsDeterministicallyAmbiguous() {
		// The exact case the model got wrong in run 18 (CLS-qwen2.5-7b-v2-strict).
		Optional<DateRangeGuardrail.BranchOverride> result = DateRangeGuardrail.resolve(
				SINGAPORE_PAYMENT_BRANCHES, List.of("Sep 30 if compliance clears this week; otherwise Oct 4."));
		assertThat(result).isPresent();
		assertThat(result.get().ambiguous()).isTrue();
	}

	@Test
	void nonDateBranchesAreLeftAlone() {
		List<GoldenBranch> weekdayBranches = List.of(
				new GoldenBranch("THU_PM", "THU_PM", "목요일 오후 가능", "d", "r"),
				new GoldenBranch("FRI", "FRI", "금요일 가능", "d", "r")
		);
		Optional<DateRangeGuardrail.BranchOverride> result = DateRangeGuardrail.resolve(weekdayBranches, List.of("목요일 3시 괜찮습니다."));
		assertThat(result).isEmpty();
	}
}
