import { API_BASE_URL, API_KEY_STORAGE_KEY } from './config'

/** 백엔드 공통 에러 응답의 { code, message }를 그대로 들고 있는 에러. */
export class BatonApiError extends Error {
  code: string

  constructor(code: string, message: string) {
    super(message)
    this.name = 'BatonApiError'
    this.code = code
  }
}

interface Envelope<T> {
  success: boolean
  data: T | null
  error: { code: string; message: string } | null
}

/**
 * BATON API 명세서 공통 규칙: `/api` prefix, `Authorization: Bearer <api_key>`,
 * 성공 `{success: true, data, error: null}` / 실패 `{success: false, data: null, error}`.
 */
export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  if (!API_BASE_URL) {
    throw new BatonApiError('CONFIG_MISSING', 'VITE_BATON_API_BASE_URL이 설정되지 않았습니다.')
  }

  const apiKey = localStorage.getItem(API_KEY_STORAGE_KEY)

  const res = await fetch(`${API_BASE_URL}/api${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(apiKey ? { Authorization: `Bearer ${apiKey}` } : {}),
      ...init?.headers,
    },
  })

  if (res.status === 401 && apiKey && path !== '/login') {
    localStorage.removeItem(API_KEY_STORAGE_KEY)
    window.location.replace('/login')
    throw new BatonApiError('UNAUTHORIZED', '세션이 만료되었습니다. 다시 로그인해주세요.')
  }

  let body: Envelope<T>
  try {
    body = await res.json()
  } catch {
    throw new BatonApiError('INVALID_RESPONSE', `서버 응답을 해석할 수 없습니다 (HTTP ${res.status}).`)
  }

  if (!body.success || body.data === null) {
    throw new BatonApiError(body.error?.code ?? 'UNKNOWN_ERROR', body.error?.message ?? '알 수 없는 오류가 발생했습니다.')
  }

  return body.data
}
