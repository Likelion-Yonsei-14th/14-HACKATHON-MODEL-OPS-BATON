# Qwen3 0.6B Classification 튜닝 기록

`CLS-qwen3-0.6b-*` Model Config들로 `BATON Scenario Dataset v1`의 SMOKE split(56건, 시나리오
BATON-001~008)에 대해 반복한 실험 기록. 전부 `provider=OLLAMA`, `temperature=0.1`,
`confidence_threshold=0.55`.

## 결과 요약

| Config | Prompt | Schema | Branch Accuracy | Ambiguous Recall | New Question Recall | Out-of-Scope Recall | False Auto-Send | Latency/case |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CLS-qwen3-0.6b-v1 | v2 (단일 few-shot 1개) | multi-boolean | 0.50 | 0.0 | 0.0 | 0.0 | 0.958 | ~20s |
| CLS-qwen3-0.6b-v2 | v3 (few-shot 3개, 상태별) | multi-boolean | 0.4375 | 0.0 | 0.25 | 0.0 | 0.75 | ~28s |
| CLS-qwen3-0.6b-v3 | v4 (reasoning_summary 필드를 맨 앞으로) | multi-boolean | 0.4792 | 0.0 | 0.125 | 0.0 | 0.875 | ~32s |
| CLS-qwen3-0.6b-v4-decisionstate | v5 (5-state enum + few-shot 5개) | compact state | 0.4444 | 0.0 | 0.333 | 0.0 | 0.778 | ~24s |

## 진단

1. **일관된 실패 패턴** (v1, v2 raw output 전수 확인): `selected_branch_id`는 거의 항상 채워짐(null
   응답 거의 없음), `is_ambiguous`/`contains_out_of_scope_content`는 사실상 항상 `false`,
   `branch_match_confidence`는 사례와 무관하게 0.85~0.9에 고정. 즉 모델이 case-by-case로 값을
   바꾸는 게 아니라 "습관적 기본값"을 반환하고 있음.
2. **날짜/숫자 범위 비교 자체가 불안정**: 동일하게 "27일"이 언급된 서로 다른 사례에서 ON_TIME과
   LATE_MARCH를 오락가락함 (예: eval_results id 58, 60, 63 비교) — few-shot 예시를 베끼는 문제가
   아니라 순수 산술/범위 비교 능력의 한계로 보임.
3. **reasoning_summary를 먼저 쓰게 해도(v4/v3-config) 패턴이 안 바뀜** — 실제로 돌려본 결과
   (branch_match_accuracy 0.4792, false_auto_send 0.875, ambiguous/out_of_scope recall 여전히 0.0)
   "브랜치부터 정하고 나중에 합리화한다"는 순서 문제가 아니었음이 확인됨. 애초에 "브랜치 매칭 +
   4개 독립 boolean 판단"을 한 번에 하는 것 자체가 이 모델 용량을 넘는 것으로 보임 (multi-task
   overload).
4. **Eval 하네스 자체 버그는 아님**: `schema_validity`는 항상 1.0 (JSON 파싱은 항상 성공),
   `AutoSendGuardrail`은 confidence/4-boolean/branch-validity를 전부 AND로 정확히 결합함(기존
   단위테스트로 검증됨). `BATON Scenario Dataset v1`의 BATON-001 R6 라벨(27일→ON_TIME 기대)은 golden
   branch 정의(LATE_MARCH가 21~31일)와 어긋나는 것으로 보이는 라벨 오류가 하나 있으나, 이 정도
   개별 오류가 0%→95%대 격차를 설명하지는 못함.

## Decision-state-first (v5 / CLS-qwen3-0.6b-v4-decisionstate) — 실행 완료, 유의미한 개선 없음

4개 독립 boolean 대신 5지선다 단일 `state` enum(`SAFE_MATCH`/`AMBIGUOUS`/`NEW_QUESTION`/
`OUT_OF_SCOPE`/`NO_MATCH`)으로 스키마를 압축. `ClassificationEvalRunnerService`가 `state` 필드
존재 여부로 두 스키마를 모두 파싱하도록 호환 처리되어 있어 기존 v1~v4 config는 그대로 동작한다
(하위 호환 유지).

결과: branch_match_accuracy 0.4444, ambiguous/out_of_scope recall 여전히 0.0, false_auto_send
0.778 — v1~v3 대비 유의미한 개선 없음. Schema 압축 자체는 정답이 아니었다.

## 결정적 진단: Input Blindness (긴 컨텍스트에서 실제 답장을 못 읽음)

Decision-state 실험의 raw output을 원본 입력(`input_snapshot_json`)과 대조한 결과, 근본 원인이
드러남. BATON-003(견적 협상) 시나리오의 R1~R5를 보면:

