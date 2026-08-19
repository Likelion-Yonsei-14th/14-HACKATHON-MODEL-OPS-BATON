import type { SenderType } from './enums'

export interface Message {
  id: string
  conversationId: string
  senderType: SenderType
  content: string
  /** 다국어 스코프: 원문 언어. null이면 번역/원문 구분 UI를 생략한다. */
  originalLanguage: string | null
  /**
   * 사용자 언어로 번역된 내용. 실제 API 응답에도 이 필드가 없다(docs/api-integration.md 참고)
   * — 항상 null. null이면 "번역 (한국어)" 섹션을 아예 표시하지 않는다 — 임의로 지어내지 않는다.
   */
  translatedContent: string | null
  /** true면 Baton이 자동 발송한 메시지 — 발신자 UI에 "자동 발송" 표시용. */
  isBatonGenerated: boolean
  sentAt: string
}
