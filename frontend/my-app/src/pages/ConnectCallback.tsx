import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'

export function ConnectCallback() {
  const navigate = useNavigate()

  useEffect(() => {
    // Slack이 OAuth 승인 후 리다이렉트하는 곳은 백엔드의 콜백 엔드포인트다
    // (GET /api/platform-connections/slack/callback) — 백엔드가 code 교환과 대화 목록
    // 동기화까지 마친 뒤 이 프론트 경로로 다시 리다이렉트해준다고 가정한다. 그래서 이
    // 화면은 code를 직접 다루지 않고, 연결이 실제로 CONNECTED됐는지만 확인한다.
    api
      .getPlatformConnection()
      .then((connection) => {
        navigate(connection?.connectionStatus === 'CONNECTED' ? '/conversations' : '/connect/error', {
          replace: true,
        })
      })
      .catch(() => navigate('/connect/error', { replace: true }))
  }, [navigate])

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-landing px-8 text-center">
      <p className="font-suit text-lg text-muted-2">Slack 연결 처리 중...</p>
    </div>
  )
}
