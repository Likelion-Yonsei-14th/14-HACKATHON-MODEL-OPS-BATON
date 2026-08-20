# BATON Model Lab

BATON에서 사용하는 AI 기능(Branch 자동 생성, 답장 자동 분류)을 실제 서버 환경에서 반복적으로 실험하고, Prompt·Model·Threshold·Fine-tuning 결과를 평가하고, 검증된 설정만 Production BATON에 배포할 수 있게 하는 내부 AI 개발·운영 콘솔입니다.

프로덕션 BATON 서비스([Frontend](https://github.com/Likelion-Yonsei-14th/14-HACKATHON-FRONTEND-BATON), [Backend](https://github.com/Likelion-Yonsei-14th/14-HACKATHON-BACKEND-BATON))와 별도 저장소로 운영되며, 이 저장소는 그 두 프로젝트를 기반으로 Model Lab 도메인만 추가한 독립적인 풀스택 애플리케이션입니다.

```text
Prompt Engineering
→ Eval (Classification / Generation)
→ Model Comparison
→ Threshold Tuning
→ Fine-tuning
→ Production 승격 / Rollback
```

## 핵심 철학

```text
Human Decision
↓
AI Interpretation
↓
Deterministic Validation / Rule Engine
↓
Action
```

> The human decides. AI interprets. BATON executes.
> Generation은 적극적이어도 되지만 Execution은 보수적이어야 한다.
> False Auto-Send는 일반적인 오분류보다 훨씬 심각한 오류다.

Model Lab의 목표는 Prompt/Model/Threshold를 감으로 바꾸는 것이 아니라, 동일한 Dataset과 기록된 결과를 바탕으로 반복 가능하게 실험하고 안전하게 Production에 배포하는 것입니다.

## 구성

```text
backend/   Spring Boot 4.1.0 / Java 17 — BATON 프로덕션 백엔드 + domain/modellab 추가
frontend/  React 19 + Vite + TypeScript — BATON 프로덕션 프론트엔드 + /batons/models 추가
```

두 프로젝트 모두 원본 BATON FE/BE 저장소를 그대로 복제한 뒤, Model Lab 기능을 별도 도메인(`domain/modellab`, `pages/modellab`)으로 얹은 구조입니다. 기존 API·테이블·화면은 수정하지 않았습니다.

## 접근 경로

```text
/batons/models
```

로컬 개발 서버 기준 예: `http://localhost:5173/batons/models`

Model Lab의 모든 API(`/api/model-lab/**`)는 `users.is_admin = true`인 계정만 호출할 수 있습니다. 최초 관리자 계정은 DB에서 직접 `is_admin`을 `true`로 설정해야 합니다 (아직 별도의 관리자 승격 UI는 없습니다).

```sql
update users set is_admin = true where email = 'you@example.com';
```

## Track A / B — 두 개의 독립된 평가 대상

Model Lab은 최소 두 종류의 AI 작업을 완전히 분리해서 관리합니다.

```text
1. BRANCH_GENERATION       사용자가 보내려는 메시지 → 예상 답변 Branch 초안
2. REPLY_CLASSIFICATION    상대의 실제 답장 → 승인된 Branch 중 매칭 판정
```

두 트랙의 Dataset과 평가 방식은 서로 다르며, Generation 결과를 Classification 평가용 정답으로 재사용하지 않습니다.

## Classification Eval 실행 방법

1. **Prompts** 화면에서 System Prompt 버전을 생성합니다 (버전은 불변, 수정 시 새 버전 생성).
2. **Models → Classification**에서 Model Config를 만듭니다 (model, temperature, confidence threshold, 사용할 Prompt Version 지정). 상태는 `DRAFT`로 시작합니다.
3. **Datasets**에서 Eval Dataset과 Scenario, Golden Branch, Reply Case(멀티 메시지 지원)를 준비합니다. 최초 배포에는 seed fixture(clear match / ambiguous / no match / new question / out-of-scope / multi-message)가 포함되어 있습니다.
4. Classification workspace에서 `SMOKE` / `CORE` / `HOLDOUT` split을 선택하면 실행 전 예상 case 수가 먼저 표시됩니다 (비용 통제).
5. Run을 실행하면 서버가 각 Reply Case를 Model Config에 설정된 Provider(OpenAI 또는 로컬 Ollama/Qwen3)로 structured output 분류하고, 반환된 branch id를 golden branch 목록과 대조 검증한 뒤 아래 지표를 계산합니다. 로컬 모델은 프로덕션과 동일한 `LocalLlmClient`(Ollama)를 재사용하며, 작은 모델을 위한 압축 스키마(단일 `state` enum)도 지원합니다 — 실제 튜닝 시도와 결과는 [`backend/docs/QWEN_TUNING.md`](backend/docs/QWEN_TUNING.md) 참고.

```text
Branch Match Accuracy
Ambiguous / New Question / Out-of-Scope / No-Match Detection Recall
Schema Validity
False Auto-Send Rate      ← 가장 중요한 지표
Auto-Send Coverage
Average Latency / Tokens / Estimated Cost
```

**False Auto-Send**는 `confidence >= threshold` 단독이 아니라 아래 조건의 전체 AND 조합으로 판정합니다 (`AutoSendGuardrail`).

```text
confidence >= threshold
AND NOT ambiguous
AND NOT contains_new_question
AND NOT contains_out_of_scope_content
AND NOT prompt_injection_suspected
AND selected branch is valid/approved
AND execution_mode = AUTO
```

실패 케이스는 Failed Cases 목록에서 Question / Golden Branches / Reply / Expected / Actual / Reasoning / Latency / Token 사용량을 함께 확인할 수 있습니다. 실패 분석 → Prompt 새 버전 생성 → 재실행 → 버전 간 비교(Eval Runs 화면) 동선을 지원합니다.

## Generation Human Review 방법

Generation workspace에서 질문별로 생성된 Branch(Condition/Decision/Response)를 확인하고, Coverage / Separation / Granularity / Pre-decidability / Naturalness / Safety / Overall 점수(1~5)와 메모를 남깁니다. 하드룰(Branch 0개, 과도한 개수, 필드 누락, JSON invalid, 이름 중복)은 자동으로 계산되어 함께 표시되며, Human Review를 자동 점수로 대체하지 않습니다.

## Production 승격 / Rollback

Deployment 화면에서 Task(`BRANCH_GENERATION` / `REPLY_CLASSIFICATION`)별 현재 Production Config와 후보 Config를 나란히 비교한 뒤 `Promote to Production`을 실행합니다. Task당 Production Config는 항상 1개만 유지되며(DB 유니크 제약), 이전 Production은 삭제되지 않고 `Rollback`으로 즉시 되돌릴 수 있습니다. Promote/Rollback 이력은 모두 `model_deployment_history`에 남습니다.

Config는 `DRAFT` 상태를 벗어나면(Promote 등) 이후 수정이 불가능합니다 — Prompt나 Threshold를 바꾸려면 항상 새 버전/새 Config를 만듭니다.

**현재 알려진 한계:** 실제 프로덕션 `BranchGenerationService` / `ClassificationService`는 아직 `ProductionModelRegistryService.getProductionConfig(...)`를 사용하도록 연결되어 있지 않습니다 (기존 Slack/Execution 흐름을 이번 작업 범위에서 재작성하지 않기 위한 의도적 결정). Promote는 Model Lab 내부 상태 전환까지만 보장하며, 실제 트래픽 전환 연결은 후속 작업입니다.

## Fine-tuning

`fine_tuning_jobs` 테이블과 CRUD, 상태 조회 API까지는 동작합니다. 실제 OpenAI Fine-tuning Job 제출은 아직 연결되어 있지 않으며, 호출 시 `501 FINE_TUNING_NOT_IMPLEMENTED`를 반환하고 화면에도 "아직 구현되지 않음"을 명시합니다 — 동작하는 것처럼 위장하지 않습니다.

## Metric 정의

- **False Auto-Send Rate**: 실제로는 자동 발송하면 안 되는 test reply 중, 현재 ModelConfig와 Threshold 조건에서 자동 실행 가능한 상태로 잘못 판정된 비율.
- **Auto-Send Coverage**: 전체 test reply 중 guardrail을 모두 통과해 자동 실행 가능하다고 판정된 비율.

Threshold를 올려 False Auto-Send를 낮추면 Auto-Send Coverage도 함께 낮아질 수 있어 두 지표를 항상 같이 봅니다.

## 환경변수

`backend/.env.example`, `frontend/.env.example` 참고. Model Lab은 기존 `OPENAI_API_KEY` / `OPENAI_MODEL` 설정을 그대로 재사용하며 별도 API 키를 요구하지 않습니다. OpenAI API Key는 프론트엔드 번들, 브라우저 저장소, API 응답, Eval 결과, 평문 로그 어디에도 노출되지 않습니다.

## 보안 주의사항

- `/api/model-lab/**` 전 구간은 `ModelLabAdminInterceptor`로 관리자 여부를 검사합니다.
- Prompt / Dataset은 내부 정보이므로 관리자 계정만 조회·수정할 수 있습니다.
- 일반 사용자 화면(Production BATON UI)에는 Model Lab API가 노출되지 않습니다.

## 로컬 실행

```bash
cd backend
docker compose up -d          # postgres, ollama
./gradlew bootRun

cd frontend/my-app
npm install
npm run dev
```

각 디렉터리의 README([backend](backend/README.md), [frontend](frontend/README.md))에 더 자세한 설정이 있습니다.
