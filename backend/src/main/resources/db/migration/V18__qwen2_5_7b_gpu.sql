-- Desktop RTX 2060 over Tailscale unlocked qwen2.5:7b at usable speed (0.2-2s/case vs 5-30s/case
-- CPU-only) -- see docs/QWEN_TUNING.md. Same Korean decision-state prompt (v5) that got the best
-- qwen2.5:1.5b result, now on a ~5x bigger model.
INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen2.5-7b-v1',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen2.5:7b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 5),
    (SELECT id FROM ai_schema_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 2),
    0.10,
    0.90,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
