import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api'
import type { Baton, Branch, Conversation, Message } from '../types'
import { AppShell } from '../components/layout/AppShell'
import { Panel } from '../components/ui/Panel'

function formatElapsed(activatedAt: string | null) {
  if (!activatedAt) return '—'
  const ms = Date.now() - new Date(activatedAt).getTime()
  const hours = Math.floor(ms / 3_600_000)
  const minutes = Math.floor((ms % 3_600_000) / 60_000)
  return `${hours}시간 ${minutes}분`
}

export function BatonDetail() {
  const { batonId } = useParams()
  const [baton, setBaton] = useState<Baton | null>(null)
  const [conversation, setConversation] = useState<Conversation | null>(null)
  const [branches, setBranches] = useState<Branch[]>([])
  const [triggerMessage, setTriggerMessage] = useState<Message | null>(null)

  useEffect(() => {
    if (!batonId) return
    api.getBaton(batonId).then(async (b) => {
      setBaton(b)
      const conv = await api.getConversation(b.conversationId)
      setConversation(conv)
      const messages = await api.getMessages(b.conversationId)
      setTriggerMessage(messages.find((m) => m.id === b.triggerMessageId) ?? null)
    })
    api.getBranches(batonId).then(setBranches)
  }, [batonId])

  if (!baton) {
    return (
      <AppShell>
        <p className="text-muted-2">불러오는 중...</p>
      </AppShell>
    )
  }

  const summary = [
    ['대화 상대', `${conversation?.counterpartName ?? ''} · ${conversation?.title ?? ''}`],
    ['상태', baton.status === 'WAITING' ? '대기 중 — 답장 없음' : baton.status],
    ['경과 시간', formatElapsed(baton.activatedAt)],
    ['상대 업무시간', '오전 9:00 – 오후 6:00'],
    ['현지 시각', conversation?.counterpartTimezone ?? '—'],
    ['다음 동기화', '약 12분 후'],
  ]

  return (
    <AppShell>
      <h1 className="font-suit text-2xl font-semibold text-ink">대기 상세</h1>
      <p className="font-suit mt-1 text-sm text-muted">마지막 동기화: 3분 전</p>

      <Panel className="mt-6 max-w-3xl">
        <p className="font-semibold text-ink">바통 상태 요약</p>
        <div className="mt-4 grid grid-cols-3 gap-x-8 gap-y-4">
          {summary.map(([label, value]) => (
            <div key={label}>
              <p className="text-sm text-muted">{label}</p>
              <p className="mt-1 text-sm font-bold text-ink">{value}</p>
            </div>
          ))}
        </div>
      </Panel>

      <Panel className="mt-4 max-w-3xl">
        <p className="font-semibold text-ink">Slack에 보낸 첫 메시지</p>
        {triggerMessage && (
          <>
            <p className="mt-4 text-sm text-muted">{triggerMessage.content}</p>
            <p className="mt-4 text-sm text-muted">
              발송 시각: {new Date(triggerMessage.sentAt).toLocaleTimeString('ko-KR')} (내 시간)
            </p>
          </>
        )}
      </Panel>

      <h2 className="font-suit mt-6 text-lg font-semibold text-ink">준비된 예상 답변 분기</h2>
      <div className="mt-3 flex max-w-3xl flex-col gap-3">
        {branches.map((branch) => (
          <Panel className="flex items-center justify-between" key={branch.id}>
            <div>
              <p className="font-semibold text-ink">{branch.name}</p>
              <p className="mt-1 text-sm text-muted">{branch.executionMode === 'AUTO' ? '자동 처리 예정' : '사용자 확인 필요'}</p>
            </div>
            <p className="max-w-md text-sm text-muted">{branch.responseText}</p>
          </Panel>
        ))}
      </div>
    </AppShell>
  )
}
