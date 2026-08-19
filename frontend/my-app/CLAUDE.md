# Baton 프론트엔드

## 이게 뭔가

**Baton**은 답장을 기다리느라 멈추는 시간을 없애는 비동기 협업 도구. 핵심 문장: "잠들기 전에 바통을 넘겨두세요."

메시지를 보낼 때 상대 답변이 갈릴 경우별로 내 응답을 미리 준비해두고("바통"), 답장이 오면 AI가 어느 분기인지 판정해 자동 발송한다. 애매하면 보류하고 알림만 준다.

**핵심 기능 2개**
1. **분기 준비** — AI가 상대 답변 유형 3개 + 각 응답 초안을 제안, 사용자는 선택/편집만.
2. **자동 판정·발송** — 답장 분류 후 해당 응답 발송, 보류 조건에 걸리면 발송 안 하고 알림.

## 스코프

**한다**: Slack 실연동(OAuth) / 분기 준비 / 자동 판정·발송 / 다국어 / 텍스트 입출력 / 1왕복 / LLM 호출 2회

**절대 안 한다 (제안조차 하지 말 것)**: 결정 기록 / Slack 외 플랫폼 / 다음 왕복 이어가기 / 로그인·회원가입 / 첨부파일·이미지·음성 / 실시간 동기화·웹소켓

새 기능을 제안하기 전에 이 목록부터 확인할 것. 좋은 아이디어여도 스코프 밖이면 제안하지 않는다.

## 확정된 설계 결정

1. **`branches.action_type`은 `send_message` 하나로 고정.** 스키마 컬럼은 유지하되 UI에 액션 선택 개념을 노출하지 않는다. 분기 카드는 "유형 라벨 + 응답 초안 + 편집"만 있는 단순 카드.
2. **보류 사유 두 가지, 사용자 행동이 다름.** 한 화면(`/batons/:batonId/pending`)의 두 상태로 구현:
   - `is_ambiguous`(판정 애매) → 후보 분기 2개를 나란히 보여주고 고르게 함.
   - `contains_new_question`(답장에 새 질문 포함) → 확신도와 무관하게 무조건 보류. 분기 선택이 아니라 상대의 새 질문을 강조 표시, 직접 작성 유도.

## 프로젝트 상태 / 히스토리 메모

`my-app`은 처음부터 다시 초기화한 프로젝트다. 이전에 팀원(andyyjlee)이 만든 JS 프로토타입이 있었는데, `package.json`이 없다는 전제로 시작한 세팅 작업과 충돌해서 **완전히 새로 초기화하기로 사용자와 합의**했다. 옛 프로토타입은 git 히스토리 커밋 `b4e3ea5`에 남아있으니 화면 구성 참고용으로만 보고, 코드를 그대로 가져오지 않는다 (JS, 라우팅 없음, "결정 기록" 화면 포함 — 지금 스코프와 안 맞음).

## 스택

- Vite + React + TypeScript, `react-router-dom`으로 라우팅.
- 스타일링은 **Tailwind CSS v4** (`@tailwindcss/vite` 플러그인, 별도 config 파일 없음, `src/index.css`에 `@import 'tailwindcss'` 한 줄).
- 그 외 의존성 추가 안 함 — 해커톤 일정상 의존성 늘리는 거 경계 중. 새 라이브러리 추가할 땐 왜 필요한지 한 줄로 설명할 것.

## 디자인 시스템 (Figma 반영)

Figma "Baton 와이어프레임 - 디자인" (`fileKey: Yt1Vc8rpDDku0IEdXaXQ30`)에서 톤을 추출해 반영했다.

