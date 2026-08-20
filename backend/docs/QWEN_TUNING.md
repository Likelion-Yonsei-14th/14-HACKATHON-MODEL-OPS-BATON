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

## Follow-up 1: OpenAI 기준선 — 계정 쿼터로 차단됨

`CLS-seed-v1`(OpenAI, gpt-4o-mini)로 이 dataset의 실제 상한선을 확인하려 했으나, 연결된 OpenAI
계정이 **RPD(하루 요청 한도) 50건 무료 티어**라 이미 소진된 상태였다. 재시도 백오프
(`OpenAiClient`에 429 재시도 로직 추가함, 최대 5회/8초 간격)를 넣었지만 RPM이 아니라 RPD(일일)
한도라 재시도로는 못 뚫는다 — 결제수단을 계정에 등록해야 풀리는 문제라 이 세션에서는 해결 불가.
필요하면 OpenAI 계정에 결제수단 등록 후 `CLS-seed-v1` config로 SMOKE run을 실행해서 기준선을
잡을 것.

## Follow-up 2: qwen2.5:1.5b (더 큰 로컬 모델) — 부분적 개선, 아직 production 아님

`qwen2.5:1.5b`(qwen3:0.6b 대비 2.5배 큰 모델, Ollama에 추가로 pull)로 두 번 테스트:

| Config | Prompt | 비고 | Branch Accuracy | Ambiguous Recall | New Question Recall | Out-of-Scope Recall | False Auto-Send | Latency/case |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CLS-qwen2.5-1.5b-v1 | v1 (영어 원본 seed) | 언어 불일치로 중국어 추론 확인, 무효 | 0.1042 | 0.0 | 0.0 | 0.0 | 0.4583* | ~8.5s |
| CLS-qwen2.5-1.5b-v2-decisionstate | v5 (한국어, decision-state) | SMOKE 56건 전체 | **0.3125** | **0.5** | 0.375 | 0.0 | **0.3333** | ~5.6s |

*v1 실행의 낮은 False Auto-Send는 실제 안전 판단이 아니라 `selected_branch_id`가 스키마와 안 맞아
거의 항상 무효화된 부작용 — 무시할 것. v2(한국어 프롬프트)가 처음으로 언어를 맞춘 공정한 비교다.

**첫 유의미한 신호**: v2에서 실제 ambiguous case(BATON-002의 진짜 애매한 답장)를 처음으로 정확히
`AMBIGUOUS`로 잡아냈다 — 0.6b는 8번의 실험 내내 단 한 번도 진짜 ambiguous를 못 잡았다. 8번의
실험 전체를 통틀어 False Auto-Send가 처음으로 50% 밑으로(33.3%) 내려왔다.

