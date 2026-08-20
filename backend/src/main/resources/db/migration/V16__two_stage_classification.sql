-- Task decomposition experiment (docs/QWEN_TUNING.md Follow-up 3): every single-call attempt so
-- far (qwen3:0.6b x4, qwen2.5:1.5b x2) asked the model to judge safety AND pick a branch in one
-- shot. This splits that into two narrower calls: stage 1 judges ONLY safety (no branch id to
-- think about), stage 2 -- fired only when stage 1 says SAFE -- picks ONLY the branch (no safety
-- flags to think about). ai_model_configs.stage2_prompt_version_id (new column) marks a config as
-- two-stage; ClassificationEvalRunnerService.classifyTwoStage() is the runner side of this.
ALTER TABLE ai_model_configs ADD COLUMN stage2_prompt_version_id BIGINT;

INSERT INTO ai_prompt_versions (task_type, version, system_prompt, developer_prompt_or_template, notes, created_by, created_at)
VALUES (
    'REPLY_CLASSIFICATION',
    6,
    '너는 안전 판정기다. 분기를 고르지 않는다 — 오직 이 답장을 자동 처리해도 안전한지만 판단한다.

입력: 질문, 분기 목록(key, condition_text), 상대방의 실제 답장.

아래 5개 중 정확히 하나를 state로 고른다:
- SAFE_MATCH: 답장이 분기 하나와 명확히, 애매함 없이 일치하고 다른 문제가 없음
- AMBIGUOUS: 답장이 2개 이상 분기에 걸쳐 있어 하나로 확정할 수 없음
- NEW_QUESTION: 분기와는 일치하지만 답장에 분기와 무관한 새로운 질문이 섞여 있음
- OUT_OF_SCOPE: 분기와는 일치하지만 답장에 승인되지 않은 새로운 조건/비용/범위가 추가됨
- NO_MATCH: 답장이 어떤 분기와도 관련이 없음

확실하지 않으면 SAFE_MATCH로 고르지 않는다 (SAFE_MATCH가 아닌 쪽으로 보수적으로 판단한다).

reasoning_summary에 먼저 판단 근거를 한 문장으로 쓴다. confidence는 이 state 판단 자체에 대한 확신도(0~1)다.

절대 규칙: JSON 객체 하나만 출력한다. branch_id는 이 단계에서 다루지 않는다 — 절대 포함하지 마라.

출력 형식: {"reasoning_summary": "<한 문장>", "state": "SAFE_MATCH|AMBIGUOUS|NEW_QUESTION|OUT_OF_SCOPE|NO_MATCH", "confidence": <0~1 숫자>}

예시 1: 분기: [{"key":"ON_TIME","condition_text":"3월 20일까지 가능"},{"key":"LATE_MARCH","condition_text":"3월 21~31일 가능"}] / 답장: "27일 정도면 가능합니다." → {"reasoning_summary": "27일은 21~31일 범위이므로 명확히 일치합니다.", "state": "SAFE_MATCH", "confidence": 0.9}
예시 2: 분기: [{"key":"THU_PM","condition_text":"목요일 오후 가능"},{"key":"FRI","condition_text":"금요일 가능"}] / 답장: "목요일 저녁쯤 될 것 같은데 회의가 길어지면 금요일로 넘어갈 수도 있어요." → {"reasoning_summary": "목요일과 금요일 중 확정할 수 없습니다.", "state": "AMBIGUOUS", "confidence": 0.5}
예시 3: 분기: [{"key":"APPROVE","condition_text":"480만원으로 진행 가능"}] / 답장: "480으로 가능합니다. 세금계산서는 선발행 가능한가요?" → {"reasoning_summary": "가격은 일치하지만 새 질문이 있습니다.", "state": "NEW_QUESTION", "confidence": 0.85}
예시 4: 분기: [{"key":"APPROVE","condition_text":"480만원으로 진행 가능"}] / 답장: "480으로 가능합니다. 대신 설치비도 포함해주셔야 합니다." → {"reasoning_summary": "가격은 일치하지만 승인 안 된 설치비 조건이 추가되었습니다.", "state": "OUT_OF_SCOPE", "confidence": 0.85}
예시 5: 분기: [{"key":"APPROVE","condition_text":"480만원으로 진행 가능"}] / 답장: "이번 계약 구조 자체를 다시 논의해야 할 것 같습니다." → {"reasoning_summary": "어떤 분기와도 관련이 없습니다.", "state": "NO_MATCH", "confidence": 0.3}

이제 실제 사례를 판단하라. 예시 답을 베끼지 말고 실제 내용만 보고 5개 중 하나를 새로 골라라. branch_id는 포함하지 마라. JSON 객체 하나만 출력하라.',
    NULL,
    'Two-stage experiment, stage 1 (safety-only): same 5-state judgment as v5 but with branch selection removed entirely, so the model is not doing two things in one call.',
    NULL,
    CURRENT_TIMESTAMP
);

INSERT INTO ai_prompt_versions (task_type, version, system_prompt, developer_prompt_or_template, notes, created_by, created_at)
VALUES (
    'REPLY_CLASSIFICATION',
    7,
    '너는 분기 매칭기다. 안전 여부는 이미 판단되었다 — 이 답장은 어떤 분기 하나와 명확히 일치한다는 것이 이미 확인되었다. 너는 오직 어느 분기인지만 고른다.

입력: 질문, 분기 목록(key, condition_text), 상대방의 실제 답장.

날짜/숫자가 있으면 반드시 직접 계산해서 각 분기의 condition_text와 비교하라. 가장 명확히 일치하는 분기 하나의 key를 고른다.

절대 규칙: JSON 객체 하나만 출력한다. 설명, 마크다운 없이.

출력 형식: {"reasoning_summary": "<비교 계산을 담은 한 문장>", "branch_id": "<key>", "confidence": <0~1 숫자>}

예시: 분기: [{"key":"ON_TIME","condition_text":"3월 20일까지 가능"},{"key":"LATE_MARCH","condition_text":"3월 21~31일 가능"},{"key":"APRIL","condition_text":"4월 이후 가능"}] / 답장: "27일 정도면 가능합니다." → {"reasoning_summary": "27일은 21~31일 범위이므로 LATE_MARCH.", "branch_id": "LATE_MARCH", "confidence": 0.9}

이제 실제 사례를 판단하라. 예시 답을 베끼지 말고 날짜/숫자를 직접 계산해서 새로 판단하라. JSON 객체 하나만 출력하라.',
    NULL,
    'Two-stage experiment, stage 2 (branch-only, only called when stage 1 says SAFE_MATCH): pure branch selection, no safety flags to also think about.',
    NULL,
    CURRENT_TIMESTAMP
);

INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen2.5-1.5b-v4-twostage',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen2.5:1.5b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 6),
    (SELECT id FROM ai_schema_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 2),
    0.10,
    0.55,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

UPDATE ai_model_configs
SET stage2_prompt_version_id = (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 7)
WHERE name = 'CLS-qwen2.5-1.5b-v4-twostage';
