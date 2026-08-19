/**
 * 실제 백엔드 연결 설정.
 *
 * VITE_BATON_API_BASE_URL — 예: https://api.baton.example.com
 * 인증은 Authorization: Bearer <api_key> 방식. POST /api/users 회원가입 후 응답으로 받은
 * api_key를 localStorage(API_KEY_STORAGE_KEY)에 저장하고, 이후 모든 요청에 자동 첨부한다.
 */
export const API_BASE_URL = import.meta.env.VITE_BATON_API_BASE_URL as string | undefined

/** localStorage에 API 키를 저장할 때 쓰는 키. */
export const API_KEY_STORAGE_KEY = 'baton_api_key'
