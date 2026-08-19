import type { ConnectionStatus, PlatformType } from './enums'

/**
 * platform_connections 중 프론트에서 쓰는 부분만.
 * access_token_encrypted / refresh_token_encrypted / token_expires_at 은
 * 서버 전용이라 제외했다 — 프론트는 토큰을 절대 다루지 않는다.
 */
export interface PlatformConnection {
  id: string
  platformType: PlatformType
  workspaceId: string
  workspaceName: string | null
  connectionStatus: ConnectionStatus
  lastSyncedAt: string | null
}
