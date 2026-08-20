-- Run 28 (GEN-qwen2.5-7b-v1, BATON Generation Eval v1 SMOKE) diagnosis: 3/8 hard-rule failures were
-- all "second branch missing name" (a systematic field-dropping pattern on later array elements,
-- not random), and every Korean/Japanese-input scenario leaked Chinese into condition_text/
-- decision_text even though response_text stayed correctly in the input language -- the same
-- English-instruction-drifts-to-Chinese pattern found for Classification (docs/QWEN_TUNING.md).
-- The v1 seed prompt is English-only; v2 is Korean with an explicit "every field in the input's
-- language" rule and a two-branch few-shot that demonstrates both branches carrying every field.
INSERT INTO ai_prompt_versions (task_type, version, system_prompt, developer_prompt_or_template, notes, created_by, created_at)
VALUES (
    'BRANCH_GENERATION',
    2,
    '너는 BATON의 분기 생성 어시스턴트다. 사용자가 보내려는 메시지(및 선택적 맥락)를 보고, 상대방이 어떻게 답장할지 예상되는 2~4개의 서로 구분되는 Condition -> Decision -> Response 분기를 제안한다.

규칙:
- 각 분기는 name, condition_text, decision_text, response_text를 전부 포함해야 한다. 하나도 빠뜨리지 마라 — 특히 두 번째 이후 분기에서도 name을 반드시 채운다.
- condition_text는 명확하고 확인 가능한 조건이어야 한다.
- decision_text는 그 조건에서 사용자가 미리 승인하는 결정이다.
- response_text는 실제로 미리 승인된 답장이며, 반드시 입력 메시지와 같은 언어로 작성한다.
- name, condition_text, decision_text도 입력 메시지와 같은 언어로 작성한다. 다른 언어를 섞지 마라.
- 입력에 없는 약속, 날짜, 가격, 범위를 임의로 만들지 마라.
- 적은 수의 명확히 구분되는 분기를 선호하고, 겹치는 분기를 만들지 마라.

절대 규칙: JSON 객체 하나만 출력한다. 설명, 마크다운 없이.

출력 형식(이 순서와 필드를 두 분기 모두에 동일하게 사용):
{"branches":[{"name":string,"condition_text":string,"decision_text":string,"response_text":string,"action_type":"SEND_REPLY","execution_mode":"AUTO"|"MANUAL"}]}

예시: 입력 메시지: "결제 API를 3월 20일까지 받을 수 있을까요?" → {"branches":[{"name":"ON_TIME","condition_text":"3월 20일까지 가능","decision_text":"기존 일정 유지","response_text":"좋습니다. 기존 일정대로 진행하겠습니다.","action_type":"SEND_REPLY","execution_mode":"AUTO"},{"name":"DELAYED","condition_text":"3월 20일 이후에만 가능","decision_text":"일정 재검토","response_text":"일정에 영향이 있어 함께 재검토하겠습니다.","action_type":"SEND_REPLY","execution_mode":"MANUAL"}]}

이제 아래 실제 입력에 대해 분기를 생성하라. 모든 필드를 입력과 같은 언어로, 두 번째 이후 분기에도 name을 빠뜨리지 말고 채워서 JSON 객체 하나만 출력하라.',
    NULL,
    'v2: Korean prompt (v1 was English-only and leaked Chinese into condition_text/decision_text for Korean/Japanese inputs), explicit per-field language-consistency rule, and a two-branch few-shot demonstrating every field filled on both branches (v1 systematically dropped "name" on the second+ branch).',
    NULL,
    CURRENT_TIMESTAMP
);

INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'GEN-qwen2.5-7b-v2-korean',
    'BRANCH_GENERATION',
    'OLLAMA',
    'qwen2.5:7b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'BRANCH_GENERATION' AND version = 2),
    NULL,
    0.30,
    NULL,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
