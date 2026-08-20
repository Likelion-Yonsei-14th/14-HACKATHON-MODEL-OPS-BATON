-- Branch Generation (Track A) had never been evaluated against a local model -- the runner was
-- hardcoded to OpenAI only (fixed in the same change as this migration, see
-- GenerationEvalRunnerService.callModel). Reuses the trigger questions already authored for the
-- Classification datasets (v1 + v2) as generation inputs -- Track A only needs question/context,
-- never golden_branches (spec section 3: "Generation 결과를 Classification 정답으로 쓰지 않는다"
-- -- this is the same rule in reverse: Classification's golden branches are never fed back into
-- Generation as ground truth, they are simply a convenient source of realistic trigger questions).
INSERT INTO eval_datasets (name, task_type, version, description, created_at)
VALUES (
    'BATON Generation Eval v1',
    'BRANCH_GENERATION',
    1,
    'Generation eval dataset reusing 40 trigger questions from the Classification v1/v2 datasets (question/context only, no golden branches) across a wide domain mix.',
    CURRENT_TIMESTAMP
);

INSERT INTO eval_scenarios (dataset_id, external_key, title, split, question, context_json, tags_json, golden_branches_json, notes, created_at, updated_at)
SELECT
    (SELECT id FROM eval_datasets WHERE name = 'BATON Generation Eval v1'),
    'GEN-' || row_number() OVER (ORDER BY s.dataset_id, s.id),
    s.title,
    CASE WHEN row_number() OVER (ORDER BY s.dataset_id, s.id) <= 8 THEN 'SMOKE' ELSE 'CORE' END,
    s.question,
    s.context_json,
    NULL,
    NULL,
    s.notes,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM eval_scenarios s
WHERE s.dataset_id IN (
    SELECT id FROM eval_datasets WHERE name IN ('BATON Scenario Dataset v1 (1.0.0)', 'BATON Scenario Dataset v2 (generalization) (2.0.0)')
)
AND s.external_key IN (
    'BATON-001','BATON-003','BATON-005','BATON-007','BATON-009','BATON-011','BATON-013','BATON-020',
    'BATON-025','BATON-030','BATON-035','BATON-040','BATON-045','BATON-050',
    'BATON2-001','BATON2-005','BATON2-011','BATON2-015','BATON2-021','BATON2-025',
    'BATON2-031','BATON2-035','BATON2-041','BATON2-045','BATON2-050',
    'BATON2-002','BATON2-012','BATON2-022','BATON2-032','BATON2-042',
    'BATON2-003','BATON2-013','BATON2-023','BATON2-033','BATON2-043',
    'BATON2-004','BATON2-014','BATON2-024','BATON2-034','BATON2-044'
);

INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'GEN-qwen2.5-7b-v1',
    'BRANCH_GENERATION',
    'OLLAMA',
    'qwen2.5:7b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'BRANCH_GENERATION' AND version = 1),
    NULL,
    0.30,
    NULL,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
