-- Try a bigger local model (qwen2.5:1.5b, ~2.5x qwen3:0.6b) with the ORIGINAL v1 seed prompt
-- (multi-boolean schema, no heavy few-shot hand-holding) rather than any of the qwen3:0.6b-tuned
-- prompts -- the hypothesis from docs/QWEN_TUNING.md is that 0.6B failed from context/attention
-- limits, not from the prompt shape itself, so a bigger model may not need the same scaffolding.
INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen2.5-1.5b-v1',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen2.5:1.5b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 1),
    (SELECT id FROM ai_schema_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 1),
    0.10,
    0.60,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
