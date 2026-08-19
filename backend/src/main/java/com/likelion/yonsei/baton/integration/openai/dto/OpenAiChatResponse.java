package com.likelion.yonsei.baton.integration.openai.dto;

import java.util.List;

public record OpenAiChatResponse(
		String id,
		String model,
		List<Choice> choices,
		Usage usage
) {

	public record Choice(
			int index,
			OpenAiChatMessage message,
			String finishReason
	) {
	}

	/** Maps OpenAI's prompt_tokens/completion_tokens (SNAKE_CASE Jackson naming) — used by Model Lab's eval token/cost metrics. */
	public record Usage(
			Integer promptTokens,
			Integer completionTokens,
			Integer totalTokens
	) {
	}
}
