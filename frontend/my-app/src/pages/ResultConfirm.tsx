import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import type { Branch, Classification, Conversation, Execution, Message } from '../types'
import { AppShell } from '../components/layout/AppShell'
import { Panel } from '../components/ui/Panel'
import { Button } from '../components/ui/Button'

function formatElapsed(executedAt: string | null) {
  if (!executedAt) return '—'
  const ms = Date.now() - new Date(executedAt).getTime()
  const hours = Math.floor(ms / 3_600_000)
  const minutes = Math.floor((ms % 3_600_000) / 60_000)
  return `${hours}시간 ${minutes}분 전 발송`
}

export function ResultConfirm() {
  const { batonId } = useParams()
  const navigate = useNavigate()

  const [execution, setExecution] = useState<Execution | null>(null)
  const [classification, setClassification] = useState<Classification | null>(null)
  const [branches, setBranches] = useState<Branch[]>([])
  const [conversation, setConversation] = useState<Conversation | null>(null)
  const [sentMessage, setSentMessage] = useState<Message | null>(null)

  useEffect(() => {
    if (!batonId) return
    api.getExecution(batonId).then(setExecution)
    api.getClassification(batonId).then(setClassification)
    api.getBranches(batonId).then(setBranches)
    api.getBaton(batonId).then(async (baton) => {
      const conv = await api.getConversation(baton.conversationId)
      setConversation(conv)
      const messages = await api.getMessages(baton.conversationId)
      const exec = await api.getExecution(batonId)
      setSentMessage(messages.find((m) => m.id === exec?.resultMessageId) ?? null)
    })
  }, [batonId])

  // execution 응답엔 branch_id가 없다 — "어떤 분기가 선택됐는지"는 classification으로 확인한다.
  const matchedBranch = branches.find((b) => b.id === classification?.selectedBranchId)

  // 결과를 "확인"했다는 사실을 서버에 남길 API가 없다(백엔드 API 명세서 기준) — 화면만
  // 벗어나면 되므로 그냥 홈으로 이동한다. docs/api-integration.md 참고.
  function handleConfirm() {
    navigate('/home')
  }

  if (!execution) {
    return (
      <AppShell>
        <p className="text-muted-2">불러오는 중...</p>
      </AppShell>
    )
  }

  return (
    <AppShell>
      <Link className="font-suit text-sm text-muted-2 hover:text-ink" to="/home">
        바통 홈으로
      </Link>
      <h1 className="font-suit mt-2 text-2xl font-semibold text-ink">판정 결과 확인</h1>

      <div className="mt-4 grid max-w-5xl grid-cols-[1fr_320px] gap-5">
        <div className="flex flex-col gap-3">
          <Panel>
            <div className="flex gap-4 text-sm">
              <span className="w-32 font-bold text-ink">선택된 분기</span>
              <span className="text-ink">{matchedBranch?.name ?? '—'}</span>
            </div>
            <div className="mt-3 flex gap-4 text-sm">
              <span className="w-32 font-bold text-ink">자동발송 상태</span>
              <span className="text-ink">{execution.executionStatus === 'SUCCESS' ? '✓ 자동 발송 완료' : execution.executionStatus}</span>
            </div>
            {classification?.confidence != null && (
              <div className="mt-3 flex gap-4 text-sm">
                <span className="w-32 font-bold text-ink">확신도</span>
                <span className="text-ink">{Math.round(classification.confidence * 100)}%</span>
              </div>
            )}
            <p className="mt-4 text-sm font-semibold text-ink">판정 근거</p>
            <p className="mt-2 text-sm text-muted">{classification?.reasoningSummary}</p>
            {execution.executedAt && (
              <p className="mt-3 text-sm text-muted">분석 시각: {new Date(execution.executedAt).toLocaleString('ko-KR')}</p>
            )}
          </Panel>

          {sentMessage && (
            <Panel>
              <p className="font-semibold text-ink">실제 발송된 응답</p>
              <p className="mt-3 text-sm text-muted">{sentMessage.content}</p>
              <p className="mt-3 text-sm text-muted">
                이 메시지는 Baton이 자동으로 발송했습니다. 이후 상대방의 답장은 Slack에서 직접 확인해야 합니다.
              </p>
              <p className="mt-3 text-sm text-muted-2 underline">Slack 원본 스레드 열기</p>
            </Panel>
          )}
        </div>

        <Panel className="h-fit">
          <p className="font-semibold text-ink">바통 정보</p>
          <div className="mt-4 flex flex-col gap-4">
            <div>
              <p className="text-sm text-muted">상대방</p>
              <p className="mt-1 text-sm font-bold text-ink">{conversation?.counterpartName ?? '—'}</p>
            </div>
            <div>
              <p className="text-sm text-muted">상대 현지 시각</p>
              <p className="mt-1 text-sm font-bold text-ink">{conversation?.counterpartTimezone ?? '—'}</p>
            </div>
            <div>
              <p className="text-sm text-muted">경과 시간</p>
              <p className="mt-1 text-sm font-bold text-ink">{formatElapsed(execution.executedAt)}</p>
            </div>
            <div>
              <p className="text-sm text-muted">대화 채널</p>
              <p className="mt-1 text-sm font-bold text-ink">
                {conversation?.title ?? (conversation?.conversationType === 'DM' ? 'DM' : '—')}
              </p>
            </div>
          </div>
        </Panel>
      </div>

      <Button className="mt-6" onClick={handleConfirm}>
        확인
      </Button>
    </AppShell>
  )
}
