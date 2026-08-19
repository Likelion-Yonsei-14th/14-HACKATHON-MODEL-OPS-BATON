import type { Baton, Branch } from '../../types'
import type { BatonApiClient } from '../client'
import {
  mockBatons,
  mockBranches,
  mockClassifications,
  mockConversations,
  mockExecutions,
  mockMessages,
  mockPlatformConnection,
  mockUser,
} from './data'

const LATENCY_MS = 300

function delay<T>(value: T): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), LATENCY_MS))
}

// 목 데이터는 모듈 스코프의 배열/객체를 그대로 들고 있다가 뮤테이션한다 —
// 새로고침하면 초기 상태로 리셋된다. 실제 API 붙이면 이 파일만 안 쓰면 된다.
const batons = [...mockBatons]
const branchesByBaton: Record<string, Branch[]> = { ...mockBranches }

let nextDraftSeq = 0
let currentUser = { ...mockUser }

export const mockApiClient: BatonApiClient = {
  async signUp(_params) {
    return delay({ apiKey: 'mock-api-key' })
  },

  async login(_params) {
    return delay({ apiKey: 'mock-api-key' })
  },

  async logout() {
    return delay(undefined)
  },

  async getCurrentUser() {
    return delay({ ...currentUser })
  },

  async updateUser(patch) {
    currentUser = { ...currentUser, ...patch }
    return delay({ ...currentUser })
  },

  async syncConversations(_connectionId) {
    return delay(undefined)
  },

  async syncMessages(_conversationId) {
    return delay(undefined)
  },

  async getDashboardMetrics() {
    const activeBatons = batons.filter((b) => b.status === 'WAITING').length
    const needsAttention = batons.filter((b) => b.status === 'PENDING_REVIEW').length
    const completedWhileOffline = batons.filter((b) => b.status === 'COMPLETED' || b.status === 'EXECUTED').length
    return delay({
      activeBatons,
      needsAttention,
      completedWhileOffline,
      roundTripsSkipped: completedWhileOffline,
      waitingTimeSavedMinutes: completedWhileOffline * 30,
    })
  },

  async disconnectPlatform(_connectionId) {
    return delay(undefined)
  },

  async getPlatformConnection() {
    return delay(mockPlatformConnection)
  },

  async startSlackConnect() {
    // 실제로는 Slack OAuth authorize URL. 목 환경에선 콜백 화면으로 바로 이동시켜
    // 스켈레톤 시절의 데모 동선을 유지한다.
    return delay({ redirectUrl: '/connect/callback' })
  },

  async getConversations() {
    return delay(mockConversations)
  },

  async getConversation(conversationId) {
    const conversation = mockConversations.find((c) => c.id === conversationId)
    if (!conversation) throw new Error(`conversation not found: ${conversationId}`)
    return delay(conversation)
  },

  async getMessages(conversationId) {
    return delay(mockMessages.filter((m) => m.conversationId === conversationId))
  },

  async getBatons() {
    return delay(batons)
  },

  async getBaton(batonId) {
    const baton = batons.find((b) => b.id === batonId)
    if (!baton) throw new Error(`baton not found: ${batonId}`)
    return delay(baton)
  },

  async startBaton(conversationId, triggerMessageText) {
    // 실제로는 메시지 발송 → BATON(DRAFT) 생성 → LLM 호출 1회(분기 3개 생성)의 3단계.
    // 목데이터는 트리거 메시지 내용과 무관하게 고정 3분기를 반환한다.
    nextDraftSeq += 1
    const id = `baton-draft-${nextDraftSeq}`
    const baton: Baton = {
      id,
      conversationId,
      triggerMessageId: `msg-${id}-trigger`,
      replyMessageId: null,
      status: 'DRAFT',
      autoSendEnabled: false,
      expiresAt: null,
      activatedAt: null,
      completedAt: null,
    }
    const branches: Branch[] = [
      {
        id: `${id}-branch-a`,
        batonId: id,
        name: '분기 A — 긍정적 수락',
        description: '상대방이 제안을 수락하거나 동의하는 경우',
        responseText: `좋습니다. "${triggerMessageText.slice(0, 20)}" 관련해 말씀해주신 대로 진행하겠습니다.`,
        executionMode: 'AUTO',
        sortOrder: 0,
      },
      {
        id: `${id}-branch-b`,
        batonId: id,
        name: '분기 B — 조건부 또는 추가 질문',
        description: '상대방이 조건을 달거나 추가 정보를 요청하는 경우',
        responseText: '확인했습니다. 필요한 조건을 알려주시면 그 기준에 맞춰 다시 정리해서 공유드리겠습니다.',
        executionMode: 'AUTO',
        sortOrder: 1,
      },
      {
        id: `${id}-branch-c`,
        batonId: id,
        name: '분기 C — 부정적 거절 또는 보류',
        description: '상대방이 거절하거나 판단을 미루는 경우',
        responseText: '알겠습니다. 지금 진행이 어렵다면 대안 일정이나 담당자를 알려주시면 그 방향으로 맞추겠습니다.',
        executionMode: 'AUTO',
        sortOrder: 2,
      },
    ]
    batons.push(baton)
    branchesByBaton[id] = branches
    return delay({ baton, branches })
  },

  async updateBranch(batonId, branchId, patch) {
    const branches = branchesByBaton[batonId]
    if (!branches) throw new Error(`baton not found: ${batonId}`)
    const branch = branches.find((b) => b.id === branchId)
    if (!branch) throw new Error(`branch not found: ${branchId}`)
    Object.assign(branch, patch)
    return delay(undefined)
  },

  async activateBaton(batonId, options) {
    const baton = batons.find((b) => b.id === batonId)
    if (!baton) throw new Error(`baton not found: ${batonId}`)
    baton.status = 'WAITING'
    baton.autoSendEnabled = options.autoSendEnabled
    baton.expiresAt = new Date(Date.now() + options.maxWaitHours * 60 * 60_000).toISOString()
    baton.activatedAt = new Date().toISOString()
    return delay(baton)
  },

  async getBranches(batonId) {
    return delay(branchesByBaton[batonId] ?? [])
  },

  async getClassification(batonId) {
    return delay(mockClassifications[batonId] ?? null)
  },

  async getExecution(batonId) {
    return delay(mockExecutions[batonId] ?? null)
  },

  async resolveBaton(batonId, _response) {
    const baton = batons.find((b) => b.id === batonId)
    if (!baton) throw new Error(`baton not found: ${batonId}`)
    baton.status = 'COMPLETED'
    baton.completedAt = new Date().toISOString()
    return delay(undefined)
  },
}
