import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import { AppShell } from '../components/layout/AppShell'
import { Panel } from '../components/ui/Panel'
import { StepTabs } from '../components/ui/StepTabs'

function Spinner() {
  return (
    <svg className="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
  )
}

export function ComposeBaton() {
  const { conversationId } = useParams()
  const navigate = useNavigate()
  const [text, setText] = useState('')
  const [starting, setStarting] = useState(false)
  const [startError, setStartError] = useState<string | null>(null)

  useEffect(() => {
    if (!conversationId) return
    api.syncMessages(conversationId).catch(() => {})
  }, [conversationId])

  async function handleStart() {
    if (!conversationId || !text.trim()) return
    setStarting(true)
    setStartError(null)
    try {
      const { baton, branches } = await api.startBaton(conversationId, text)
      navigate(`/conversations/${conversationId}/branches`, { state: { batonId: baton.id, branches } })
    } catch {
      setStartError('분기를 생성하지 못했습니다. 다시 시도해주세요.')
      setStarting(false)
    }
  }

  return (
    <AppShell>
      <div className="flex items-start justify-between">
        <div>
          <h1 className="font-suit text-2xl font-semibold text-ink">답장 준비하기</h1>
          <div className="mt-4">
            <StepTabs current={1} />
          </div>
        </div>
        <button
          className="font-suit inline-flex items-center gap-2 rounded-[6px] bg-primary px-6 py-3 text-sm font-semibold text-white disabled:opacity-40"
          disabled={!text.trim() || starting}
          onClick={handleStart}
          type="button"
        >
          {starting && <Spinner />}
          {starting ? 'AI가 분기 생성 중...' : '이 대화로 바통 만들기'}
        </button>
      </div>

      <Panel className="mt-6 max-w-3xl">
        <div className="flex items-center justify-between">
          <div>
            <p className="font-semibold text-ink">메시지 작성</p>
            <span className="font-mono mt-2 inline-block rounded-[4px] bg-[#f9f8f6] px-2 py-1 text-xs text-muted">
              conversationId: {conversationId}
            </span>
          </div>
          <span className="text-sm text-muted-2">대화 맥락 보기</span>
        </div>

        <label className="mt-4 block text-sm text-ink" htmlFor="trigger-message">
          상대에게 보낼 메시지
        </label>
        <textarea
          className="font-suit mt-2 w-full rounded-[6px] border border-border p-3 text-sm outline-none focus:border-primary"
          id="trigger-message"
          onChange={(e) => setText(e.target.value)}
          placeholder="보낼 메시지"
          rows={4}
          value={text}
        />
        <p className="mt-4 text-sm text-muted">이 바통은 한 번의 왕복(메시지 1회, 응답 1회)만 준비합니다.</p>
        {starting && (
          <p className="mt-3 flex items-center gap-2 text-sm text-primary">
            <Spinner />
            AI가 예상 답변 분기를 만들고 있어요. 로컬 모델은 최대 1분 정도 걸릴 수 있어요 — 오류 아니니 잠시만 기다려주세요.
          </p>
        )}
        {startError && <p className="mt-3 text-sm text-red-500">{startError}</p>}
      </Panel>
    </AppShell>
  )
}