```text
R1 "네, 480만 원으로 이번 주 발주 가능합니다."           -> APPROVE (정답)
R2 "450만 원까지 낮아지면 가능합니다."                    -> APPROVE (오답, NEGOTIATE가 정답)
R3 "결재권자가 출장 중이라 다음 주에야 확인됩니다."        -> APPROVE (오답, DELAY가 정답)
R4 "이번 분기 예산으로는 어렵습니다."                     -> APPROVE (오답, REJECT가 정답)
```

R3의 실제 모델 reasoning_summary: *"480 million yen is within the approved timeframe..."* —
답장에 없는 내용을 지어냈다. 입력이 캐싱되거나 재사용된 게 아니라(각 case의 `reply_messages`는
DB에 실제로 다르게 저장되어 있음을 확인함), **모델이 실제로 새 입력을 읽지 않고 시나리오의 branch
목록 + few-shot 예시 패턴만으로 그럴듯한 답을 생성하는 것**으로 보인다. 이는 프롬프트 길이(질문 +
분기 4개 + few-shot 예시 5개 + 답장)가 0.6B 모델이 안정적으로 attend할 수 있는 유효 컨텍스트를
넘어섰다는 뜻이다. 이 실패는 스키마를 어떻게 바꿔도 고쳐지지 않는다 — 애초에 "지금 이 답장이
뭐라고 했는지"를 모델이 참조하지 못하고 있기 때문이다.

## 결론: C (구조적 부적합) — 단, 조건부

**단일 호출 안에 "질문 + 분기 여러 개 + 답장 + 지시사항 + few-shot"을 전부 넣는 현재 구조로는
qwen3:0.6b는 BATON Classification에 구조적으로 부적합하다.** 4번의 서로 다른 prompt/schema
설계(v1~v4)가 전부 비슷한 실패 폭(false_auto_send 75~96%, ambiguous/out_of_scope recall 0%)에
수렴했고, 마지막 실험은 모델이 입력 자체를 놓치고 있다는 직접 증거까지 나왔다 — prompt 문구를 더
다듬는 다섯 번째 시도는 기대값이 낮다.

다만 "0.6B는 무조건 못 쓴다"는 결론은 아니다. 근본 원인이 **컨텍스트 길이/task 개수**이지 모델의
언어 이해력 자체가 0이라는 뜻은 아니다 (branch_match_accuracy가 완전 랜덤보다는 높고, JSON
schema 준수는 100%). 진짜 다음 실험은 Experiment 3/4(Task Decomposition)이다 — 분기 하나씩,
"이 답장이 이 조건과 일치하는가? YES/NO"처럼 프롬프트를 극단적으로 짧게 쪼개면 결과가 달라질
가능성이 있다. 단, 이건 이번 세션에서 시도하지 않았다 — 호출 수가 늘어나고(N분기 × M케이스), 지금
CPU 환경에서 호출당 20~30초가 걸리는 걸 고려하면 eval 자체가 비현실적으로 오래 걸린다(SMOKE
기준으로도 수십 분~1시간대). Task decomposition을 실제로 검증하려면 (a) 더 빠른 추론 환경(GPU
또는 더 작은 quantization) 또는 (b) eval을 비동기 job으로 돌리는 구조 변경이 선행되어야 한다.

## 중단 기준 — 이미 충족됨

- Ambiguous Recall < 0.3 → **0.0, 4번의 실험 전부 충족 안 됨**
- Out-of-Scope Recall < 0.3 → **0.0, 4번의 실험 전부 충족 안 됨**
- False Auto-Send > 0.5 → **모든 실험에서 0.75 이상**

세 기준 모두 4번 연속 미달이므로, 현재 구조(단일 호출, qwen3:0.6b)에서의 프롬프트 튜닝은 여기서
중단한다. **이 상태로 Production에 절대 승격하지 않는다** — Model Lab의 "Production은 Task당
1개, immutable, Promote 시 확인 절차" 안전장치가 정확히 이런 상황을 막기 위한 것이다. 지금까지의
모든 `CLS-qwen3-0.6b-*` config는 DRAFT 상태로 남겨둔다.

## 다음으로 시도할 것 (이번 세션 범위 밖)

1. **Task Decomposition (Experiment 3/4)**: 분기 매칭과 안전 신호 판단을 분리된 호출로 쪼갠다.
   가장 유력한 다음 실험이지만 CPU 환경에서 호출 수 증가는 evaluation 시간을 크게 늘린다.
2. **더 큰 로컬 모델**: qwen2.5:1.5b 또는 3b 등, 같은 Ollama 인프라에서 시도 가능한 다음 크기.
3. **OpenAI 모델을 이 데이터셋 기준선으로 확보**: `CLS-seed-v1`(OpenAI, gpt-4o-mini)을 이
   dataset(v1, 350건)에 대해 실행해 실제 상한선을 확인하지 않았다 — 로컬 모델과의 격차를
   정량적으로 비교하려면 이것부터 하는 게 맞다.
