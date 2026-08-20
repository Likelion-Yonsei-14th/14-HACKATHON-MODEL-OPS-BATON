-- v9 (prompt 9) confidence values cluster almost entirely at exactly 0.9 regardless of correctness
-- (run 25: 214/294 cases at 0.9, run 26 at threshold 0.90: False Auto-Send jumped to 0.4048 --
-- confidence is not discriminating right from wrong at this band). Testing 0.85 as a last
-- calibration point before concluding v5+threshold-0.95 (run 22/23, docs/QWEN_TUNING.md) remains
-- the better overall config despite v9's higher raw branch accuracy -- False Auto-Send is priority
-- #1 and a threshold that cannot discriminate is not a usable safety gate regardless of where it's set.
INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen2.5-7b-v6-nondate-085',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen2.5:7b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 9),
    (SELECT id FROM ai_schema_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 2),
    0.10,
    0.85,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
