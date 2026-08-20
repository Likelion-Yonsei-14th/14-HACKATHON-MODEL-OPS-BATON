package com.likelion.yonsei.baton.domain.modellab.entity;

/**
 * LLM provider for a ModelConfig / FineTuningJob. The spec (section 2/17) scoped Model Lab to
 * OpenAI only, but production BATON's default provider for a fresh account is the local Qwen3
 * (Ollama) model (see {@code LlmRouter}), so evaluating and tuning against that same model is
 * exactly what Model Lab needs to be useful for the common case. Fine-tuning stays OpenAI-only —
 * there is no local fine-tuning path.
 */
public enum ModelLabProvider {
	OPENAI,
	OLLAMA
}
