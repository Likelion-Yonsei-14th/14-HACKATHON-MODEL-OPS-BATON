-- Two dataset-authoring bugs found while diagnosing run 21 (CLS-qwen2.5-7b-v2-strict on
-- BATON Scenario Dataset v2, docs/QWEN_TUNING.md), not model/prompt problems:
--
-- 1. The 5 travel-approval scenarios (BATON2-031..035) are entirely in English except one branch's
--    condition_text ("BUDGET_HOLD": "예산 보류, 추가 승인 필요"), left in Korean by copy-paste from
--    a different domain template. The model consistently failed to match English budget-hold replies
--    against that one Korean-only condition -- fixed by translating it to match the scenario's language.
-- 2. The 5 education scenarios' R7 (out-of-scope) reply used "이번 기수부터 커리큘럼이 변경됩니다"
--    (curriculum content changes), which reads more like incidental new information than an
--    unapproved new condition/cost -- genuinely ambiguous phrasing, not a model failure. Reworded to
--    an unambiguous added-cost example, consistent with every other domain's R7 pattern in this
--    dataset (an added condition/cost, not a content change).
UPDATE eval_scenarios
SET golden_branches_json = replace(golden_branches_json, '"예산 보류, 추가 승인 필요"', '"Requires additional budget sign-off"')
WHERE dataset_id = (SELECT id FROM eval_datasets WHERE name = 'BATON Scenario Dataset v2 (generalization) (2.0.0)')
  AND external_key IN ('BATON2-031','BATON2-032','BATON2-033','BATON2-034','BATON2-035');

UPDATE eval_reply_cases
SET reply_messages_json = replace(reply_messages_json, '"이번 기수부터 커리큘럼이 변경됩니다."', '"대신 이번 기수부터 수강료가 10% 인상됩니다."')
WHERE scenario_id IN (
    SELECT id FROM eval_scenarios
    WHERE dataset_id = (SELECT id FROM eval_datasets WHERE name = 'BATON Scenario Dataset v2 (generalization) (2.0.0)')
      AND external_key IN ('BATON2-021','BATON2-022','BATON2-023','BATON2-024','BATON2-025')
)
AND notes = 'R7';