**하지만 여전히 미달**: branch_match_accuracy는 여전히 0.31로 낮고(날짜 산술 오류 지속 — "4월
첫째 주"를 LATE_MARCH로 잘못 판단하는 등), out_of_scope_recall은 여전히 0.0, false_auto_send
33.3%는 프로덕션 기준(< 0.05)과는 거리가 멀다. 또한 한 사례(id 294)에서 해당 시나리오에 존재하지
않는 "APPROVE" 조건을 reasoning에서 언급하는 등 다른 시나리오와의 컨텍스트 혼동으로 보이는 현상도
관찰됨 — few-shot 예시(APPROVE가 예시 3/4에 등장)를 실제 케이스와 혼동하는 것으로 추정.

**결론**: 모델을 키우니 방향은 맞게 개선됐다(질적으로 다른 실패 모드: 0.6b는 신호를 아예 못 읽고,
1.5b는 신호를 감지는 하지만 아직 부정확). 이는 Model Lab 결론 B(Task decomposition 필요)를
약화시키지 않는다 — 오히려 "모델 크기를 계속 올리는 것"과 "task를 쪼개는 것" 둘 다 유효한 다음
방향이라는 근거가 된다. 다음 시도 우선순위:

1. qwen2.5:3b 또는 7b로 같은 v5 프롬프트 재현 (더 큰 모델일수록 서버 메모리 여유 확인 필요 —
   현재 서버는 3.6GB RAM, 1.5b도 빠듯함)
2. Task Decomposition (분기 매칭 / 안전 판단 분리 호출) — 여전히 유효한 방향, 특히 out_of_scope
   가 모든 모델 크기에서 0%인 것을 보면 "한 번에 여러 신호"가 근본 병목일 가능성이 높음
3. OpenAI 결제수단 등록 후 기준선 확보 — 로컬 모델 격차를 정량화하는 데 필요

## Follow-up 3: 데스크탑 GPU(RTX 2060) + Task Decomposition + qwen2.5:7b — 목표 달성

가비아 서버가 CPU-only라 eval 한 번에 20~30분씩 걸리는 문제를 해결하려고, 팀원 데스크탑의 Ollama를
Tailscale로 가비아 서버에 연결했다(`OLLAMA_BASE_URL`을 데스크탑 Tailscale IP로 변경, 코드 변경
없음). 그 결과 latency가 5~30초/case → 0.2~7초/case로 떨어져 CORE(294건) 전체를 20분 안에 돌릴 수
있게 됐다 — 이게 이후 모든 실험의 반복 속도를 근본적으로 바꿨다.

동시에 두 가지를 시도했다: (a) Task Decomposition을 실제로 구현(`ClassificationEvalRunnerService.
classifyTwoStage` — stage1 안전 판정만, stage2 분기 선택만, `ai_model_configs.stage2_prompt_
version_id`로 마킹), (b) `qwen2.5:7b`(1.5b의 5배 크기)로 재현.

**핵심 발견: 모델 크기가 지배적 변수였다.** two-stage 분리 자체는 미미한 개선에 그쳤지만(branch
accuracy 0.31→0.33), 같은 프롬프트를 qwen2.5:7b로만 바꿨을 때 branch accuracy 0.58, **False
Auto-Send가 처음으로 0%**를 기록했다 (SMOKE, run 16). CORE(294건, run 17~18)에서도 False
Auto-Send 1.53~6.35%로 유지됐다. **threshold를 0.55→0.90/0.95로 올리는 것도 유효한 레버였다**
(같은 config, threshold만 올려서 False Auto-Send 33%→17%→1.5%까지 내려감, 단 그만큼
Auto-Send Coverage도 같이 낮아짐 — trade-off, 다만 confidence가 실제로 correctness와 상관관계가
있을 때만 threshold가 의미 있다. qwen3/1.5b에서는 confidence가 거의 상수라 threshold를 올려도
"거의 다 막힘" 아니면 "거의 다 통과" 둘 중 하나였는데, 7b는 threshold 0.90 vs 0.95 사이에서 실제
False Auto-Send가 유의미하게 갈렸다 — 즉 7b는 confidence가 처음으로 진짜 판별력을 가짐).

**최종 후보로 확정: `CLS-qwen2.5-7b-v2-strict`** (id=13, prompt v5 decision-state, threshold
0.95). CORE 299건: branch accuracy 0.65, False Auto-Send **0.0153** (production 기준 < 0.05
최초 통과).

시도했지만 폐기한 것 (`CLS-qwen2.5-7b-v3-tightened`, prompt v8): "NO_MATCH 남발하지 마라"는
지시를 추가했더니 branch accuracy는 그대로인데 ambiguous/new_question recall이 급락하고
False Auto-Send가 다시 0.076으로 악화됨 — 모델이 안전 신호 전반을 덜 잡는 쪽으로 과보정된 것.
프롬프트를 한 방향으로 밀면 다른 방향이 깨지는 전형적 trade-off라 되돌림.

## Follow-up 4: 두 번째 독립 데이터셋으로 일반화 검증 — v1의 낮은 정확도는 모델 한계가 아니라 도메인 난이도였다

`BATON Scenario Dataset v2 (generalization)`: v1과 동일 규모(50 시나리오/350건)지만 완전히 다른
10개 도메인(보증 심사, 레스토랑 예약, 수강신청, 출장 승인, 채용 온보딩, 제조 불량률, 웨딩홀 예약,
환불, API rate limit, 사무실 재계약)으로 새로 작성. 의도적으로 날짜 범위 조건 비중을 낮추고
가격/수량/승인여부 같은 비-날짜 조건을 늘림 (`scratch/gen_dataset_v2.py`로 생성, 마이그레이션
V22).

같은 config(`CLS-qwen2.5-7b-v2-strict`)로 그대로 실행한 결과:

| 지표 | v1 (날짜 위주) | v2 (일반 도메인) |
| --- | --- | --- |
| Branch Accuracy | 65.1% | **85.7%** |
| False Auto-Send | 1.53% | **0%** |
| Ambiguous Recall | 33.3% | 85.7% |
| New Question Recall | 73.8% | 100% |
| Out-of-Scope Recall | 64.3% | 66.7% |

**결론 확정**: 모델/프롬프트/threshold 전부 동일한데 도메인만 바꿨더니 branch accuracy가
20%p 넘게 올랐다. v1의 65%는 모델의 일반적 한계가 아니라 **그 데이터셋 특유의 날짜 범위 산술
난이도** 때문이었다. 일반적인 비-날짜 도메인에서는 이미 실사용 가능한 수준.

### DateRangeGuardrail 영어 확장

v2 데이터셋의 국제 시나리오(영어 날짜: "Sep 15", "Before Oct 1", "After Oct 7" 등)에서 모델이
날짜 산술을 놓치는 사례를 발견해, 결정론적 날짜 파서(`DateRangeGuardrail`)를 영어 월 이름까지
지원하도록 확장(V21). 단위테스트로 실제 실패 케이스("Sep 30 if compliance clears...; otherwise
Oct 4." → 정확히 AMBIGUOUS로 판정) 검증 완료.

### v2 데이터셋 자체의 저작 버그 2건 발견 및 수정

실패 케이스를 파다 보니 모델이 아니라 **내가 만든 데이터셋의 문구 버그**를 2건 발견:
1. 여행 승인 시나리오(BATON2-031~035)에서 영어 시나리오인데 `BUDGET_HOLD` 분기 조건문만 한국어로
   남아있어 모델이 매칭을 못 함 → 영어로 통일(V23).
2. 교육 시나리오 R7(out-of-scope) 문구 "커리큘럼이 변경됩니다"가 out-of-scope라기엔 애매해서
   "수강료가 10% 인상됩니다"로 명확화(V23).
3. 위 수정 후 새로 드러난 문제: DENIED 답장 문구("can't be approved")가 "보류/재검토"
   뉘앙스와 겹쳐 BUDGET_HOLD와 계속 혼동됨 → "decline"으로 명확히 재작성(V24).

세 번 다 재실행해서 회귀 확인함 — branch accuracy/False Auto-Send 지속 개선, 부작용 없음.

### R4 특이 패턴: "분기가 전부 날짜 조건은 아니다" — 프롬프트 구조적 개선 (v9)

reply-type별(R1~R7) pass rate를 쪼개보니 R4(4번째 clear-match 패턴)만 유독 50%로 낮았다(나머지는
81~95%). 원인: 채용 온보딩(DECLINE, "오퍼 거절 의사"), 웨딩(DEPOSIT_NEEDED, "가계약금 입금")처럼
분기 목록에 날짜형과 비날짜형이 섞여 있으면, 모델이 비날짜 분기까지 "날짜 범위에 들어가는가"로
검사하려다 실패하고 NO_MATCH로 도피하는 패턴이 3개 무관한 도메인에서 반복됨.

prompt v9로 "분기가 전부 날짜 조건은 아니다, 날짜가 없어도 의미가 통하면 매치다" 지침 + 비날짜
few-shot 추가 → **branch accuracy 65%→92%까지 상승** (`CLS-qwen2.5-7b-v4-nondate`, CORE 294건,
run 25). 그러나 **confidence가 전부 0.9 이하로 눌려서 threshold 0.95에서 auto_send_coverage가
정확히 0%**가 됨(아무 것도 통과 못 함). threshold를 0.90/0.85로 낮춰봤지만 그 순간 False
Auto-Send가 37~40%로 폭발(run 26/27) — v9의 confidence는 0.85~0.9 구간에서 맞고 틀림을 전혀
구분하지 못한다는 뜻.

**최종 결론**: v9는 raw 분류 정확도(92%)는 최고지만 confidence를 안전판으로 쓸 수 없어서 (어떤
threshold를 줘도 "다 막힘" 아니면 "다 뚫림") production 후보에서 제외. **`CLS-qwen2.5-7b-v2-strict`
(v5 prompt + threshold 0.95)가 최종 확정 후보로 유지된다** — branch accuracy는 v9보다 낮지만
(v1 65%/v2 86%), 1순위 지표인 False Auto-Send를 실제로 안전하게 지킨다.

## Branch Generation (Track A) — 처음 테스트, 빠르게 실사용 가능 수준 도달

Classification만 튜닝하다가 Generation(질문 → 분기 후보 생성)도 처음 테스트했다. 발견/수정한
것:

1. **`GenerationEvalRunnerService`가 OpenAI에 하드코딩되어 있었다** — Ollama provider 분기가 아예
   없어서 로컬 모델을 못 썼다. `ClassificationEvalRunnerService.callModel`과 동일한 패턴으로
   provider dispatch 추가.
2. **`BATON Generation Eval v1`** 데이터셋 신설(39 시나리오) — Classification v1/v2 데이터셋의
   질문(question/context)을 재사용, golden_branches는 Generation에 안 쓰므로 비움 (spec 3번:
   Classification 정답을 Generation 검증에 쓰지 않는다의 반대 방향도 마찬가지로 지킴 — 재사용은
   trigger question 텍스트뿐).
3. 첫 실행(`GEN-qwen2.5-7b-v1`, 영어 seed prompt, SMOKE 8건): hard rule pass 62.5%. 실패 원인
   전수 확인 결과 전부 "두 번째 분기부터 name 필드 누락" — 그리고 **Classification 때와 동일한
   패턴으로, 한국어/일본어 질문인데 condition_text/decision_text가 중국어로 새는 현상**을 발견
   (response_text는 정상적으로 입력 언어 유지 — 사용자에게 실제로 나가는 텍스트는 안전했지만,
   관리자가 사전 승인 화면에서 보는 condition_text/decision_text가 중국어로 보이는 건 실사용
   불가).
4. **한국어 prompt v2**로 교체(영어 지시문이 원인으로 보임 — Classification 때와 같은 결론) +
   "두 번째 이후 분기에도 name을 반드시 채워라" 명시 + 2-branch few-shot(둘 다 필드 완전히 채운
   예시). 결과: **SMOKE 8/8 (100%), CORE 30/31 (96.8%)** hard rule 통과, 언어 이탈도 전수 확인
   결과 완전히 사라짐 (`GEN-qwen2.5-7b-v2-korean`).

**중요한 한계**: hard rule은 스키마 유효성(분기 개수/name/condition/decision 필드 존재)만
확인한다. spec 4.3이 요구하는 진짜 품질 지표(Coverage/Separation/Granularity/Pre-decidability/
Naturalness/Safety)는 Human Review 몫이다. 아래는 그 hard-rule을 통과한 39건(v2) 전체를 사람이
직접 읽고 진단한 결과다 — `GenerationHumanReviewService` UI를 통한 정식 점수 매기기는 아직
안 했고, 이건 그 전 단계의 정성적 결함 진단이다.

### 사람이 직접 읽고 찾은 7개 실질 결함 (hard rule로는 안 걸림)

1. **날짜/사실 지어내기**: 입력에 날짜 정보가 전혀 없는데 response_text에 "가능한 날짜는 4월
   5일입니다"처럼 구체적 날짜를 지어냄 — spec 4.2 핵심 금지 규칙 정면 위반.
2. **관점 역전**: "당신은 언제 입사 가능하신가요?"처럼 상대방에게 묻는 메시지인데, 생성된 분기가
   "우리 회사는 다음 주부터 가능합니다"처럼 우리가 통보하는 것으로 뒤집힘.
3. **condition_text가 상대방 답장이 아니라 우리 내부 절차**: "재무팀 승인 필요", "확인 후 답변"
   같은 우리 쪽 프로세스를 조건으로 씀 — 조건은 반드시 상대방 답장에서 확인 가능한 내용이어야
   한다.
4. **분기 조건 겹침**: "1년 이내"와 "8~12개월 사이"가 겹쳐서 8개월 케이스가 두 분기 모두에
   해당하는 모순 발생.
5. **response_text가 되물음으로 끝남**: "인터뷰 진행해도 좋을까요?"처럼 이미 승인된 결정을
   전달해야 할 자리에 다시 질문을 던짐 — BATON의 "왕복 줄이기" 취지에 정면으로 반함.
6. **모순된 톤**: 좋은 소식(자리 있음)에 "죄송합니다"를 붙이는 등 내용과 톤 불일치.
7. **언어 이탈**: 한국어/일본어/영어 질문인데 condition_text/decision_text/response_text가
   중국어로 새는 현상 — Classification 때와 동일한 qwen 계열 특성.

### v3 (7개 결함 타겟 수정) — 6개 완전 해결, 1개 부분 해결

`GEN-qwen2.5-7b-v3-qualityfix` (prompt v3, temperature 0.3→0.1): 관점/조건정의/hallucination/
겹침/되물음/모순 6개는 CORE 31건 재검토에서 전부 해결 확인. **언어 이탈만 부분 개선**
(8개 국제 시나리오 중 8개 전부 문제 있던 것 → 2개만 남음).

### v4 (언어 이탈 추가 수정 시도) — 역효과로 폐기

"중국어(简体字/繁体字)는 절대 쓰지 마라"를 명시적으로 추가했더니 **오히려 악화** — 국제
시나리오 8건 중 5건이 중국어 또는 심지어 러시아어까지 섞이며 무너짐(Dubai 시나리오는 중국어+
러시아어+깨진 JSON 텍스트 조각까지 섞임). 특정 언어를 이름으로 지목하는 부정 지시가 그 언어를
오히려 활성화시키는 역설적 효과로 보인다. **v4는 폐기, v3를 최종 후보로 확정.**

### 최종 확정: `GEN-qwen2.5-7b-v3-qualityfix`

31건 중 29건(93.5%) 완전히 정상, 2건만 언어 이탈 잔존. Coverage/Naturalness 같은 정식 Human
Review 점수는 아직 `/batons/models/generation`에서 사람이 매겨야 한다 — 이 진단은 그 전 단계
결함 스캔이며 정식 리뷰를 대체하지 않는다.

## 현재 상태 요약 (두 트랙 모두)

| Track | 최종 후보 Config | 검증 규모 | 핵심 지표 |
| --- | --- | --- | --- |
| Classification | `CLS-qwen2.5-7b-v2-strict` (id=13) | v1 299건 + v2 294건 | False Auto-Send 0~1.5%, Branch Accuracy 65~86% |
| Generation | `GEN-qwen2.5-7b-v3-qualityfix` (id=20) | 39건 + 31건 재검토 | Hard rule 96.8~100%, 정성적 결함 진단 완료(7개 중 6개 해결), 정식 Human Review 점수는 미실시 |

둘 다 여전히 DRAFT 상태 — Production 승격은 spec 15번 절차(Promote 확인 UI)를 통해 사람이
명시적으로 결정할 것.
