import type { ExecutionStatus } from './enums'

/**
 * executions 중 프론트에서 쓰는 부분.
 * action_type은 branches와 마찬가지로 스코프상 send_message 고정이라 제외.
 * branch_id는 SQL 스키마엔 있지만 실제 API 응답(Execution 단일/목록 조회)엔 내려오지
 * 않는다 — "어떤 분기가 발송됐는지"는 classification.selectedBranchId로 대신 확인한다.
 */
export interface Execution {
  id: string
  batonId: string
  classificationId: string | null
  executionStatus: ExecutionStatus
  resultMessageId: string | null
  executedAt: string | null
  failureReason: string | null
}
