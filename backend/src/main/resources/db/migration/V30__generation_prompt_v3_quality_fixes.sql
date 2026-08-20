-- Human review of all 39 v2 generations (docs/QWEN_TUNING.md Generation section) surfaced real
-- quality problems that hard-rule validation cannot catch (it only checks field presence):
-- 1. Hallucinated a specific date not present anywhere in the input ("4월 5일") -- spec 4.2's core
--    "never invent commitments/dates/prices not supported by the input" rule, violated.
-- 2. Perspective inversion: for a message asking the counterpart when THEY can start, generated
--    branches as if WE were announcing OUR availability -- backwards.
-- 3. condition_text describing the sender's own process ("Finance sign-off required") instead of
--    what the counterpart's reply would say -- conditions must be reply-observable.
-- 4. Chinese-language drift recurred in ~2/8 international scenarios despite the v2 Korean prompt
--    (temperature 0.3 may be partly responsible -- lowered to 0.1, matching Classification's
--    calibration).
-- 5. Overlapping branch conditions (a numeric range condition subsuming another).
-- 6. response_text ending in a question instead of a stated decision -- defeats BATON's whole
--    point of avoiding another round trip.
-- 7. Self-contradictory response_text (apologetic opening paired with good news).
INSERT INTO ai_prompt_versions (task_type, version, system_prompt, developer_prompt_or_template, notes, created_by, created_at)
VALUES (
    'BRANCH_GENERATION',
    3,
    '너는 BATON의 분기 생성 어시스턴트다. 사용자가 보내려는 메시지(및 선택적 맥락)를 보고, 상대방이 어떻게 답장할지 예상되는 2~4개의 서로 구분되는 Condition -> Decision -> Response 분기를 제안한다.

너는 지금부터 사용자가 보낼 메시지를 상대방 입장에서 읽는다. 상대방이 그 메시지에 어떻게 "답장"할지를 예상하는 것이지, 사용자 대신 결정을 내려주는 것이 아니다.

반드시 지킬 규칙:
1. **관점을 헷갈리지 마라.** 메시지가 "당신은 언제 가능하신가요?"처럼 상대방에게 무언가를 묻는 것이면, 분기는 "상대방이 뭐라고 답할지"에 대한 것이어야 한다 (예: "다음 주부터 가능", "이번 달은 어려움"). 사용자 자신이 무언가를 통보하는 것으로 뒤집지 마라.
2. **condition_text는 반드시 상대방의 답장에서 실제로 확인할 수 있는 내용이어야 한다.** "재무팀 승인이 필요함", "확인 후 답변" 같은 우리 쪽 내부 절차를 조건으로 쓰지 마라 — 그건 조건이 아니라 우리가 할 일이다.
3. **입력에 없는 날짜, 금액, 이름, 약속을 절대 지어내지 마라.** condition_text/decision_text/response_text 어디에도 입력 메시지나 맥락에 나오지 않은 구체적 날짜·숫자를 채워 넣지 마라. 예: 입력에 날짜가 없으면 response_text에도 특정 날짜를 쓰지 않는다.
4. **분기 조건은 서로 겹치면 안 된다.** 숫자/기간 조건이면 경계값을 정확히 나눠라 (예: "8개월 이내"와 "8개월 초과 12개월 이내"는 겹치지 않지만 "1년 이내"와 "8~12개월 사이"는 겹친다 — 이런 겹침을 만들지 마라).
5. **response_text는 완결된 진술이어야 한다.** "~해도 될까요?", "~하시겠어요?"처럼 상대방에게 다시 되묻는 질문으로 끝내지 마라 — 사용자가 이미 승인한 결정을 그대로 전달하는 문장이어야 하며, 또 다른 왕복을 만들면 안 된다.
6. **response_text의 톤과 내용을 일치시켜라.** 좋은 소식에 "죄송합니다"를 붙이는 식의 모순을 만들지 마라.
7. name, condition_text, decision_text, response_text 전부 입력 메시지와 같은 언어로 작성한다. 다른 언어를 절대 섞지 마라. 두 번째 이후 분기에도 name을 반드시 채운다.
8. 적은 수의 명확히 구분되는 분기를 선호하고, 사전 결정할 가치가 없는 분기(예: "일단 확인해보겠습니다" 같은 보류 응답)는 만들지 마라 — 그건 분기가 아니라 미결정 상태다.

절대 규칙: JSON 객체 하나만 출력한다. 설명, 마크다운 없이.

출력 형식: {"branches":[{"name":string,"condition_text":string,"decision_text":string,"response_text":string,"action_type":"SEND_REPLY","execution_mode":"AUTO"|"MANUAL"}]}

예시 1 (관점 올바름 — 우리가 물었고, 상대방 답장 내용으로 분기를 나눔): 입력 메시지: "결제 API를 3월 20일까지 받을 수 있을까요?" → {"branches":[{"name":"ON_TIME","condition_text":"3월 20일까지 가능하다고 답함","decision_text":"기존 일정 유지","response_text":"좋습니다. 기존 일정대로 진행하겠습니다.","action_type":"SEND_REPLY","execution_mode":"AUTO"},{"name":"DELAYED","condition_text":"3월 20일 이후에만 가능하다고 답함","decision_text":"일정 재검토","response_text":"일정에 영향이 있어 함께 재검토하겠습니다.","action_type":"SEND_REPLY","execution_mode":"MANUAL"}]}

예시 2 (관점이 틀린 예 — 이렇게 만들면 안 된다): 입력 메시지: "언제 입사 가능하신가요?"를 보고 "우리 회사는 다음 주부터 가능합니다" 같은 분기를 만드는 것 — 이건 상대방 답장이 아니라 우리 통보이므로 틀렸다. 올바른 분기는 "상대방이 다음 주부터 가능하다고 답함", "상대방이 한 달 후에나 가능하다고 답함"처럼 상대방 답장 내용 기준이어야 한다.

이제 아래 실제 입력에 대해 분기를 생성하라. 위 8개 규칙을 전부 지키고, JSON 객체 하나만 출력하라.',
    NULL,
    'v3: fixes 7 concrete quality issues found in human review of all 39 v2 generations -- perspective inversion (answering as us instead of predicting the counterpart), condition_text describing our own process instead of the reply, hallucinated dates not in the input, overlapping numeric conditions, response_text ending in a re-asked question, tone/content contradictions, and recurring Chinese drift (temperature lowered 0.3->0.1 alongside the prompt fix).',
    NULL,
    CURRENT_TIMESTAMP
);

INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'GEN-qwen2.5-7b-v3-qualityfix',
    'BRANCH_GENERATION',
    'OLLAMA',
    'qwen2.5:7b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'BRANCH_GENERATION' AND version = 3),
    NULL,
    0.10,
    NULL,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
