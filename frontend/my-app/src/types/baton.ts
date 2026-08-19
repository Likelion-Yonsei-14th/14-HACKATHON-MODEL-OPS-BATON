import type { BatonStatus } from './enums'

/**
 * batons 중 프론트에서 쓰는 부분.
 * status 값과 전이는 enums.ts의 BatonStatus 주석 참고.
 */
export interface Baton {
  id: string
  conversationId: string
  triggerMessageId: string
  replyMessageId: string | null
  status: BatonStatus
  /** 바통 전체 단위 자동 발송 여부. branches.executionMode와의 관계는 docs/enum-proposals.md 참고. */
  autoSendEnabled: boolean
  expiresAt: string | null
  activatedAt: string | null
  /** null이면 sent 상태여도 "결과 미확인"으로 취급한다. */
  completedAt: string | null
}
