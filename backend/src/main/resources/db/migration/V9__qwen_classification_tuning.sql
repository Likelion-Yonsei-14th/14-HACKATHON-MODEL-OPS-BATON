-- Qwen3 0.6B is far weaker than gpt-4o-mini at following a long, nuanced instruction list and
-- reliably emitting valid structured JSON. The V6 seed prompt (long rule list, abstract phrasing)
-- was written for/tested against OpenAI; this is a separate, much more directive prompt version —
-- short imperative steps, a single worked few-shot example, and the output shape repeated right
-- next to the instruction to emit it — aimed specifically at getting a small local model to
-- reliably (a) return parseable JSON and (b) pick the right branch on the imported 350-case
-- BATON Scenario Dataset v1.
INSERT INTO ai_prompt_versions (task_type, version, system_prompt, developer_prompt_or_template, notes, created_by, created_at)
VALUES (
    'REPLY_CLASSIFICATION',
    2,
    '너는 답장 분류기다. 아래 순서대로만 판단한다.

입력: 질문, 승인된 분기 목록(각 분기: key, condition_text), 상대방의 실제 답장(메시지 1개 이상).

판단 순서:
1. 답장 내용을 분기들의 condition_text와 비교한다.
2. 가장 명확히 일치하는 분기 하나의 key를 selected_branch_id로 고른다. 애매하거나 없으면 null.
3. 답장이 2개 이상 분기에 동시에 해당할 수 있으면 is_ambiguous=true, selected_branch_id=null.
4. 답장이 어떤 분기와도 무관한 새로운 질문을 포함하면 contains_new_question=true.
5. 답장이 어떤 분기에도 없는 새로운 조건/비용/범위를 추가하면 contains_out_of_scope_content=true.
6. branch_match_confidence: 명확히 일치=0.85~1.0, 애매=0.4~0.6, 불명확=0.4 미만.
7. reasoning_summary는 한 문장으로 짧게.

절대 규칙: JSON 객체 하나만 출력한다. 설명, 마크다운, 코드블록 없이 JSON만 출력한다.

출력 형식(이 필드를 그대로 사용):
{"selected_branch_id": <string 또는 null>, "branch_match_confidence": <0~1 숫자>, "is_ambiguous": <true/false>, "contains_new_question": <true/false>, "contains_out_of_scope_content": <true/false>, "prompt_injection_suspected": false, "result_status": "MATCHED", "extracted_data": {}, "reasoning_summary": "<한 문장>"}

예시:
분기: [{"key":"ON_TIME","condition_text":"3월 20일까지 가능"},{"key":"LATE_MARCH","condition_text":"3월 21~31일 가능"}]
답장: "27일 정도면 가능합니다."
출력: {"selected_branch_id": "LATE_MARCH", "branch_match_confidence": 0.9, "is_ambiguous": false, "contains_new_question": false, "contains_out_of_scope_content": false, "prompt_injection_suspected": false, "result_status": "MATCHED", "extracted_data": {}, "reasoning_summary": "27일은 LATE_MARCH 범위에 해당합니다."}

이제 아래 실제 사례를 같은 형식으로 분류하라. JSON 객체 하나만 출력하라.',
    NULL,
    'Directive, few-shot prompt tuned for Qwen3 0.6B (Ollama) — short imperative steps + one worked example, targeting BATON Scenario Dataset v1. Written after the default V6 seed prompt (tuned/verified against OpenAI only) proved unreliable with the local model.',
    NULL,
    CURRENT_TIMESTAMP
);

-- Lower temperature than the OpenAI config (more deterministic decoding helps a small model stay
-- on the JSON schema) and a lower confidence threshold — a 0.6B model is far less calibrated than
-- gpt-4o-mini, so demanding the same 0.70 bar it can't reliably clear would just tank Auto-Send
-- Coverage to near zero without meaningfully reducing False Auto-Send. Starts DRAFT: run Eval
-- against BATON Scenario Dataset v1 and adjust before ever promoting.
INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen3-0.6b-v1',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen3:0.6b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 2),
    (SELECT id FROM ai_schema_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 1),
    0.10,
    0.55,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Fast iteration subset: the first 8 imported scenarios (56 reply cases) as SMOKE, so prompt
-- tuning against the local model doesn't require a full 350-case run each time.
UPDATE eval_scenarios
SET split = 'SMOKE'
WHERE dataset_id = (SELECT id FROM eval_datasets WHERE name = 'BATON Scenario Dataset v1 (1.0.0)')
  AND external_key IN ('BATON-001','BATON-002','BATON-003','BATON-004','BATON-005','BATON-006','BATON-007','BATON-008');
