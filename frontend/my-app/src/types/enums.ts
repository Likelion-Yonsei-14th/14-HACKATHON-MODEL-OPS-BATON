// 스키마의 VARCHAR 상태 컬럼들을 프론트가 쓸 수 있는 union type으로 정리한 것.
// 값은 실제 백엔드 API 명세("BATON API 명세서/Enum 정의")를 그대로 반영한다 — 더 이상 프론트의 추측이 아니다.
// 배경/미해결 항목은 docs/api-integration.md 참고.

/** platform_connections.platform_type */
export type PlatformType = 'SLACK' | 'TEAMS' | 'DISCORD'

/** platform_connections.connection_status */
export type ConnectionStatus = 'CONNECTED' | 'EXPIRED' | 'ERROR' | 'DISCONNECTED'

/** conversations.conversation_type */
export type ConversationType = 'DM' | 'CHANNEL' | 'THREAD'

/** messages.sender_type — Baton 사용자 본인 / 상대방 / Baton이 자동 발송한 메시지. */
export type SenderType = 'USER' | 'COUNTERPART' | 'BATON'

/**
 * batons.status — 화면 전환의 축.
 *
 * 확인된 상태 전이(백엔드 "핵심 API 흐름" 문서 기준):
 *   DRAFT --(POST /batons/{id}/activate)--> WAITING
 *   WAITING --(답장 도착, 분기 매칭 성공)--> COMPLETED (또는 EXECUTED)
 *   WAITING --(답장 도착, AMBIGUOUS/NO_MATCH/GUARDRAIL_REJECTED)--> PENDING_REVIEW
 *   PENDING_REVIEW --(POST /batons/{id}/resolve)--> COMPLETED
 *   WAITING --(만료 시각 경과)--> EXPIRED
 *
 * ARMED는 Enum 정의엔 있지만 활성화 응답 예시가 WAITING을 반환해 실제 사용 지점이
 * 명세에 명시돼 있지 않다 — UI 분기에서는 WAITING과 동일하게 취급한다.
 * ERROR/CANCELLED는 각각 서버 오류/사용자 취소(POST /batons/{id}/cancel) 결과.
 */
export type BatonStatus =
  | 'DRAFT'
  | 'ARMED'
  | 'WAITING'
  | 'PENDING_REVIEW'
  | 'EXECUTED'
  | 'COMPLETED'
  | 'EXPIRED'
  | 'CANCELLED'
  | 'ERROR'

/** branches.execution_mode */
export type ExecutionMode = 'AUTO' | 'MANUAL'

/** classifications.result_status */
export type ClassificationResultStatus =
  | 'MATCHED'
  | 'LOW_CONFIDENCE'
  | 'NO_MATCH'
  | 'AMBIGUOUS'
  | 'GUARDRAIL_REJECTED'

/**
 * branches.action_type / executions.action_type — 스코프상 UI엔 노출하지 않지만
 * (docs 참고) 백엔드 값 자체는 4종류다.
 */
export type ActionType = 'SEND_REPLY' | 'REQUEST_HUMAN' | 'FORWARD' | 'NOTIFY'

/** executions.execution_status */
export type ExecutionStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'
