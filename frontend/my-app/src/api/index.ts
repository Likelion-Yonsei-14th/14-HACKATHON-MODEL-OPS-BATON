import { mockApiClient } from './mock/client'
import { httpApiClient } from './http/client'
import type { BatonApiClient } from './client'

// VITE_BATON_API_BASE_URL이 설정돼 있으면 실제 백엔드로, 아니면 목데이터로 붙는다.
// 나머지 코드는 전부 BatonApiClient 인터페이스에만 의존하므로 이 파일만 보면 된다.
export const api: BatonApiClient = import.meta.env.VITE_BATON_API_BASE_URL ? httpApiClient : mockApiClient

export type { BatonApiClient } from './client'
