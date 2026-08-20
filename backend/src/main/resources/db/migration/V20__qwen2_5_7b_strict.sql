-- CLS-qwen2.5-7b-v1 got False Auto-Send to 0.0635 on the 294-case CORE split -- close to the 0.05
-- production bar. Unlike the 0.6b/1.5b experiments, this model's confidence values plausibly carry
-- some real signal (branch/flag accuracy is high enough that threshold has something meaningful to
-- filter on), so it's worth actually testing a stricter bar here rather than assuming it won't help.
INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen2.5-7b-v2-strict',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen2.5:7b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 5),
    (SELECT id FROM ai_schema_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 2),
    0.10,
    0.95,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
