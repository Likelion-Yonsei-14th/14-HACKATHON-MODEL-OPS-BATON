-- CLS-qwen2.5-1.5b-v2-decisionstate got False Auto-Send down to 0.333 at threshold 0.55, still far
-- from the < 0.05 production bar. Cheapest next lever, before trying task decomposition: raise the
-- confidence threshold itself. AutoSendGuardrail requires confidence >= threshold AND state ==
-- effectively-safe, so a stricter bar filters out more of the model's less-certain (and often
-- wrong) matches without any new prompt or model change -- same eval data, just re-scored.
INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen2.5-1.5b-v3-strictthreshold',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen2.5:1.5b',
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
