-- CLS-qwen2.5-1.5b-v1 (V13) used the original English v1 seed prompt against a Korean/Japanese
-- dataset and the model's reasoning_summary came back in Chinese (Qwen's dominant pretraining
-- language) -- a confound, not a fair capability read. Re-run with the same Korean decision-state
-- prompt (v5) already used for the best qwen3:0.6b attempt, for an apples-to-apples comparison.
INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen2.5-1.5b-v2-decisionstate',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen2.5:1.5b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 5),
    (SELECT id FROM ai_schema_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 2),
    0.10,
    0.55,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
