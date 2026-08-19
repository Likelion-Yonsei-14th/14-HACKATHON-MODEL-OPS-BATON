# BATON Frontend

시차가 있는 비동기 협업에서 사용자가 다음 의사결정의 범위를 미리 승인하고, 상대방의 답변이 도착하면 승인된 행동만 실행하도록 돕는 BATON의 웹 클라이언트입니다.

## 핵심 경험

```text
메시지 작성
→ AI가 예상 답변별 workflow 생성
→ 사용자가 Condition / Decision / Action 검토
→ BATON 활성화
→ 답변 도착 후 실행 결과 또는 검토 필요 상태 확인
```

BATON은 범용 AI 채팅이나 빈 캔버스형 자동화 도구가 아닙니다. AI가 workflow 초안을 만들고 사용자가 빠르게 검토·수정·승인하는 경험을 지향합니다.

## 제품 원칙

- 모든 분기는 `Condition → Decision → Action` 구조를 유지합니다.
- 사용자가 직접 개입하면 자동 처리를 중지합니다.
- 애매하거나 승인 범위를 벗어난 답변은 사용자 검토로 전환합니다.
- 자동 발송 사실을 상대방에게 명확히 표시합니다.

## 주요 화면

- 홈: 절약한 대기 시간, 생략한 왕복 횟수, 오프라인 중 완료된 결정
- 대화 선택 및 메시지 작성
- AI workflow 생성 및 분기 편집
- BATON 최종 검토 및 활성화
- 진행 중 BATON과 검토 필요 항목
- 판정 결과 및 실행 timeline
- 플랫폼 연결 및 오류 상태
- 개인 설정과 timezone

## 기술 스택

`my-app/` 하위에서 실제로 사용 중인 스택입니다.

| 구분 | 기술 |
| --- | --- |
| 프레임워크 | Vite · React 19 |
| 언어 | TypeScript |
| 라우팅 | react-router-dom |
| 패키지 매니저 | npm |
| 스타일링 | Tailwind CSS v4 (`@tailwindcss/vite`) |
| 린트 | oxlint |

서버 상태 관리 라이브러리(TanStack Query 등), 폼 라이브러리, 테스트 러너는 아직 도입하지 않았습니다.

## 시작하기

### 사전 요구사항

- Node.js LTS

### 실행

```bash
cd my-app
npm install
cp .env.example .env.local
npm run dev
```

### 검증 명령

```bash
npm run lint
npm run build
npm run preview
```

`typecheck`/`test` 스크립트는 아직 없습니다. `npm run build`가 `tsc -b`를 포함해 타입 체크를 겸합니다.

## 환경변수

로컬 값은 `my-app/.env.local`에 작성하고 저장소에 커밋하지 않습니다. 공개 가능한 키 목록과 예시는 [`my-app/.env.example`](my-app/.env.example)에만 유지합니다.

| 변수 | 설명 |
| --- | --- |
| `VITE_BATON_API_BASE_URL` | BATON 백엔드 API 주소 (origin까지만, `/api` prefix는 코드에서 자동으로 붙음). 비워두면 mock 데이터로 동작 |
| `VITE_BATON_API_KEY` | 데모용 단일 사용자의 API Key. 로그인/회원가입 화면이 스코프 밖이라 백엔드 팀이 `POST /api/users`로 한 번 발급해 전달하는 값을 사용 |

## 폴더 구조

```text
my-app/
├── src/
│   ├── pages/           # 화면 컴포넌트 (docs/screens.md 참고)
│   ├── components/
│   │   ├── ui/          # 공용 UI primitives (Button, Panel, Badge, Dialog, StepTabs)
│   │   └── layout/      # AppShell, Header
│   ├── api/
│   │   ├── client.ts    # BatonApiClient 인터페이스 (API 계약)
│   │   ├── index.ts     # VITE_BATON_API_BASE_URL 유무로 http/mock 클라이언트 선택
│   │   ├── http/        # 실제 백엔드 fetch 클라이언트 (client/config/mappers/request)
│   │   └── mock/        # mock 데이터 클라이언트
│   ├── types/           # 도메인 타입, enum
│   └── lib/
├── docs/
│   ├── api-integration.md  # 실제 백엔드 연동 방법, 확정된 API 흐름, 남은 응답 스키마 간극
│   ├── enum-proposals.md   # 폐기됨, 히스토리 참고용
│   └── screens.md          # 화면 목록, 데모 시연 동선
└── CLAUDE.md             # my-app 작업 지침
```

컴포넌트가 직접 네트워크 요청을 수행하지 않도록 `src/api`를 통해 접근합니다. 백엔드 DTO(snake_case)와 화면 모델(camelCase) 변환은 `src/api/http/mappers.ts`에서 처리합니다.

## UI 상태

최소한 다음 상태를 화면에서 구분해야 합니다.

- `DRAFT`
- `ARMED` / `WAITING`
- `PENDING_REVIEW`
- `EXECUTED` / `COMPLETED`
- `EXPIRED` / `CANCELLED`
- `ERROR` / `SYNC_FAILED`

## 디자인 시스템

디자인 토큰은 아직 확정되지 않았습니다. 값이 정해지기 전까지 임의의 브랜드 색상이나 타이포그래피를 문서에 확정하지 않습니다. 기준이 합의되면 [`docs/DESIGN_TOKENS.md`](docs/DESIGN_TOKENS.md)와 실제 테마 파일을 함께 갱신합니다.

## 관련 문서

- [`AGENTS.md`](AGENTS.md): 코드 작업 시 지켜야 할 규칙
- [`docs/DESIGN_TOKENS.md`](docs/DESIGN_TOKENS.md): 디자인 토큰 작성 양식
- [`CONTRIBUTING.md`](CONTRIBUTING.md): 브랜치·커밋·PR 규칙

## Model Lab

`src/pages/modellab/`에 BATON Model Lab(AI Eval/Ops 콘솔) 화면이 `/batons/models` 경로 아래 추가되어 있습니다. `ModelLabLayout`이 Overview / Models(Classification, Generation) / Prompts / Datasets / Eval Runs / Fine-tuning / Deployment 사이드바를 제공합니다. Linear·Vercel·Supabase Dashboard·GitHub Actions 계열의 내부 개발툴 톤을 따르며, 화려한 AI SaaS 랜딩페이지 스타일(그라디언트·glow·glassmorphism·큰 hero)은 의도적으로 배제했습니다.

API 연동은 `src/api/modelLab/client.ts`, 타입은 `src/types/modelLab.ts`에 있으며 기존 `api/http` 클라이언트 설정을 그대로 재사용합니다. Model Lab 화면은 관리자(`is_admin`) 계정으로 로그인했을 때만 정상 동작합니다.

루트 [`README.md`](../README.md)에 Model Lab 사용법 전체가 있습니다.

## 관련 저장소

- Backend: `Likelion-Yonsei-14th/14-HACKATHON-BACKEND-BATON`
- Model Lab (이 저장소가 속한 fullstack): `Likelion-Yonsei-14th/14-HACKATHON-MODEL-OPS-BATON`
