-- V9's prompt (one few-shot example) caused Qwen3 0.6B to anchor on that example's answer
-- (LATE_MARCH) almost regardless of the actual reply content -- observed on run #1 against the
-- SMOKE split: branch_match_accuracy 0.50, ambiguous/new_question/out_of_scope recall all 0.0,
-- false_auto_send_rate 0.958. A single example gives a 0.6B model a pattern to copy rather than
-- generalize from. v3 adds three few-shot examples spanning the three cases the model was
-- ignoring (clear match, ambiguous, match+new-question) and an explicit anti-copying instruction
-- repeated before and after the examples.
INSERT INTO ai_prompt_versions (task_type, version, system_prompt, developer_prompt_or_template, notes, created_by, created_at)
VALUES (
    'REPLY_CLASSIFICATION',
    3,
    '너는 답장 분류기다. 아래 순서대로만 판단한다.

입력: 질문, 승인된 분기 목록(각 분기: key, condition_text), 상대방의 실제 답장(메시지 1개 이상).

판단 순서:
1. 답장 내용을 분기들의 condition_text와 하나씩 실제로 비교한다. 아래 예시들은 형식을 보여주기 위한 것일 뿐이다. 예시의 정답(selected_branch_id 값)을 그대로 베끼면 안 되고, 지금 주어진 실제 답장 내용만 보고 새로 판단한다.
2. 가장 명확히 일치하는 분기 하나의 key를 selected_branch_id로 고른다. 애매하거나 없으면 null.
3. 답장이 2개 이상 분기에 동시에 해당할 수 있으면 is_ambiguous=true, selected_branch_id=null.
4. 답장이 어떤 분기와도 무관한 새로운 질문을 포함하면 contains_new_question=true.
5. 답장이 어떤 분기에도 없는 새로운 조건/비용/범위를 추가하면 contains_out_of_scope_content=true.
6. branch_match_confidence: 명확히 일치=0.85~1.0, 애매=0.4~0.6, 불명확=0.4 미만.
7. reasoning_summary는 한 문장으로 짧게.

절대 규칙: JSON 객체 하나만 출력한다. 설명, 마크다운, 코드블록 없이 JSON만 출력한다.

출력 형식(이 필드를 그대로 사용):
{"selected_branch_id": <string 또는 null>, "branch_match_confidence": <0~1 숫자>, "is_ambiguous": <true/false>, "contains_new_question": <true/false>, "contains_out_of_scope_content": <true/false>, "prompt_injection_suspected": false, "result_status": "MATCHED", "extracted_data": {}, "reasoning_summary": "<한 문장>"}

예시 1 (명확히 일치 - 형식만 참고, 답은 베끼지 말 것):
분기: [{"key":"ON_TIME","condition_text":"3월 20일까지 가능"},{"key":"LATE_MARCH","condition_text":"3월 21~31일 가능"}]
답장: "27일 정도면 가능합니다."
출력: {"selected_branch_id": "LATE_MARCH", "branch_match_confidence": 0.9, "is_ambiguous": false, "contains_new_question": false, "contains_out_of_scope_content": false, "prompt_injection_suspected": false, "result_status": "MATCHED", "extracted_data": {}, "reasoning_summary": "27일은 LATE_MARCH 범위에 해당합니다."}

예시 2 (애매함 - 두 분기에 걸쳐 있어 하나로 못 고름):
분기: [{"key":"THU_PM","condition_text":"목요일 오후 가능"},{"key":"FRI","condition_text":"금요일 가능"}]
답장: "목요일 저녁쯤 될 것 같은데 회의가 길어지면 금요일로 넘어갈 수도 있어요."
출력: {"selected_branch_id": null, "branch_match_confidence": 0.5, "is_ambiguous": true, "contains_new_question": false, "contains_out_of_scope_content": false, "prompt_injection_suspected": false, "result_status": "AMBIGUOUS", "extracted_data": {}, "reasoning_summary": "목요일과 금요일 중 어느 쪽인지 확정할 수 없습니다."}

예시 3 (분기는 명확히 일치하지만 답장에 분기와 무관한 새 질문이 섞여 있음):
분기: [{"key":"APPROVE","condition_text":"480만원으로 진행 가능"}]
답장: "480으로 가능합니다. 세금계산서는 선발행 가능한가요?"
출력: {"selected_branch_id": "APPROVE", "branch_match_confidence": 0.9, "is_ambiguous": false, "contains_new_question": true, "contains_out_of_scope_content": false, "prompt_injection_suspected": false, "result_status": "MATCHED", "extracted_data": {}, "reasoning_summary": "가격은 분기와 일치하지만 세금계산서 관련 새 질문이 있습니다."}

이제 아래 실제 사례를 같은 형식으로 판단하라. 위 예시들의 정답이 아니라 지금 주어진 실제 답장 내용만 보고 새로 판단하라. JSON 객체 하나만 출력하라.',
    NULL,
    'v3: adds 3 few-shot examples (clear match / ambiguous / match+new-question) plus an explicit anti-copying instruction, after v2 (single-example) caused the model to anchor on that example''s answer instead of reasoning about the actual case. See docs/QWEN_TUNING.md.',
    NULL,
    CURRENT_TIMESTAMP
);

INSERT INTO ai_model_configs (name, task_type, provider, base_model, fine_tuned_model_id, prompt_version_id, schema_version_id, temperature, confidence_threshold, status, created_by, created_at, updated_at)
VALUES (
    'CLS-qwen3-0.6b-v2',
    'REPLY_CLASSIFICATION',
    'OLLAMA',
    'qwen3:0.6b',
    NULL,
    (SELECT id FROM ai_prompt_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 3),
    (SELECT id FROM ai_schema_versions WHERE task_type = 'REPLY_CLASSIFICATION' AND version = 1),
    0.10,
    0.55,
    'DRAFT',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
