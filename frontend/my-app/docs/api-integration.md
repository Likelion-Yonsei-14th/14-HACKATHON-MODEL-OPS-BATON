# 실제 백엔드 연동

`docs/enum-proposals.md`에 있던 값들은 전부 프론트의 추측이었다. 이후 팀이 만든
"BATON API 명세서"(Notion export, 저장소 루트의 `BATON API 명세서/` 디렉터리)를 확인해
enum·요청/응답 스키마가 실제로 확정됐고, `src/api/http/`에 그 명세를 그대로 구현한
fetch 기반 클라이언트를 추가했다. 이 문서는 그 연동 내용과 아직 남은 간극을 정리한다.

## 켜는 법

`src/api/index.ts`가 `VITE_BATON_API_BASE_URL` 존재 여부로 실제 백엔드(`src/api/http/client.ts`)와
목데이터(`src/api/mock/client.ts`) 중 하나를 고른다. `.env.example`을 `.env.local`로 복사해서
`VITE_BATON_API_BASE_URL`, `VITE_BATON_API_KEY`를 채우면 된다.

- **API Key**: `/signup`, `/login` 화면에서 실제로 회원가입/로그인한다. `POST /api/users`(회원가입)와
  `POST /api/login` 모두 응답으로 `api_key`를 내려주고, 프론트는 이 값을 `localStorage`
  (`API_KEY_STORAGE_KEY`)에 저장해 이후 모든 요청에 `Authorization: Bearer <api_key>`로 자동 첨부한다.
  로컬에서 백엔드 없이 데모 데이터로만 확인하려면 `.env.local`에서 `VITE_BATON_API_BASE_URL`을 비워두면
  목데이터(`src/api/mock/client.ts`)로 동작한다.

## 공통 규약 (BATON API 명세서 "구현 차이 및 PR 이력" 기준)

- 모든 경로에 `/api` prefix.
- `Authorization: Bearer <api_key>`.
- 성공 `{success: true, data, error: null}` / 실패 `{success: false, data: null, error: {code, message}}`.
  → `src/api/http/request.ts`의 `request()`가 이 wrapper를 벗기고, 실패 시 `BatonApiError`를 던진다.
- id는 전부 `Long`(JSON number). 프론트 타입은 기존 관례대로 문자열 id를 쓰므로
  `src/api/http/mappers.ts`가 매 경계에서 `String()`/`Number()`로 변환한다.

## enum 값 — 이제 확정이다

`src/types/enums.ts`가 실제 값(대문자, 예: `WAITING`, `PENDING_REVIEW`)으로 교체됐다.
`docs/enum-proposals.md`에 있던 소문자 추측값(`active`, `held`, `sent`...)은 전부 폐기했다.
가장 크게 달라진 지점은 `BatonStatus`다 — 기존엔 5개(`draft/active/held/sent/expired`)를
추측했지만 실제로는 9개(`DRAFT/ARMED/WAITING/PENDING_REVIEW/EXECUTED/COMPLETED/EXPIRED/CANCELLED/ERROR`)이고,
화면 라우팅은 `Home.tsx`의 `statusMeta()`가 이 값들을 3개 화면(대기/검토 필요/결과)으로 묶는다.

## 실제 바통 생성 흐름 (BATON API 명세서 "핵심 API 흐름" 기준)

CSV만 봤을 땐 몰랐던 구조적 차이: `POST /batons`(BATON 생성)가 `POST /batons/{id}/branches/generate`(AI
분기 생성)보다 **먼저** 와야 한다 — `branches/generate`가 이미 존재하는 `baton_id`를 요구하기 때문이다.
그래서 3단계 위저드의 API 호출을 다음과 같이 재구성했다 (`BatonApiClient.startBaton`/`updateBranch`/`activateBaton`,
`src/api/http/client.ts`):

```
1단계(ComposeBaton) "이 대화로 바통 만들기" 클릭
  → POST /conversations/{id}/messages   (트리거 메시지 발송)
  → POST /batons                        (DRAFT 생성, auto_send_enabled는 임시 false)
  → POST /batons/{id}/branches/generate (분기 3개 생성 — 이 시점에 이미 서버에 저장됨)

2단계(BranchPrep) 분기 편집
  → PATCH /batons/{id}/branches/{branchId}  (textarea blur마다)

3단계(SendConfirm) "바통 시작하기" 클릭
  → PATCH /batons/{id}    (자동발송/최대 대기 시간 확정)
  → POST /batons/{id}/activate  (DRAFT → WAITING)
```

## 아직 남은 간극 (백엔드 응답 자체의 한계)

- **`candidateBranchIds`가 없다.** `is_ambiguous`일 때 "후보 분기 2개"를 보여주려던 원래 설계는
  실제 API 응답에도 해당 필드가 없어 그대로 막혀 있다. `PendingResponse.tsx`는 후보가 없으면
  해당 바통의 branches 전체를 후보로 보여주는 것으로 폴백한다.
- **결과 "확인" API가 없다.** `ResultConfirm.tsx`의 "확인" 버튼이 예전엔 `confirmResult()`로
  `completed_at`을 채우는 걸 흉내 냈는데, 실제 API 목록엔 그런 엔드포인트가 없다. 지금은 그냥
  홈으로 이동만 한다.
- **`executions` 응답에 `branch_id`가 없다.** SQL 스키마엔 있지만 API 응답에선 빠져 있어서,
  "어떤 분기가 선택됐는지"는 `execution.branchId`가 아니라 `classification.selectedBranchId`로 확인한다.
- **`classifications` 응답에 `reply_message_id`가 없다.** 답장 메시지는 `baton.replyMessageId`로 조회한다.
- **번역문 저장 컬럼이 없다.** (기존과 동일 — `messages.translatedContent`는 항상 null.)
- **목록 조회(`GET /batons`, `GET /conversations`)는 cursor 페이지네이션을 지원**하지만 지금은
  첫 페이지만 가져온다. 데모 데이터 규모에선 문제없지만 실사용 시엔 `next_cursor`를 따라가는
  로직이 필요하다.
- **Slack OAuth 콜백은 백엔드가 직접 받는다.** `GET /api/platform-connections/slack/callback`은
  Slack이 브라우저를 리다이렉트하는 대상이 백엔드 자신이라고 가정했다(코드 교환 + 대화 동기화까지
  서버가 처리). 그래서 프론트의 `/connect/callback`(`ConnectCallback.tsx`)은 `code`를 직접 다루지
  않고, 백엔드가 처리를 마친 뒤 이 경로로 다시 리다이렉트해준다고 가정한 뒤 `getPlatformConnection()`으로
  연결 상태만 확인한다. 백엔드가 실제로 어디로 리다이렉트하는지는 확인이 필요하다.
