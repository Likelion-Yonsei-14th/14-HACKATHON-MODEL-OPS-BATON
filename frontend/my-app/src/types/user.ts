export type LlmProvider = 'LOCAL' | 'OPENAI'

export interface User {
  id: string
  name: string
  /** UI 문구용 (예: "e2e", "다국어"). 로그인 기능은 스코프 밖 — 데모 환경엔 고정 단일 사용자를 가정. */
  language: string | null
  timezone: string | null
  /** 답장 분류/분기 생성에 쓸 모델. 기본 LOCAL(서버의 Qwen3 0.6B), OPENAI는 개인설정에서 선택. */
  llmProvider: LlmProvider
}
