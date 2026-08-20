-- BATON Scenario Dataset v1's per-scenario reply pattern (R1-R7, per the source JSON's own "notes"
-- field) never included a genuine NO_MATCH case -- every scenario's replies always relate to at
-- least one branch's subject matter. no_match_detection_recall being 0.0 in every eval run so far
-- (docs/QWEN_TUNING.md) was a 0/0 dataset gap, not a model failure -- there was nothing to recall.
-- Adds one clearly off-topic reply per scenario across 6 scenarios spanning different domains, so
-- the metric has an actual denominator. The v5 prompt already has a NO_MATCH few-shot example
-- (unrelated to these specific scenarios) -- no prompt change needed, only data.
INSERT INTO eval_reply_cases (scenario_id, reply_messages_json, expected_branch_key, expected_ambiguous, expected_new_question, expected_out_of_scope, expected_no_match, tags_json, notes, created_at, updated_at)
SELECT s.id, v.reply_messages_json, NULL, false, false, false, true, '["NO_MATCH"]', 'R8-synthetic', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM eval_scenarios s
JOIN (VALUES
    ('BATON-001', '["이번 결제 게이트웨이 자체를 다른 PG사로 교체하는 걸 검토해야 할 것 같습니다."]'),
    ('BATON-009', '["계약서 준거법을 아예 다른 나라 법으로 바꾸는 논의를 먼저 해야 할 것 같습니다."]'),
    ('BATON-011', '["이 프로젝트 자체를 내년으로 통째로 미루는 걸 논의해야 할 것 같습니다."]'),
    ('BATON-020', '["웨비나 플랫폼을 아예 다른 서비스로 교체하는 걸 검토 중입니다."]'),
    ('BATON-030', '["행사 형식 자체를 오프라인에서 온라인으로 바꾸는 걸 논의해야 할 것 같습니다."]'),
    ('BATON-040', '["이번 공급처와의 계약 자체를 재검토해야 할 것 같습니다."]')
) AS v(external_key, reply_messages_json) ON v.external_key = s.external_key
WHERE s.dataset_id = (SELECT id FROM eval_datasets WHERE name = 'BATON Scenario Dataset v1 (1.0.0)');
