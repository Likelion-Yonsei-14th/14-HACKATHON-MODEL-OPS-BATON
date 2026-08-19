# enum 값 제안 — 백엔드 합의 필요 (⚠ 폐기됨, 히스토리 참고용)

**이 문서는 더 이상 유효하지 않다.** "BATON API 명세서"(저장소 루트의 `BATON API 명세서/`
디렉터리)로 실제 enum 값과 API 계약이 확정됐다 — `docs/api-integration.md`를 보고,
`src/types/enums.ts`도 실제 값으로 이미 교체됐다. 아래 내용은 그 이전에 프론트가
추측했던 값들의 기록으로만 남겨둔다 (왜 그렇게 추측했는지 배경 참고용).

---

`baton-server.sql`의 상태 컬럼은 전부 `VARCHAR`라 실제 값이 정해져 있지 않다. 아래는 프론트가 임시로 쓰기 위해 제안한 값이며, 백엔드와 합의 전까지는 확정이 아니다. 코드상 위치: `src/types/enums.ts`.

## batons.status — 화면 전환의 축

제안: `draft | active | held | sent | expired`

```
draft --(트리거 메시지 발송 + 분기 준비 완료)--> active
active --(만료 시각 경과, 답장 없음)--> expired
active --(답장 도착, 분기 매칭 성공)--> sent
active --(답장 도착, is_ambiguous 또는 contains_new_question)--> held
held --(사용자가 분기 선택 또는 직접 작성 후 발송)--> sent
```

- `sent` 상태에서 "결과 미확인" 배지는 별도 status 값이 아니라 `completed_at IS NULL` 로 파생시키는 걸 제안함 (스키마에 컬럼 추가 없이 기존 `completed_at`으로 표현 가능).
- **질문**: 답장 도착 후 AI 판정 처리 중인 짧은 로딩 구간을 별도 status(`classifying`)로 영속화할지, 아니면 프론트에서만 로컬 로딩 state로 처리할지 — 스코프상 동기 처리로 보여 후자로 가정함.
- **질문**: 사용자가 활성화된 바통을 중간에 취소하는 액션이 스코프에 명시되지 않았음. 필요 없다고 보고 `cancelled` 상태는 제안하지 않았는데 맞는지 확인 필요.

## branches.execution_mode ↔ batons.auto_send_enabled — 의견

프롬프트에서 요청한 의견: **`execution_mode`를 분기별 토글로 살리기보다, `batons.auto_send_enabled` 하나로 합치는 쪽을 추천한다.**

이유:
- `action_type`이 `send_message` 하나로 고정된 것처럼, 스코프가 "1왕복 + LLM 호출 2회"로 좁아서 분기마다 다른 발송 정책을 가질 실익이 크지 않음 (사용자가 분기 A는 자동, 분기 B는 확인 후 발송으로 나눠 쓸 시나리오가 데모 동선에 없음).
- 두 값이 다르면 "바통은 자동발송 켜짐인데 분기는 확인 후 발송" 같은 충돌 상태를 판정 로직이 처리해야 해서 복잡도만 늘어남.
- 컬럼 자체는 스키마에 남겨두되(`action_type`과 동일하게), UI/판정 로직에서는 `batons.auto_send_enabled` 하나만 참조하는 걸 제안함.

이 제안대로 `src/types/branch.ts`에는 `executionMode`를 일단 남겨뒀다 — 스키마 컬럼이 있으니 타입에서 빼는 것보다 남겨두고 실제 판정 로직에서 안 쓰는 편이 안전하다고 판단. 백엔드와 합의되면 정리.

## 그 외 후보 목록

| 컬럼 | 제안 값 | 비고 |
|---|---|---|
| `platform_connections.connection_status` | `connected \| expired \| revoked \| error` | ConnectError/SyncError 화면과 매핑됨 |
| `conversations.conversation_type` | `channel \| dm` | Slack엔 group DM(mpim)도 있음 — 스코프에서 뺄지 확인 필요 |
| `messages.sender_type` | `user \| counterpart` | 채널에 제3자(다른 봇 등)가 섞이는 경우는 스코프에서 제외한다고 가정 |
| `classifications.result_status` | `matched \| ambiguous \| new_question \| failed` | `is_ambiguous`/`contains_new_question` 두 불리언과 의미가 겹침 — **질문**: `result_status`가 이 둘로부터 파생되는 값인지, 아니면 LLM 호출 실패(`failed`) 같은 추가 상태를 위해 별도로 필요한 값인지 확인 필요 |
| `executions.execution_status` | `pending \| success \| failed` | |

## branches의 텍스트 필드 4개 — 확인 필요

`name`, `description`, `condition_text`, `decision_text` 네 개 텍스트 컬럼 중 분기 카드에 노출할 "유형 라벨"이 무엇인지 불명확함. 일단 `name`을 라벨, `description`을 부가 설명으로 가정해서 타입을 만들었고(`src/types/branch.ts`), `condition_text`/`decision_text`는 AI 매칭용 내부 표현으로 보고 프론트 타입에서 제외했다. 디자인 시안 나오면 확인 필요.

## 프론트에서 필요한 스키마 관련 논의 (수정 요청 아님, 별도 목록)

- `conversations`에 "최근 활동 시각" 같은 필드가 없음. 대화 목록 화면에서 정렬/표시용으로 필요한데, 가장 최근 `messages.sent_at`을 조인해서 API가 내려주는 방식이면 스키마 변경 없이 해결 가능 — 백엔드에 API 응답 형태로 요청할 예정.
- `executions.action_type`과 `branches.action_type`이 값이 항상 같음(`send_message` 고정) — 스키마 수정 제안은 아니고 참고 사항.
- **`messages`에 번역문 저장할 컬럼이 없음.** 다국어 스코프상 원문과 다른 언어면 번역을 보여줘야 하는데(PendingResponse의 "번역 (한국어)" 섹션), `original_language`만 있고 번역된 텍스트를 저장할 곳이 없음. API가 매번 호출 시점에 번역해서 내려주는지, DB에 캐싱하는지 백엔드 논의 필요. 프론트 타입(`src/types/message.ts`)엔 `translatedContent`를 옵셔널로 추가해둠 — null이면 번역 섹션 자체를 표시하지 않는다.
- **`classifications`에 "후보 분기 2개" 저장할 곳이 없음.** `is_ambiguous`일 때 화면엔 후보 분기 2개를 나란히 보여줘야 하는데(확정된 설계), `selected_branch_id`는 단수 컬럼이라 어떤 2개가 후보였는지 저장이 안 됨. API 응답에 `candidateBranchIds` 같은 필드를 추가하거나(DB 컬럼 추가 없이 서버 로직에서 계산해 내려주는 것도 가능), 컬럼을 추가해야 함 — 백엔드 논의 필요. 프론트 타입(`src/types/classification.ts`)엔 일단 옵셔널로 추가해둠.