- 토큰은 `src/index.css`의 `@theme`에 있다: `--color-primary: #477AD0`(메인), `--color-primary-soft`, `--color-ink`, `--color-muted`/`--color-muted-2`, `--color-border`/`--color-border-strong`, `--color-landing`. Tailwind 유틸리티로 `bg-primary`, `text-muted-2`처럼 바로 쓴다.
- 폰트는 SUIT(제목/버튼, `font-suit` 클래스)+Inter(본문 기본값)를 CDN(`jsdelivr`, `rsms.me`)에서 `@font-face`로 불러온다. npm 의존성 아님.
- 공용 컴포넌트: `src/components/layout/AppShell.tsx`(헤더+사이드바, 인증 후 화면 전부에 사용), `src/components/ui/Button.tsx`/`Panel.tsx`/`Badge.tsx`. 버튼 클래스는 `src/lib/buttonClasses.ts`에 있어서 `<Link>`처럼 `<button>`이 아닌 요소에도 같은 스타일을 입힐 수 있다.
- **모든 화면을 Figma 프레임에서 `get_design_context`로 직접 가져와 1:1로 구현했다.** 이 프로젝트의 원칙: Figma 디자인을 반영할 땐 대표 화면 몇 개만 가져와서 디자인 시스템을 추출한 뒤 나머지를 손으로 만드는 방식은 쓰지 않는다 — 화면마다 실제 Figma node를 찾아서 그 코드를 레퍼런스로 변환한다. 화면-노드 매핑:
  - `/` Landing → `126:1765` (+ 헤더 `126:1766`)
  - `/connect` SlackConnect → `128:283` (Desktop-1)
  - `/conversations` ConversationPicker → `128:410` (Desktop-2)
  - `/conversations/:id/compose` ComposeBaton → `128:742` (Desktop-3, 3단계 위저드 1단계)
  - `/conversations/:id/branches` BranchPrep → `132:285` (Desktop-4, 2단계)
  - `/conversations/:id/confirm` SendConfirm → `132:608` (Desktop-5, 3단계 — 자동발송 토글 + 최대 대기 시간)
  - `/home` Home → `126:1664` (Frame 20, "바통 홈 페이지"(`119:538`)는 배경만 있는 빈 프레임이고 실제 콘텐츠는 이 프레임이 별도로 겹쳐 있는 것이었음)
  - `/batons/:id` BatonDetail → `132:844` (Desktop-6의 "Main Content")
  - `/batons/:id/pending` PendingResponse → `132:939` (Desktop-7) — 단, **화면 스타일만 가져오고 내용 구조는 확정된 설계(2-state: is_ambiguous/contains_new_question)를 따름.** Figma 목업은 분기 A/B/C + "직접 응답"까지 라디오 4개로 보여주는 더 이전 버전 설계라 그대로 쓰지 않았다.
  - `/batons/:id/result` ResultConfirm → `133:1346` (Desktop-8) — 단, "⚠ 후속 답장 도착" 패널은 다음 왕복 감지 기능이라 스코프 밖("다음 왕복 이어가기" 제외)이라 옮기지 않음.
  - `/connect/error`, `/sync-error` → `133:1628` (Desktop-9, "데이터를 불러오지 못했어요")
  - 응답 발송 확인 모달(`src/components/ui/Dialog.tsx`) → `133:1332` (Dialog)
  - **`/settings`는 Figma에 해당 화면이 없어서 스켈레톤 그대로 남아있음.** 시안이 추가되면 다시 요청할 것.
- Figma의 Desktop-3/4엔 상단에 "이 대화로 바통 만들기" 버튼이 매 단계 반복해서 떠 있는데, 용도가 불명확해서(위저드 진행 버튼과 별개로 뭘 하는 버튼인지 불명) 1단계(ComposeBaton)에만 살려서 실제 진행 버튼으로 썼고 2단계(BranchPrep)에는 옮기지 않았다 — 확인 필요하면 물어볼 것.
- 에셋(`src/assets/logo.png`, `user-circle.svg`, `chevron.svg`, `landing-hero.png`)은 Figma의 임시 URL(7일 만료)에서 다운로드해 커밋한 것. Figma에서 원본을 다시 export하면 같은 파일명으로 덮어쓰면 된다.

