package com.likelion.yonsei.baton.domain.modellab.service;

import com.likelion.yonsei.baton.domain.modellab.dto.GoldenBranch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic correction layer for the recurring failure mode found across every Qwen tuning
 * attempt (docs/QWEN_TUNING.md): the model doesn't get branch matching wrong on semantics, it gets
 * it wrong on basic calendar-date arithmetic ("20일" is not in "21~31일", "4월" is not "3월 21~31일").
 * A huge fraction of BATON Scenario Dataset v1's branches are literal calendar-date ranges in
 * `condition_text` — that is not an LLM reasoning task, it is regex + integer comparison.
 *
 * <p>This never invents an answer the LLM didn't already imply: it only fires when (a) every golden
 * branch's condition_text parses cleanly as a date bound/range and (b) the reply text contains an
 * unambiguous date mention. Anything it can't parse confidently, it leaves alone — the LLM's own
 * branch pick and ambiguity flag pass through untouched. It corrects two things: the selected
 * branch (when the LLM's own date arithmetic was wrong) and the ambiguous flag (when the reply
 * text itself contains two date mentions that resolve to two different branches — a case the LLM
 * consistently missed in every experiment run so far).
 */
final class DateRangeGuardrail {

	private DateRangeGuardrail() {
	}

	record DateBound(int month, int day) implements Comparable<DateBound> {
		@Override
		public int compareTo(DateBound o) {
			if (month != o.month) return Integer.compare(month, o.month);
			return Integer.compare(day, o.day);
		}
	}

	private record DateRange(DateBound lo, DateBound hi) {
		boolean contains(DateBound d) {
			return (lo == null || d.compareTo(lo) >= 0) && (hi == null || d.compareTo(hi) <= 0);
		}
	}

	record BranchOverride(String branchKey, boolean ambiguous) {
	}

