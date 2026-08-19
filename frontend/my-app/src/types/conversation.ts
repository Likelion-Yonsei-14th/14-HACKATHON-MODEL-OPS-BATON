import type { ConversationType } from './enums'

/**
 * conversations 중 프론트에서 쓰는 부분.
 * external_conversation_id / external_thread_id 는 Slack 딥링크 등에 나중에
 * 필요할 수 있어 남겨뒀지만 지금 화면 어디서도 직접 쓰지 않는다.
 */
export interface Conversation {
  id: string
  externalConversationId: string
  externalThreadId: string | null
  conversationType: ConversationType
  title: string | null
  counterpartName: string | null
  counterpartTimezone: string | null
}
