-- Combine the two independently-positive levers found so far: two-stage decomposition
-- (branch_match_accuracy 0.3125->0.3333, docs/QWEN_TUNING.md Follow-up 3) and a stricter
-- confidence threshold (False Auto-Send 0.3333->0.1667 at threshold 0.90, Follow-up 4). Neither
-- alone got False Auto-Send under the 0.05 production bar; testing both together.
INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen2.5-1.5b-v5-twostage-strict',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen2.5:1.5b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 6),
    (SELECT id FROM ai_schema_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 2),
    0.10,
    0.90,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

UPDATE ai_model_configs
SET stage2_prompt_version_id = (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 7)
WHERE name = 'CLS-qwen2.5-1.5b-v5-twostage-strict';