## 디렉토리

- `src/types/` — 도메인 타입. `baton-server.sql` 1:1이 아니라 프론트가 쓰는 형태로 정리한 것. 서버 전용 컬럼(토큰, model_name 등)과 UI에 노출 안 하는 컬럼(action_type 등)은 제외했다.
- `src/types/enums.ts` — 스키마의 VARCHAR 상태 컬럼 값. "BATON API 명세서"(저장소 루트의
  `BATON API 명세서/` 디렉터리)로 확정된 실제 값이다 — 더 이상 추측이 아니다. 배경은
  `docs/api-integration.md` 참고.
- `src/api/client.ts` — `BatonApiClient` 인터페이스 (API 계약).
- `src/api/mock/` — 목 데이터 구현체.
- `src/api/http/` — 실제 백엔드에 붙는 fetch 기반 구현체. 공통 응답 wrapper 처리는
  `request.ts`, snake_case→camelCase 매핑은 `mappers.ts`.
- `src/api/index.ts` — `VITE_BATON_API_BASE_URL` 환경변수가 있으면 `http/client.ts`, 없으면
  `mock/client.ts`를 export한다. 나머지 코드는 `BatonApiClient` 인터페이스에만 의존.
- `src/pages/` — 화면 컴포넌트. 목록과 각 화면의 목적/데이터/전이는 `docs/screens.md` 참고.
- `docs/screens.md` — 화면 목록 + 데모 시연 동선.
- `docs/api-integration.md` — 실제 백엔드 연동 방법, 확정된 API 흐름, 아직 남은 응답 스키마
  간극(후보 분기 목록 없음, 결과 확인 API 없음 등).
- `docs/enum-proposals.md` — (폐기됨, 히스토리 참고용) enum 값이 확정되기 전 프론트의 추측 기록.

## 데모 시연 동선

두 헤드라인 시나리오 모두 `/home`에서 1클릭:
- (a) 새 질문 없는 답장 → 자동 발송 성공: "Jamie Lee — 자동 발송 완료" 카드 → `/batons/baton-result-1/result`
- (b) 새 질문 섞인 답장 → 보류 후 알림: "이서연 — 보류 응답 처리 필요" 카드 → `/batons/baton-pending-new-question-1/pending`

라이브 전체 흐름(Slack 연결 → 대화 선택 → 바통 생성 → 분기 준비 → 발송)도 목 데이터로 끝까지 동작한다 (`/conversations` → 대화 선택 → 메시지 작성 → 분기 준비 → 발송 → Home에 새 바통 반영).

## 아직 안 풀린 것들 (실제 API 응답 자체의 한계, `docs/api-integration.md`에 상세)

VARCHAR 상태 값들은 "BATON API 명세서"로 전부 확정됐다 — 아래는 그 이후에도 남아있는,
API 응답 스키마 자체의 간극이다.

- `classifications` 응답에 "후보 분기 2개"(is_ambiguous 케이스) 필드가 없음 — 화면에서는
  해당 바통의 branches 전체를 후보로 보여주는 것으로 폴백 중.
- 결과를 "확인"했다는 사실을 서버에 남길 API가 없음 — `ResultConfirm`의 "확인" 버튼은
  그냥 홈으로 이동만 한다.
- `messages`에 번역문을 담을 컬럼/필드가 없음 — `translatedContent`는 항상 null.

## 지켜야 할 것

- "안 한다" 목록에 있는 기능은 제안하지 않는다.
- `baton-server.sql`을 임의로 수정하지 않는다. 프론트에 필요한 변경은 `docs/enum-proposals.md`의 "프론트에서 필요한 스키마 관련 논의" 섹션에 추가한다.
- 판단이 필요한 지점(enum 값, 화면 구성 등)은 혼자 정하지 말고 사용자에게 물어본다.
