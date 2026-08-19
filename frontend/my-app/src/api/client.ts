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
} from '../types'

/**
 * 프론트가 백엔드에 기대하는 API 계약. "BATON API 명세서"(실제 엔드포인트 문서) 기준으로
 * 설계했다. 지금은 mock/client.ts와 http/client.ts 두 구현체가 있고, src/api/index.ts가
 * 환경변수(VITE_BATON_API_BASE_URL 존재 여부)로 둘 중 하나를 고른다.
 */
export interface BatonApiClient {
  /** POST /api/users — 회원가입. 반환된 apiKey를 localStorage에 저장해 Bearer 인증에 사용한다. */
  signUp(params: { name: string; email: string; password: string }): Promise<{ apiKey: string }>
  /** POST /api/login — 로그인. 반환된 apiKey를 localStorage에 저장해 Bearer 인증에 사용한다. */
  login(params: { email: string; password: string }): Promise<{ apiKey: string }>
  /** POST /api/logout — 로그아웃. 서버 세션 종료 후 로컬 apiKey를 제거한다. */
  logout(): Promise<void>
  getCurrentUser(): Promise<User>
  updateUser(patch: { name?: string; language?: string; timezone?: string; llmProvider?: LlmProvider }): Promise<User>
  getPlatformConnection(): Promise<PlatformConnection | null>
  /** 특정 Slack 연결의 대화 목록 메타데이터를 동기화한다. */
  syncConversations(connectionId: string): Promise<void>
  /** 특정 대화의 최근 메시지를 동기화한다. */
  syncMessages(conversationId: string): Promise<void>
  /** 홈 화면 KPI 지표를 조회한다. */
  getDashboardMetrics(): Promise<{
    activeBatons: number
    needsAttention: number
    completedWhileOffline: number
    roundTripsSkipped: number
    waitingTimeSavedMinutes: number
  }>
  /** 연결된 플랫폼을 해제한다. */
  disconnectPlatform(connectionId: string): Promise<void>

  /** Slack OAuth authorize URL을 발급받는다. 호출 측은 반환된 redirectUrl로 location을 이동시키면 된다. */
  startSlackConnect(): Promise<{ redirectUrl: string }>

  getConversations(): Promise<Conversation[]>
  getConversation(conversationId: string): Promise<Conversation>
  getMessages(conversationId: string): Promise<Message[]>

  getBatons(): Promise<Baton[]>
  getBaton(batonId: string): Promise<Baton>

  /**
   * 바통 생성(1단계 → 2단계 전이). 실제 백엔드는 세 단계로 나뉜다 —
   * 트리거 메시지 발송 → BATON 초안(DRAFT) 생성 → AI 분기 3개 생성(생성 시점에 이미 저장됨).
   * 이 메서드는 세 호출을 묶어서 노출한다. 반환된 branches는 이미 서버에 저장된 상태라
   * id로 바로 updateBranch를 호출할 수 있다.
   */
  startBaton(
    conversationId: string,
    triggerMessageText: string,
  ): Promise<{ baton: Baton; branches: Branch[] }>

  /** 분기 준비(2단계) 화면에서 사용자가 응답 초안 등을 수정할 때. */
  updateBranch(
    batonId: string,
    branchId: string,
    patch: Partial<Pick<Branch, 'name' | 'description' | 'responseText'>>,
  ): Promise<void>

  /**
   * 발송 확인(3단계) 화면에서 "바통 시작하기" 클릭 — 발송 설정(자동발송/최대 대기 시간) 반영 후
   * BATON을 WAITING 상태로 활성화한다.
   */
  activateBaton(
    batonId: string,
    options: { autoSendEnabled: boolean; maxWaitHours: number },
  ): Promise<Baton>

  getBranches(batonId: string): Promise<Branch[]>
  /** 가장 최근 판정 1건. 아직 판정이 없으면(답장 전) null. */
  getClassification(batonId: string): Promise<Classification | null>
  /** 가장 최근 실행 1건. 아직 실행이 없으면 null. */
  getExecution(batonId: string): Promise<Execution | null>

  /**
   * 보류(PENDING_REVIEW) 응답 처리 화면에서 "발송" 클릭.
   * isAmbiguous 케이스면 branchId를, 그 외(containsNewQuestion 등)엔 customText를 넘긴다.
   */
  resolveBaton(
    batonId: string,
    response: { branchId: string } | { customText: string },
  ): Promise<void>
}
