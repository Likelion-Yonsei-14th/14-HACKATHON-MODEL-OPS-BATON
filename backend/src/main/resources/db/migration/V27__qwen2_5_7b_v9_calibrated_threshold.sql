-- Run 25 (CLS-qwen2.5-7b-v4-nondate, threshold 0.95) got branch_match_accuracy up to 0.9206 -- the
-- non-date-branch fix (V25) worked -- but auto_send_coverage came out exactly 0.0: every single
-- confidence value in the run topped out at 0.9 (never 0.95+, see distribution query in
-- docs/QWEN_TUNING.md), so nothing ever cleared the 0.95 bar. Not a capability regression -- v9's
-- self-reported confidence calibration simply sits lower than v5's did. Same prompt/schema, lower
-- threshold to match where this prompt version's confidence actually lands.
INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen2.5-7b-v5-nondate-calibrated',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen2.5:7b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 9),
    (SELECT id FROM ai_schema_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 2),
    0.10,
    0.90,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
