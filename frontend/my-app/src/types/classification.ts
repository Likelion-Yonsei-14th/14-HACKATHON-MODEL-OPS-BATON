import type { ClassificationResultStatus } from './enums'

/**
 * classifications 중 프론트에서 쓰는 부분.
 * model_name은 운영/디버그용이라 제외. extracted_data_json은 용도가 불명확해 제외 —
 * 필요해지면 스키마 논의와 함께 다시 추가.
 *
 * isAmbiguous / containsNewQuestion 두 값이 PendingResponse 화면의 두 상태를 가른다:
 *   - isAmbiguous만 true       → 후보 분기 중 선택
 *   - containsNewQuestion true (또는 위 둘 다 false — NO_MATCH/GUARDRAIL_REJECTED 등)
 *     → 분기 선택 대신 상대의 새 질문 강조 + 직접 작성 유도
 *
 * replyMessageId는 실제 API 응답에 없다 — 답장 메시지는 baton.replyMessageId로 조회한다.
 *
 * candidateBranchIds는 실제 API 응답에도 없는 필드다(백엔드 API 명세서 확인 완료 —
 * "후보 분기 2개"를 저장/응답할 컬럼 자체가 없음). is_ambiguous일 때 어떤 분기가
 * 후보였는지 구분할 방법이 없어서, 화면에서는 해당 바통의 branches 전체를 후보로
 * 보여주는 걸로 대체한다 (docs/api-integration.md 참고). 항상 null.
 */
export interface Classification {
  id: string
  batonId: string
  selectedBranchId: string | null
  candidateBranchIds: string[] | null
  confidence: number | null
  isAmbiguous: boolean
  containsNewQuestion: boolean
  reasoningSummary: string | null
  resultStatus: ClassificationResultStatus
}
