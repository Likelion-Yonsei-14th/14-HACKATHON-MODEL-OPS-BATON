import type { ExecutionMode } from './enums'

/**
 * branches 중 프론트에서 쓰는 부분.
 *
 * action_type / action_config_json은 스키마엔 남아있지만 UI엔 노출하지 않기로
 * 확정했으므로 이 타입에서 제외했다 (분기 카드는 "유형 라벨 + 응답 초안 + 편집"만).
 * condition_rule_json도 AI 매칭용 내부 표현이라 제외.
 *
 * name/description/conditionText 세 필드 중 어떤 걸 카드의 "유형 라벨"로 쓸지는
 * 미확정 — 일단 name을 라벨, description을 부가 설명으로 가정했다. 확인 필요.
 */
export interface Branch {
  id: string
  batonId: string
  name: string
  description: string | null
  /** 사용자가 편집 가능한 응답 초안. */
  responseText: string
  executionMode: ExecutionMode
  sortOrder: number
}
