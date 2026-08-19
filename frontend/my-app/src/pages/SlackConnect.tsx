import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Header } from '../components/layout/Header'
import { api } from '../api'
import { BatonApiError } from '../api/http/request'
import { API_KEY_STORAGE_KEY } from '../api/http/config'
import slackLogo from '../assets/slack-logo.png'
import slackIconWhite from '../assets/slack-icon-white.png'

export function SlackConnect() {
  const [connecting, setConnecting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  async function handleConnect() {
    setConnecting(true)
    setError(null)
    try {
      const { redirectUrl } = await api.startSlackConnect()
      window.location.href = redirectUrl
    } catch (e) {
      if (e instanceof BatonApiError && e.code === 'UNAUTHORIZED') {
        localStorage.removeItem(API_KEY_STORAGE_KEY)
        navigate('/login', { replace: true })
        return
      }
      setError('Slack 연결을 시작하지 못했습니다. 다시 시도해주세요.')
      setConnecting(false)
    }
  }

  return (
    <div className="min-h-screen bg-landing">
      <Header />
      <div className="flex justify-center px-8 py-24">
        <div className="w-full max-w-3xl rounded-[10px] bg-white p-16 text-center shadow-[0px_4px_4px_0px_rgba(0,0,0,0.15)]">
          <img alt="Slack" className="mx-auto h-24 w-24 object-contain" src={slackLogo} />
          <h1 className="font-suit mt-8 text-4xl font-semibold tracking-[0.3px] text-ink">
            <span className="text-primary">SLACK</span>을 연결하고
            <br />
            대화를 자동으로 이어가세요
          </h1>
          <p className="font-suit mt-6 text-xl text-muted">
            Baton이 채널과 메시지를 모니터링하고,
            <br />
            AI가 대신 답변해드립니다.
          </p>
          <button
            className="font-suit mt-10 inline-flex items-center gap-3 rounded-[10px] bg-primary px-8 py-4 text-xl font-semibold text-white hover:opacity-90 disabled:opacity-60"
            disabled={connecting}
            onClick={handleConnect}
            type="button"
          >
            <img alt="" className="h-6 w-6" src={slackIconWhite} />
            {connecting ? '연결 중...' : 'Slack 연결하기'}
          </button>
          {error && (
            <p className="font-suit mt-4 text-sm text-red-500">{error}</p>
          )}
        </div>
      </div>
    </div>
  )
}
