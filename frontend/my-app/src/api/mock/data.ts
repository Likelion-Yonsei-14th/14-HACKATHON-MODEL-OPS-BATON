import type {
  Baton,
  Branch,
  Classification,
  Conversation,
  Execution,
  Message,
  PlatformConnection,
  User,
} from '../../types'

// 데모 시연 시점과 무관하게 "N분 전"이 자연스럽게 보이도록 상대 시각으로 생성한다.
const minutesAgo = (n: number) => new Date(Date.now() - n * 60_000).toISOString()
const hoursAgo = (n: number) => minutesAgo(n * 60)

export const mockUser: User = {
  id: 'user-1',
  name: '김민준',
  language: 'ko',
  timezone: 'Asia/Seoul',
  llmProvider: 'LOCAL',
}

export const mockPlatformConnection: PlatformConnection = {
  id: 'conn-1',
  platformType: 'SLACK',
  workspaceId: 'T0001',
  workspaceName: 'Team Baton',
  connectionStatus: 'CONNECTED',
  lastSyncedAt: minutesAgo(2),
}

// conv-1: 한국어 DM · 답장에 새 질문이 섞여 보류되는 시나리오
// conv-2: 영어 DM · 다국어(원문 영어 → 한국어 표시) + 자동 발송 성공 시나리오
// conv-3: 한국어 DM · 판정 애매로 보류되는 시나리오
// conv-4: 채널 · 아직 답장 없음(대기 중) 시나리오
export const mockConversations: Conversation[] = [
  {
    id: 'conv-1',
    externalConversationId: 'D0001',
    externalThreadId: null,
    conversationType: 'DM',
    title: null,
    counterpartName: '이서연',
    counterpartTimezone: 'Asia/Seoul',
  },
  {
    id: 'conv-2',
    externalConversationId: 'D0002',
    externalThreadId: null,
    conversationType: 'DM',
    title: null,
    counterpartName: 'Jamie Lee',
    counterpartTimezone: 'America/Los_Angeles',
  },
  {
    id: 'conv-3',
    externalConversationId: 'D0003',
    externalThreadId: null,
    conversationType: 'DM',
    title: null,
    counterpartName: 'Pavel Kowalski',
    counterpartTimezone: 'Europe/Warsaw',
  },
  {
    id: 'conv-4',
    externalConversationId: 'C0004',
    externalThreadId: null,
    conversationType: 'CHANNEL',
    title: '#engineering',
    counterpartName: 'James Kim',
    counterpartTimezone: 'America/New_York',
  },
]

export const mockMessages: Message[] = [
  // conv-1 — 이서연, 한국어
  {
    id: 'msg-1-trigger',
    conversationId: 'conv-1',
    senderType: 'USER',
    content: '이번 주 디자인 리뷰 시간, 목요일 오후 3시 어떠세요?',
    originalLanguage: 'ko',
    translatedContent: null,
    isBatonGenerated: false,
    sentAt: hoursAgo(4),
  },
  {
    id: 'msg-1-reply',
    conversationId: 'conv-1',
    senderType: 'COUNTERPART',
    content: '목요일 3시 좋아요. 근데 리뷰 끝나고 바로 배포까지 가는 건가요?',
    originalLanguage: 'ko',
    translatedContent: null,
    isBatonGenerated: false,
    sentAt: hoursAgo(1),
  },
  // conv-2 — Jamie Lee, 영어 (다국어 케이스: 원문 영어 → 한국어 번역)
  {
    id: 'msg-2-trigger',
    conversationId: 'conv-2',
    senderType: 'USER',
    content: 'Can we confirm the Q3 roadmap priority before Friday?',
    originalLanguage: 'en',
    translatedContent: null,
    isBatonGenerated: false,
    sentAt: hoursAgo(6),
  },
  {
    id: 'msg-2-reply',
    conversationId: 'conv-2',
    senderType: 'COUNTERPART',
    content: 'Sounds good, let’s go with that priority.',
    originalLanguage: 'en',
    translatedContent: '좋습니다, 그 우선순위로 진행하시죠.',
    isBatonGenerated: false,
    sentAt: hoursAgo(2),
  },
  {
    id: 'msg-2-sent',
    conversationId: 'conv-2',
    senderType: 'BATON',
    content: '좋습니다. 확인해 주셔서 감사합니다. 말씀해주신 우선순위로 진행하겠습니다.',
    originalLanguage: 'ko',
    translatedContent: null,
    isBatonGenerated: true,
    sentAt: hoursAgo(2),
  },
  // conv-3 — Pavel Kowalski
  {
    id: 'msg-3-trigger',
    conversationId: 'conv-3',
    senderType: 'USER',
    content: '이번 스프린트 범위, 제안한 대로 진행해도 될까요?',
    originalLanguage: 'ko',
    translatedContent: null,
    isBatonGenerated: false,
    sentAt: hoursAgo(5),
  },
  {
    id: 'msg-3-reply',
    conversationId: 'conv-3',
    senderType: 'COUNTERPART',
    content: '음... 일단 방향은 나쁘지 않은 것 같아요.',
    originalLanguage: 'ko',
    translatedContent: null,
    isBatonGenerated: false,
    sentAt: minutesAgo(40),
  },
  // conv-4 — James Kim, 답장 없음
  {
    id: 'msg-4-trigger',
    conversationId: 'conv-4',
    senderType: 'USER',
    content: '백엔드 스코프 오늘 중 확정 가능할까요?',
    originalLanguage: 'ko',
    translatedContent: null,
    isBatonGenerated: false,
    sentAt: hoursAgo(3),
  },
]

