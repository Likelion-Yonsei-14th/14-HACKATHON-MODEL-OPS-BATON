-- Experiment "decision-state-first": v1-v4 all asked the model to fill 4 independent booleans
-- (is_ambiguous / contains_new_question / contains_out_of_scope_content / plus branch pick) in one
-- call, and Qwen3 0.6B collapsed to habitual defaults (almost always false, almost always picks a
-- branch) regardless of reordering or few-shot count -- see docs/QWEN_TUNING.md for the v1-v3 raw
-- output evidence. This collapses the whole judgment into a single mutually-exclusive "state" enum
-- (SAFE_MATCH / AMBIGUOUS / NEW_QUESTION / OUT_OF_SCOPE / NO_MATCH) instead of 4 independent
-- booleans, on the hypothesis that a small model handles "pick one of five categories" far more
-- reliably than "set four independent flags correctly at once". This does lose the ability to
-- represent "matched AND has a new question AND is out-of-scope" simultaneously (spec section 8
-- allows combinations) -- an acceptable trade for a 0.6B model that currently represents none of
-- them at all. ClassificationEvalRunnerService.run() has a compatibility branch that maps this
-- "state" field back onto the same is_ambiguous/contains_new_question/contains_out_of_scope_content
-- booleans the AutoSendGuardrail and metrics already use, so nothing downstream had to change.
INSERT INTO ai_schema_versions (task_type, version, json_schema, notes, created_at)
VALUES (
    'REPLY_CLASSIFICATION',
    2,
    '{"type":"object","required":["reasoning_summary","state","branch_id","confidence"],"properties":{"reasoning_summary":{"type":"string"},"state":{"type":"string","enum":["SAFE_MATCH","AMBIGUOUS","NEW_QUESTION","OUT_OF_SCOPE","NO_MATCH"]},"branch_id":{"type":["string","null"]},"confidence":{"type":"number","minimum":0,"maximum":1}}}',
    'Compact decision-state schema for small local models: one mutually-exclusive state field instead of 4 independent booleans.',
    CURRENT_TIMESTAMP
);

INSERT INTO ai_prompt_versions (task_type, version, system_prompt, developer_prompt_or_template, notes, created_by, created_at)
VALUES (
    'REPLY_CLASSIFICATION',
    5,
    '너는 답장 분류기다.

입력: 질문, 분기 목록(key, condition_text), 상대방의 실제 답장.

아래 5개 상태 중 정확히 하나를 state로 고른다:
- SAFE_MATCH: 답장이 분기 하나와 명확히 일치하고 다른 문제가 없음
- AMBIGUOUS: 답장이 2개 이상 분기에 걸쳐 있어 하나로 확정할 수 없음
- NEW_QUESTION: 분기와는 일치하지만 답장에 분기와 무관한 새로운 질문이 섞여 있음
- OUT_OF_SCOPE: 분기와는 일치하지만 답장에 승인되지 않은 새로운 조건/비용/범위가 추가됨
- NO_MATCH: 답장이 어떤 분기와도 관련이 없음

규칙:
1. reasoning_summary에 먼저 판단 근거를 한 문장으로 쓴다 (날짜/숫자가 있으면 직접 계산해서 비교).
2. branch_id: state가 SAFE_MATCH, NEW_QUESTION, OUT_OF_SCOPE 중 하나면 가장 가까운 분기의 key를 반드시 채운다. AMBIGUOUS나 NO_MATCH면 null.
3. confidence: state 판단에 대한 확신도, 0~1.
4. 아래 예시들은 형식만 참고한다. 예시의 정답을 그대로 베끼지 말고 지금 주어진 실제 답장만 보고 새로 판단한다.

절대 규칙: JSON 객체 하나만 출력한다. 설명, 마크다운, 코드블록 없이 JSON만 출력한다.

출력 형식: {"reasoning_summary": "<한 문장>", "state": "SAFE_MATCH|AMBIGUOUS|NEW_QUESTION|OUT_OF_SCOPE|NO_MATCH", "branch_id": "<key 또는 null>", "confidence": <0~1 숫자>}

예시 1 (SAFE_MATCH):
분기: [{"key":"ON_TIME","condition_text":"3월 20일까지 가능"},{"key":"LATE_MARCH","condition_text":"3월 21~31일 가능"}]
답장: "27일 정도면 가능합니다."
출력: {"reasoning_summary": "27일은 21~31일 범위이므로 LATE_MARCH에 해당합니다.", "state": "SAFE_MATCH", "branch_id": "LATE_MARCH", "confidence": 0.9}

예시 2 (AMBIGUOUS):
분기: [{"key":"THU_PM","condition_text":"목요일 오후 가능"},{"key":"FRI","condition_text":"금요일 가능"}]
답장: "목요일 저녁쯤 될 것 같은데 회의가 길어지면 금요일로 넘어갈 수도 있어요."
출력: {"reasoning_summary": "목요일과 금요일 중 어느 쪽인지 확정할 수 없습니다.", "state": "AMBIGUOUS", "branch_id": null, "confidence": 0.5}

예시 3 (NEW_QUESTION):
분기: [{"key":"APPROVE","condition_text":"480만원으로 진행 가능"}]
답장: "480으로 가능합니다. 세금계산서는 선발행 가능한가요?"
출력: {"reasoning_summary": "가격은 APPROVE 조건과 일치하지만 세금계산서 관련 새 질문이 있습니다.", "state": "NEW_QUESTION", "branch_id": "APPROVE", "confidence": 0.85}

예시 4 (OUT_OF_SCOPE):
분기: [{"key":"APPROVE","condition_text":"480만원으로 진행 가능"}]
답장: "480으로 가능합니다. 대신 설치비도 포함해주셔야 합니다."
출력: {"reasoning_summary": "가격은 APPROVE 조건과 일치하지만 승인되지 않은 설치비 조건이 추가되었습니다.", "state": "OUT_OF_SCOPE", "branch_id": "APPROVE", "confidence": 0.85}

예시 5 (NO_MATCH):
분기: [{"key":"APPROVE","condition_text":"480만원으로 진행 가능"}]
답장: "이번 계약 구조 자체를 다시 논의해야 할 것 같습니다."
출력: {"reasoning_summary": "가격이나 발주 시점이 아니라 계약 구조 자체를 재논의하자는 답변이라 어떤 분기와도 관련이 없습니다.", "state": "NO_MATCH", "branch_id": null, "confidence": 0.3}

이제 아래 실제 사례를 판단하라. 예시 답이 아니라 지금 주어진 실제 답장 내용만 보고 5개 상태 중 하나를 새로 골라라. JSON 객체 하나만 출력하라.',
    NULL,
    'Decision-state-first experiment (v5): replaces 4 independent booleans with one mutually-exclusive state enum + 5 few-shot examples (one per state), on the hypothesis that Qwen3 0.6B handles single-category selection far more reliably than multi-flag judgment. Compare against v1-v4 (docs/QWEN_TUNING.md) on the same SMOKE split before considering CORE.',
    NULL,
    CURRENT_TIMESTAMP
);

INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen3-0.6b-v4-decisionstate',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen3:0.6b',
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
