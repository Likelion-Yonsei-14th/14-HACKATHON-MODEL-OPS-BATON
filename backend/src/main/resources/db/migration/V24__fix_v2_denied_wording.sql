-- Run 22 (CLS-qwen2.5-7b-v2-strict on BATON Scenario Dataset v2) diagnosis: after fixing
-- BUDGET_HOLD's language (V23), the model started confusing DENIED replies with BUDGET_HOLD in the
-- travel-approval scenarios (3/5). The reply text said "can't be approved this quarter", which reads
-- semantically closer to "needs more review/hold" than to a clean rejection -- a genuine wording
-- overlap I introduced, not a model reasoning failure. Reworded to unambiguous decline language.
UPDATE eval_reply_cases SET reply_messages_json = '["We have to decline the trip -- anything after Nov 20 will not work for us this quarter."]'
WHERE scenario_id = (SELECT id FROM eval_scenarios WHERE external_key = 'BATON2-031' AND dataset_id = (SELECT id FROM eval_datasets WHERE name = 'BATON Scenario Dataset v2 (generalization) (2.0.0)')) AND notes = 'R4';

UPDATE eval_reply_cases SET reply_messages_json = '["We have to decline the trip -- anything after Jan 15 will not work for us this quarter."]'
WHERE scenario_id = (SELECT id FROM eval_scenarios WHERE external_key = 'BATON2-032' AND dataset_id = (SELECT id FROM eval_datasets WHERE name = 'BATON Scenario Dataset v2 (generalization) (2.0.0)')) AND notes = 'R4';

UPDATE eval_reply_cases SET reply_messages_json = '["We have to decline the trip -- anything after Mar 10 will not work for us this quarter."]'
WHERE scenario_id = (SELECT id FROM eval_scenarios WHERE external_key = 'BATON2-033' AND dataset_id = (SELECT id FROM eval_datasets WHERE name = 'BATON Scenario Dataset v2 (generalization) (2.0.0)')) AND notes = 'R4';

UPDATE eval_reply_cases SET reply_messages_json = '["We have to decline the trip -- anything after Feb 16 will not work for us this quarter."]'
WHERE scenario_id = (SELECT id FROM eval_scenarios WHERE external_key = 'BATON2-034' AND dataset_id = (SELECT id FROM eval_datasets WHERE name = 'BATON Scenario Dataset v2 (generalization) (2.0.0)')) AND notes = 'R4';

UPDATE eval_reply_cases SET reply_messages_json = '["We have to decline the trip -- anything after Apr 9 will not work for us this quarter."]'
WHERE scenario_id = (SELECT id FROM eval_scenarios WHERE external_key = 'BATON2-035' AND dataset_id = (SELECT id FROM eval_datasets WHERE name = 'BATON Scenario Dataset v2 (generalization) (2.0.0)')) AND notes = 'R4';