const branchSet = (batonId: string): Branch[] => [
  {
    id: `${batonId}-branch-a`,
    batonId,
    name: '분기 A — 긍정적 수락',
    description: '상대방이 제안을 수락하거나 동의하는 경우',
    responseText: '좋습니다. 확인해 주셔서 감사합니다. 말씀해주신 방향으로 진행하겠습니다.',
    executionMode: 'AUTO',
    sortOrder: 0,
  },
  {
    id: `${batonId}-branch-b`,
    batonId,
    name: '분기 B — 조건부 또는 추가 질문',
    description: '상대방이 조건을 달거나 추가 정보를 요청하는 경우',
    responseText: '확인했습니다. 필요한 조건을 알려주시면 그 기준에 맞춰 다시 정리해서 공유드리겠습니다.',
    executionMode: 'AUTO',
    sortOrder: 1,
  },
  {
    id: `${batonId}-branch-c`,
    batonId,
    name: '분기 C — 부정적 거절 또는 보류',
    description: '상대방이 거절하거나 판단을 미루는 경우',
    responseText: '알겠습니다. 지금 진행이 어렵다면 대안 일정이나 담당자를 알려주시면 그 방향으로 맞추겠습니다.',
    executionMode: 'AUTO',
    sortOrder: 2,
  },
]

export const mockBranches: Record<string, Branch[]> = {
  'baton-result-1': branchSet('baton-result-1'),
  'baton-pending-new-question-1': branchSet('baton-pending-new-question-1'),
  'baton-pending-ambiguous-1': branchSet('baton-pending-ambiguous-1'),
  'baton-waiting-1': branchSet('baton-waiting-1'),
}

export const mockBatons: Baton[] = [
  {
    id: 'baton-result-1',
    conversationId: 'conv-2',
    triggerMessageId: 'msg-2-trigger',
    replyMessageId: 'msg-2-reply',
    status: 'COMPLETED',
    autoSendEnabled: true,
    expiresAt: null,
    activatedAt: hoursAgo(6),
    completedAt: hoursAgo(2),
  },
  {
    id: 'baton-pending-new-question-1',
    conversationId: 'conv-1',
    triggerMessageId: 'msg-1-trigger',
    replyMessageId: 'msg-1-reply',
    status: 'PENDING_REVIEW',
    autoSendEnabled: true,
    expiresAt: null,
    activatedAt: hoursAgo(4),
    completedAt: null,
  },
  {
    id: 'baton-pending-ambiguous-1',
    conversationId: 'conv-3',
    triggerMessageId: 'msg-3-trigger',
    replyMessageId: 'msg-3-reply',
    status: 'PENDING_REVIEW',
    autoSendEnabled: true,
    expiresAt: null,
    activatedAt: hoursAgo(5),
    completedAt: null,
  },
  {
    id: 'baton-waiting-1',
    conversationId: 'conv-4',
    triggerMessageId: 'msg-4-trigger',
    replyMessageId: null,
    status: 'WAITING',
    autoSendEnabled: true,
    expiresAt: hoursAgo(-21), // 앞으로 21시간 후 만료
    activatedAt: hoursAgo(3),
    completedAt: null,
  },
]

export const mockClassifications: Record<string, Classification> = {
  'baton-result-1': {
    id: 'clf-result-1',
    batonId: 'baton-result-1',
    selectedBranchId: 'baton-result-1-branch-a',
    candidateBranchIds: null,
    confidence: 0.94,
    isAmbiguous: false,
    containsNewQuestion: false,
    reasoningSummary: '상대가 제안된 우선순위에 명확히 동의함 — 분기 A(긍정적 수락)와 일치.',
    resultStatus: 'MATCHED',
  },
  'baton-pending-new-question-1': {
    id: 'clf-pending-nq-1',
    batonId: 'baton-pending-new-question-1',
    selectedBranchId: null,
    candidateBranchIds: null,
    confidence: null,
    isAmbiguous: false,
    containsNewQuestion: true,
    reasoningSummary: '일정엔 동의했지만 "배포까지 가는지"를 묻는 새로운 질문이 포함되어 자동 발송을 보류함.',
    resultStatus: 'GUARDRAIL_REJECTED',
  },
  'baton-pending-ambiguous-1': {
    id: 'clf-pending-amb-1',
    batonId: 'baton-pending-ambiguous-1',
    selectedBranchId: null,
    // 실제 API 응답에도 후보 분기 목록 필드는 없다 — 화면에서는 candidateBranchIds가
    // null이면 해당 바통의 branches 전체를 후보로 보여준다 (docs/api-integration.md 참고).
    candidateBranchIds: null,
    confidence: 0.51,
    isAmbiguous: true,
    containsNewQuestion: false,
    reasoningSummary: '"나쁘지 않은 것 같다"는 긍정도 부정도 아니어서 분기 A/B 중 확신도가 낮음.',
    resultStatus: 'AMBIGUOUS',
  },
}

export const mockExecutions: Record<string, Execution> = {
  'baton-result-1': {
    id: 'exec-result-1',
    batonId: 'baton-result-1',
    classificationId: 'clf-result-1',
    executionStatus: 'SUCCESS',
    resultMessageId: 'msg-2-sent',
    executedAt: hoursAgo(2),
    failureReason: null,
  },
}
