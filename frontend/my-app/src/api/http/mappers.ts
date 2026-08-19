import type {
  Baton,
  Branch,
  Classification,
  Conversation,
  Execution,
  LlmProvider,
  Message,
  PlatformConnection,
  User,
} from '../../types'

// 백엔드 응답은 snake_case + Long(number) id. 아래는 각 엔드포인트 예시에 나온 필드만
// 담은 원시 타입이다 — 목록 응답은 상세 응답보다 필드가 적으므로 대부분 Partial로 받는다.

export interface RawUser {
  id: number
  name: string
  timezone: string | null
  language: string | null
  llm_provider?: LlmProvider
}

export function mapUser(raw: RawUser): User {
  return {
    id: String(raw.id),
    name: raw.name,
    language: raw.language,
    timezone: raw.timezone,
    llmProvider: raw.llm_provider ?? 'LOCAL',
  }
}

export interface RawPlatformConnection {
  id: number
  platform_type: string
  workspace_id: string
  workspace_name: string | null
  connection_status: string
  last_synced_at: string | null
}

export function mapPlatformConnection(raw: RawPlatformConnection): PlatformConnection {
  return {
    id: String(raw.id),
    platformType: raw.platform_type as PlatformConnection['platformType'],
    workspaceId: raw.workspace_id,
    workspaceName: raw.workspace_name,
    connectionStatus: raw.connection_status as PlatformConnection['connectionStatus'],
    lastSyncedAt: raw.last_synced_at,
  }
}

export interface RawConversation {
  id: number
  external_conversation_id: string
  external_thread_id?: string | null
  conversation_type: string
  title?: string | null
  counterpart_name: string | null
  counterpart_timezone: string | null
}

export function mapConversation(raw: RawConversation): Conversation {
  return {
    id: String(raw.id),
    externalConversationId: raw.external_conversation_id,
    externalThreadId: raw.external_thread_id ?? null,
    conversationType: raw.conversation_type as Conversation['conversationType'],
    title: raw.title ?? null,
    counterpartName: raw.counterpart_name,
    counterpartTimezone: raw.counterpart_timezone,
  }
}

export interface RawMessage {
  id: number
  sender_type: string
  content: string
  original_language?: string | null
  is_baton_generated: boolean
  sent_at: string
}

export function mapMessage(raw: RawMessage): Message {
  return {
    id: String(raw.id),
    conversationId: '', // 목록 조회 응답엔 conversation_id가 없어 호출부에서 채운다
    senderType: raw.sender_type as Message['senderType'],
    content: raw.content,
    originalLanguage: raw.original_language ?? null,
    // 번역문 저장 컬럼이 스키마에 없다 — docs/api-integration.md 참고, 항상 null.
    translatedContent: null,
    isBatonGenerated: raw.is_baton_generated,
    sentAt: raw.sent_at,
  }
}

export interface RawBaton {
  id: number
  conversation_id: number
  trigger_message_id?: number
  reply_message_id?: number | null
  status: string
  auto_send_enabled: boolean
  expires_at: string | null
  activated_at?: string | null
  completed_at?: string | null
}

export function mapBaton(raw: RawBaton): Baton {
  return {
    id: String(raw.id),
    conversationId: String(raw.conversation_id),
    triggerMessageId: raw.trigger_message_id != null ? String(raw.trigger_message_id) : '',
    replyMessageId: raw.reply_message_id != null ? String(raw.reply_message_id) : null,
    status: raw.status as Baton['status'],
    autoSendEnabled: raw.auto_send_enabled,
    expiresAt: raw.expires_at,
    activatedAt: raw.activated_at ?? null,
    completedAt: raw.completed_at ?? null,
  }
}

export interface RawBranch {
  id: number
  baton_id?: number
  name: string
  description?: string | null
  condition_text?: string | null
  decision_text?: string | null
  response_text?: string | null
  execution_mode?: string
  sort_order?: number
}

export function mapBranch(raw: RawBranch, fallback: { batonId: string; sortOrder: number }): Branch {
  return {
    id: String(raw.id),
    batonId: raw.baton_id != null ? String(raw.baton_id) : fallback.batonId,
    name: raw.name,
    description: raw.description ?? raw.condition_text ?? null,
    responseText: raw.response_text ?? '',
    executionMode: (raw.execution_mode as Branch['executionMode']) ?? 'AUTO',
    sortOrder: raw.sort_order ?? fallback.sortOrder,
  }
}

export interface RawClassification {
  id: number
  baton_id?: number
  selected_branch_id: number | null
  confidence: number | null
  is_ambiguous: boolean
  contains_new_question: boolean
  reasoning_summary: string | null
  result_status: string
  created_at?: string
}

export function mapClassification(raw: RawClassification, fallback: { batonId: string }): Classification {
  return {
    id: String(raw.id),
    batonId: raw.baton_id != null ? String(raw.baton_id) : fallback.batonId,
    selectedBranchId: raw.selected_branch_id != null ? String(raw.selected_branch_id) : null,
    // candidateBranchIds는 스키마/응답 어디에도 없다 — docs/api-integration.md 참고.
    candidateBranchIds: null,
    confidence: raw.confidence,
    isAmbiguous: raw.is_ambiguous,
    containsNewQuestion: raw.contains_new_question,
    reasoningSummary: raw.reasoning_summary,
    resultStatus: raw.result_status as Classification['resultStatus'],
  }
}

export interface RawExecution {
  id: number
  baton_id?: number
  classification_id: number | null
  result_message_id: number | null
  execution_status: string
  executed_at: string | null
  failure_reason: string | null
  created_at?: string
}

export function mapExecution(raw: RawExecution, fallback: { batonId: string }): Execution {
  return {
    id: String(raw.id),
    batonId: raw.baton_id != null ? String(raw.baton_id) : fallback.batonId,
    classificationId: raw.classification_id != null ? String(raw.classification_id) : null,
    executionStatus: raw.execution_status as Execution['executionStatus'],
    resultMessageId: raw.result_message_id != null ? String(raw.result_message_id) : null,
    executedAt: raw.executed_at,
    failureReason: raw.failure_reason,
  }
}
