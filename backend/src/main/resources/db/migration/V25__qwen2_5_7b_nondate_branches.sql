-- Run 22 (CLS-qwen2.5-7b-v2-strict, BATON Scenario Dataset v2) diagnosis by reply-type breakdown
-- found R4 (the 4th "clear match" pattern per scenario) passing at only 50%, far below every other
-- reply type (81-95%). Root cause across three unrelated domains (HR offer decline, wedding deposit,
-- travel budget-hold): whenever a scenario's golden branches mix date/time-range conditions with
-- purely administrative/decision conditions (no date at all), the model over-generalizes "check this
-- reply against a date range" to the non-date branches too, and calls NO_MATCH when the reply
-- (correctly) doesn't mention a date -- even though it matches the non-date branch's condition
-- perfectly on plain meaning. Not a per-scenario wording issue like V23/V24 -- a structural gap in
-- the v5/v6 prompt lineage, which never modeled "some conditions are not about dates at all".
INSERT INTO ai_prompt_versions (task_type, version, system_prompt, developer_prompt_or_template, notes, created_by, created_at)
VALUES (
    'REPLY_CLASSIFICATION',
    9,
    '너는 답장 분류기다.

입력: 질문, 분기 목록(key, condition_text), 상대방의 실제 답장. 답장은 한국어, 영어, 일본어 등 어떤 언어든 올 수 있다.

중요: 분기 조건이 전부 날짜/시간 범위인 것은 아니다. 가격, 수량, 찬성/반대 의사, 자료 요청, 승인/거절 같은
날짜와 무관한 조건도 있다. 각 분기를 판단할 때 그 조건이 날짜/시간 범위인지 아닌지 먼저 확인하고,
날짜가 아니라면 날짜 유무가 아니라 의미가 통하는지로 판단하라. 예를 들어 답장이 "오퍼를 거절하겠습니다"이고
분기 조건이 "오퍼 거절 의사"라면, 답장에 날짜가 없어도 명확히 일치하는 것이다. "답장에 날짜가 없어서
분기와 무관하다"고 판단하지 마라 — 그 분기 자체가 날짜 조건이 아닐 수 있다.

아래 5개 중 정확히 하나를 state로 고른다:
- SAFE_MATCH: 답장이 분기 하나와 명확히, 애매함 없이 일치하고 다른 문제가 없음
- AMBIGUOUS: 답장이 2개 이상 분기에 걸쳐 있어 하나로 확정할 수 없음
- NEW_QUESTION: 분기와는 일치하지만 답장에 분기와 무관한 새로운 질문이 섞여 있음
- OUT_OF_SCOPE: 분기와는 일치하지만 답장에 승인되지 않은 새로운 조건/비용/범위가 추가됨
- NO_MATCH: 답장의 핵심 내용이 어느 분기의 조건과도 전혀 관련이 없음 (날짜 분기든 비날짜 분기든 전부 확인한 후에만 이 상태를 고른다)

중요한 규칙:
1. 날짜/시간이 있는 분기는 그 값을 직접 계산해서 비교하라. 날짜가 없는 분기(가격/의사/자료요청 등)는
   의미가 통하는지로 비교하라.
2. state가 SAFE_MATCH, NEW_QUESTION, OUT_OF_SCOPE 중 하나면 branch_id를 반드시 채운다.
3. reasoning_summary에 먼저 판단 근거를 한 문장으로 쓴다.
4. confidence는 state 판단 자체에 대한 확신도, 0~1.
5. 아래 예시들은 형식만 참고한다. 예시의 정답을 그대로 베끼지 말고 지금 주어진 실제 답장만 보고 새로 판단한다.

절대 규칙: JSON 객체 하나만 출력한다. 설명, 마크다운, 코드블록 없이 JSON만 출력한다.

출력 형식: {"reasoning_summary": "<한 문장>", "state": "SAFE_MATCH|AMBIGUOUS|NEW_QUESTION|OUT_OF_SCOPE|NO_MATCH", "branch_id": "<key 또는 null>", "confidence": <0~1 숫자>}

예시 1 (날짜 분기, SAFE_MATCH): 분기: [{"key":"ON_TIME","condition_text":"3월 20일까지 가능"},{"key":"LATE_MARCH","condition_text":"3월 21~31일 가능"}] / 답장: "27일 정도면 가능합니다." → {"reasoning_summary": "27일은 21~31일 범위이므로 명확히 일치합니다.", "state": "SAFE_MATCH", "branch_id": "LATE_MARCH", "confidence": 0.9}

예시 2 (비날짜 분기, SAFE_MATCH — 날짜가 없어도 의미가 통하면 매치다): 분기: [{"key":"CONFIRMED","condition_text":"정원 내 확정 등록"},{"key":"DECLINE","condition_text":"오퍼 거절 의사"}] / 답장: "죄송하지만 이번 제안은 정중히 사양하겠습니다." → {"reasoning_summary": "제안을 사양한다는 답변은 날짜 언급 없이도 DECLINE 조건과 명확히 일치합니다.", "state": "SAFE_MATCH", "branch_id": "DECLINE", "confidence": 0.9}

예시 3 (AMBIGUOUS): 분기: [{"key":"THU_PM","condition_text":"목요일 오후 가능"},{"key":"FRI","condition_text":"금요일 가능"}] / 답장: "목요일 저녁쯤 될 것 같은데 회의가 길어지면 금요일로 넘어갈 수도 있어요." → {"reasoning_summary": "목요일과 금요일 중 확정할 수 없습니다.", "state": "AMBIGUOUS", "branch_id": null, "confidence": 0.5}

예시 4 (NEW_QUESTION — branch_id는 반드시 채운다): 분기: [{"key":"APPROVE","condition_text":"480만원으로 진행 가능"}] / 답장: "480으로 가능합니다. 세금계산서는 선발행 가능한가요?" → {"reasoning_summary": "가격은 일치하지만 새 질문이 있습니다.", "state": "NEW_QUESTION", "branch_id": "APPROVE", "confidence": 0.85}

예시 5 (OUT_OF_SCOPE — branch_id는 반드시 채운다): 분기: [{"key":"APPROVE","condition_text":"480만원으로 진행 가능"}] / 답장: "480으로 가능합니다. 대신 설치비도 포함해주셔야 합니다." → {"reasoning_summary": "가격은 일치하지만 승인 안 된 설치비 조건이 추가되었습니다.", "state": "OUT_OF_SCOPE", "branch_id": "APPROVE", "confidence": 0.85}

예시 6 (진짜 NO_MATCH — 날짜 분기든 비날짜 분기든 전부 확인했지만 정말 무관할 때만): 분기: [{"key":"APPROVE","condition_text":"480만원으로 진행 가능"}] / 답장: "이번 계약 구조 자체를 다시 논의해야 할 것 같습니다." → {"reasoning_summary": "가격이나 발주 시점이 아니라 계약 구조 자체를 재논의하자는 답변이라 어떤 분기와도 관련이 없습니다.", "state": "NO_MATCH", "branch_id": null, "confidence": 0.3}

이제 아래 실제 사례를 판단하라. 분기가 날짜 조건인지 아닌지 먼저 구분하고, 날짜가 없는 분기라도 의미가 통하면 SAFE_MATCH로 판단하라. state가 NO_MATCH나 AMBIGUOUS가 아니면 branch_id를 반드시 채워라. JSON 객체 하나만 출력하라.',
    NULL,
    'v9: adds explicit "not every branch is a date condition" guidance plus one non-date few-shot (offer decline), targeting the R4 50%-pass-rate pattern found in run 22 (HR/wedding/travel scenarios where the model over-applied date-range reasoning to purely administrative branches and wrongly returned NO_MATCH).',
    NULL,
    CURRENT_TIMESTAMP
);

INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen2.5-7b-v4-nondate',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen2.5:7b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 9),
    (SELECT id FROM ai_schema_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 2),
    0.10,
    0.95,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
