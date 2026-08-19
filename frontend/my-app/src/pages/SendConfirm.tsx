import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import { AppShell } from '../components/layout/AppShell'
import { Panel } from '../components/ui/Panel'
import { StepTabs } from '../components/ui/StepTabs'
import { Button } from '../components/ui/Button'

const waitOptions = [
  { label: '1시간', hours: 1 },
  { label: '4시간', hours: 4 },
  { label: '12시간', hours: 12 },
  { label: '24시간', hours: 24 },
  { label: '48시간', hours: 48 },
]

export function SendConfirm() {
  const { conversationId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const batonId = (location.state as { batonId?: string } | null)?.batonId

  const [autoSendEnabled] = useState(false)
  const [maxWaitHours, setMaxWaitHours] = useState(24)
  const [starting, setStarting] = useState(false)

  useEffect(() => {
    if (!batonId) navigate(`/conversations/${conversationId}/compose`, { replace: true })
  }, [batonId, conversationId, navigate])

  if (!batonId) return null

  async function handleStart() {
    setStarting(true)
    await api.activateBaton(batonId!, { autoSendEnabled, maxWaitHours })
    navigate('/home')
  }

  return (
    <AppShell>
      <h1 className="font-suit text-2xl font-semibold text-ink">답장 준비하기</h1>
      <div className="mt-4">
        <StepTabs current={3} />
      </div>

      <Panel className="mt-6 max-w-2xl">
        <p className="font-semibold text-ink">발송 설정</p>

        <label className="mt-4 block text-sm text-ink" htmlFor="max-wait">
          최대 대기 시간
        </label>
        <select
          className="font-suit mt-2 w-full rounded-[6px] border border-border p-3 text-sm outline-none focus:border-primary"
          id="max-wait"
          onChange={(e) => setMaxWaitHours(Number(e.target.value))}
          value={maxWaitHours}
        >
          {waitOptions.map((opt) => (
            <option key={opt.hours} value={opt.hours}>
              {opt.label}
            </option>
          ))}
        </select>
        <p className="mt-4 text-sm text-muted">대기 시간 내 상대 답장이 없으면 바통이 만료 상태로 전환됩니다.</p>
      </Panel>

      <div className="mt-6 flex max-w-2xl items-center justify-between">
        <button
          className="font-suit text-sm text-muted-2 hover:text-ink"
          onClick={() => navigate(`/conversations/${conversationId}/branches`, { state: { batonId } })}
          type="button"
        >
          이전 단계로
        </button>
        <Button disabled={starting} onClick={handleStart}>
          바통 시작하기
        </Button>
      </div>
    </AppShell>
  )
}