	private static final Pattern UNTIL = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일\\s*(까지|이내)");
	private static final Pattern RANGE = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})\\s*[~\\-]\\s*(\\d{1,2})일");
	private static final Pattern AFTER = Pattern.compile("(\\d{1,2})월\\s*이후");
	private static final Pattern EXACT_DAY = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");

	private static final Pattern REPLY_MONTH_DAY = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");
	private static final Pattern REPLY_MONTH_EARLY = Pattern.compile("(\\d{1,2})월\\s*(첫|초)");
	private static final Pattern REPLY_MONTH_LATE = Pattern.compile("(\\d{1,2})월\\s*(말|마지막)");
	private static final Pattern REPLY_BARE_DAY = Pattern.compile("(\\d{1,2})일");

	// English month names (~1/3 of BATON Scenario Dataset v1 is international/English, spec
	// section 31 / docs/QWEN_TUNING.md) — same deterministic-correction approach, different lexicon.
	private static final Map<String, Integer> EN_MONTHS = Map.ofEntries(
			Map.entry("jan", 1), Map.entry("january", 1),
			Map.entry("feb", 2), Map.entry("february", 2),
			Map.entry("mar", 3), Map.entry("march", 3),
			Map.entry("apr", 4), Map.entry("april", 4),
			Map.entry("may", 5),
			Map.entry("jun", 6), Map.entry("june", 6),
			Map.entry("jul", 7), Map.entry("july", 7),
			Map.entry("aug", 8), Map.entry("august", 8),
			Map.entry("sep", 9), Map.entry("sept", 9), Map.entry("september", 9),
			Map.entry("oct", 10), Map.entry("october", 10),
			Map.entry("nov", 11), Map.entry("november", 11),
			Map.entry("dec", 12), Map.entry("december", 12)
	);
	private static final String EN_MONTH_ALT = String.join("|", EN_MONTHS.keySet());
	private static final Pattern EN_RANGE = Pattern.compile(
			"(" + EN_MONTH_ALT + ")\\.?\\s+(\\d{1,2})\\s*[~\\-]\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE);
	private static final Pattern EN_BEFORE = Pattern.compile(
			"before\\s+(" + EN_MONTH_ALT + ")\\.?\\s+(\\d{1,2})", Pattern.CASE_INSENSITIVE);
	private static final Pattern EN_AFTER_DAY = Pattern.compile(
			"after\\s+(" + EN_MONTH_ALT + ")\\.?\\s+(\\d{1,2})", Pattern.CASE_INSENSITIVE);
	private static final Pattern EN_MONTH_ONWARD = Pattern.compile(
			"(" + EN_MONTH_ALT + ")\\s+(이후|onward|or later)", Pattern.CASE_INSENSITIVE);
	private static final Pattern EN_MONTH_DAY = Pattern.compile(
			"(" + EN_MONTH_ALT + ")\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?", Pattern.CASE_INSENSITIVE);
	private static final Pattern EN_MID_MONTH = Pattern.compile("mid[\\s-](" + EN_MONTH_ALT + ")", Pattern.CASE_INSENSITIVE);
	private static final Pattern EN_EARLY_MONTH = Pattern.compile("early\\s+(" + EN_MONTH_ALT + ")", Pattern.CASE_INSENSITIVE);
	private static final Pattern EN_LATE_MONTH = Pattern.compile("late\\s+(" + EN_MONTH_ALT + ")", Pattern.CASE_INSENSITIVE);

	/** Returns empty when the branches or the reply aren't cleanly date-parseable — the caller
	 * should keep whatever the model said. Only returns a value when this can resolve the case with
	 * plain arithmetic, more reliably than the model has managed in any prompt version tried. */
	static Optional<BranchOverride> resolve(List<GoldenBranch> branches, List<String> replyMessages) {
		// Catch-all branches ("일정 확정 불가" etc.) legitimately have no date in them — skip them
		// rather than bailing on the whole scenario. They just never become a deterministic-match
		// target, same as before this guardrail existed.
		Map<String, DateRange> branchRanges = new HashMap<>();
		for (GoldenBranch branch : branches) {
			DateRange range = parseBranchRange(branch.conditionText());
			if (range != null) {
				branchRanges.put(branch.key(), range);
			}
		}
		if (branchRanges.size() < 2) {
			return Optional.empty();
		}

		int baseMonth = mostCommonMonth(branchRanges.values());
		String combinedReply = String.join(" ", replyMessages);
		List<DateBound> mentions = extractReplyDates(combinedReply, baseMonth);
		if (mentions.isEmpty()) {
			return Optional.empty();
		}

		Set<String> matchedBranches = new LinkedHashSet<>();
		for (DateBound mention : mentions) {
			for (Map.Entry<String, DateRange> entry : branchRanges.entrySet()) {
				if (entry.getValue().contains(mention)) {
					matchedBranches.add(entry.getKey());
				}
			}
		}

		if (matchedBranches.isEmpty()) {
			return Optional.empty();
		}
		if (matchedBranches.size() == 1) {
			return Optional.of(new BranchOverride(matchedBranches.iterator().next(), false));
		}
		return Optional.of(new BranchOverride(null, true));
	}

	private static int monthNum(String name) {
		return EN_MONTHS.get(name.toLowerCase(java.util.Locale.ROOT));
	}

	private static DateRange parseBranchRange(String conditionText) {
		if (conditionText == null) return null;

		Matcher m = RANGE.matcher(conditionText);
		if (m.find()) {
			int month = Integer.parseInt(m.group(1));
			int lo = Integer.parseInt(m.group(2));
			int hi = Integer.parseInt(m.group(3));
			return new DateRange(new DateBound(month, lo), new DateBound(month, hi));
		}
		m = UNTIL.matcher(conditionText);
		if (m.find()) {
			int month = Integer.parseInt(m.group(1));
			int day = Integer.parseInt(m.group(2));
			return new DateRange(null, new DateBound(month, day));
		}
		m = AFTER.matcher(conditionText);
		if (m.find()) {
			int month = Integer.parseInt(m.group(1));
			return new DateRange(new DateBound(month, 1), null);
		}
		m = EXACT_DAY.matcher(conditionText);
		if (m.find()) {
			int month = Integer.parseInt(m.group(1));
			int day = Integer.parseInt(m.group(2));
			return new DateRange(new DateBound(month, day), new DateBound(month, day));
		}

		// English: "Sep 16-30", "Before Oct 1", "After Oct 7", "October 이후", "Sep 15" (exact).
		m = EN_RANGE.matcher(conditionText);
		if (m.find()) {
			int month = monthNum(m.group(1));
			int lo = Integer.parseInt(m.group(2));
			int hi = Integer.parseInt(m.group(3));
			return new DateRange(new DateBound(month, lo), new DateBound(month, hi));
		}
		m = EN_BEFORE.matcher(conditionText);
		if (m.find()) {
			int month = monthNum(m.group(1));
			int day = Integer.parseInt(m.group(2));
			int hiMonth = month;
			int hiDay = day - 1;
			if (hiDay < 1) {
				hiMonth = month - 1;
				hiDay = 31;
			}
			return new DateRange(null, new DateBound(hiMonth, hiDay));
		}
		m = EN_AFTER_DAY.matcher(conditionText);
		if (m.find()) {
			int month = monthNum(m.group(1));
			int day = Integer.parseInt(m.group(2));
			return new DateRange(new DateBound(month, day + 1), null);
		}
		m = EN_MONTH_ONWARD.matcher(conditionText);
		if (m.find()) {
			int month = monthNum(m.group(1));
			return new DateRange(new DateBound(month, 1), null);
		}
		m = EN_MONTH_DAY.matcher(conditionText);
		if (m.find()) {
			int month = monthNum(m.group(1));
			int day = Integer.parseInt(m.group(2));
			return new DateRange(new DateBound(month, day), new DateBound(month, day));
		}
		return null;
	}

	private static int mostCommonMonth(Iterable<DateRange> ranges) {
		Map<Integer, Integer> counts = new HashMap<>();
		for (DateRange r : ranges) {
			if (r.lo() != null) counts.merge(r.lo().month(), 1, Integer::sum);
			if (r.hi() != null) counts.merge(r.hi().month(), 1, Integer::sum);
		}
		return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(1);
	}

	private static List<DateBound> extractReplyDates(String reply, int baseMonth) {
		List<DateBound> found = new ArrayList<>();
		Set<String> consumed = new LinkedHashSet<>();

		Matcher m = REPLY_MONTH_DAY.matcher(reply);
		while (m.find()) {
			found.add(new DateBound(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))));
			consumed.add(m.group());
		}
		m = REPLY_MONTH_EARLY.matcher(reply);
		while (m.find()) {
			found.add(new DateBound(Integer.parseInt(m.group(1)), 3));
			consumed.add(m.group());
		}
		m = REPLY_MONTH_LATE.matcher(reply);
		while (m.find()) {
			found.add(new DateBound(Integer.parseInt(m.group(1)), 28));
			consumed.add(m.group());
		}
		// Bare "27일" mentions, in the scenario's dominant month — only counted if not already part
		// of a "N월 D일" match above (avoid double-counting "3월 27일" as both a full match and a
		// bare-day match on "27일").
		m = REPLY_BARE_DAY.matcher(reply);
		while (m.find()) {
			String matched = m.group();
			boolean alreadyCounted = consumed.stream().anyMatch(s -> s.contains(matched));
			if (!alreadyCounted) {
				found.add(new DateBound(baseMonth, Integer.parseInt(m.group(1))));
			}
		}

		// English mentions: "Sep 28", "mid-October", "early Sep", "late Oct". No bare-day-only English
		// extraction (a lone "3" in English prose is far more likely to be unrelated than a Korean
		// "27일" is, since the 일 suffix disambiguates in Korean but nothing does in English).
		m = EN_MONTH_DAY.matcher(reply);
		while (m.find()) {
			found.add(new DateBound(monthNum(m.group(1)), Integer.parseInt(m.group(2))));
			consumed.add(m.group());
		}
		m = EN_MID_MONTH.matcher(reply);
		while (m.find()) {
			found.add(new DateBound(monthNum(m.group(1)), 15));
		}
		m = EN_EARLY_MONTH.matcher(reply);
		while (m.find()) {
			found.add(new DateBound(monthNum(m.group(1)), 3));
		}
		m = EN_LATE_MONTH.matcher(reply);
		while (m.find()) {
			found.add(new DateBound(monthNum(m.group(1)), 28));
		}
		return found;
	}
}
