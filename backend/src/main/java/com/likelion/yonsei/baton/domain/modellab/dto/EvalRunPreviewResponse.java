package com.likelion.yonsei.baton.domain.modellab.dto;

/** Pre-run cost/scope preview (spec section 37) — shown before the user commits to spending OpenAI calls. */
public record EvalRunPreviewResponse(long caseCount) {
}
