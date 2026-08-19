import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import type { Baton, Classification, Conversation } from '../types'
import { AppShell } from '../components/layout/AppShell'
import { buttonClasses } from '../lib/buttonClasses'

function initials(name: string) {
  return name
    .split(' ')
    .map((p) => p[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

function formatElapsed(activatedAt: string | null) {
  if (!activatedAt) return ''
  const ms = Date.now() - new Date(activatedAt).getTime()
  const hours = Math.floor(ms / 3_600_000)
  const minutes = Math.floor((ms % 3_600_000) / 60_000)
  return hours > 0 ? `경과: ${hours}시간 ${minutes}분` : `경과: ${minutes}분`
}

function formatLocalTime(timezone: string | null) {
  if (!timezone) return ''
  try {
    return new Intl.DateTimeFormat('ko-KR', { hour: '2-digit', minute: '2-digit', timeZone: timezone }).format(
      new Date(),
    )
  } catch {
    return ''
  }
}

function statusMeta(baton: Baton, classification: Classification | null) {
  if (baton.status === 'COMPLETED' || baton.status === 'EXECUTED') {
    return { border: 'border-l-emerald-600', label: '발송 완료 — Slack에서 확인하세요', action: '결과 확인', to: `/batons/${baton.id}/result` }
  }
  if (baton.status === 'PENDING_REVIEW') {
    const label = classification?.containsNewQuestion
      ? '검토 필요 — 직접 확인이 필요해요'
      : '검토 필요 — AI가 판단하지 못했어요'
    return { border: 'border-l-amber-500', label, action: '응답 처리', to: `/batons/${baton.id}/pending` }
  }
  if (baton.status === 'EXPIRED') {
    return { border: 'border-l-border-strong', label: '만료됨 — 답장이 오지 않았어요', action: '상세 보기', to: `/batons/${baton.id}` }
  }
  if (baton.status === 'CANCELLED') {
    return { border: 'border-l-border-strong', label: '취소됨', action: '상세 보기', to: `/batons/${baton.id}` }
  }
  if (baton.status === 'ERROR') {
    return { border: 'border-l-red-500', label: '오류 발생', action: '상세 보기', to: `/batons/${baton.id}` }
  }
  // WAITING (및 DRAFT/ARMED — 정상 흐름에선 activateBaton이 끝나야 홈에 표시되므로 실질적으로 안 나타남)
  return { border: 'border-l-border-strong', label: '답장 기다리는 중', action: '상세 보기', to: `/batons/${baton.id}` }
}

export function Home() {
  const [batons, setBatons] = useState<Baton[]>([])
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [classifications, setClassifications] = useState<Record<string, Classification | null>>({})
  const [kpi, setKpi] = useState<{ activeBatons: number; needsAttention: number } | null>(null)

  useEffect(() => {
    api.getBatons().then(async (list) => {
      setBatons(list)
      const pendingReview = list.filter((b) => b.status === 'PENDING_REVIEW')
      const entries = await Promise.all(
        pendingReview.map(async (b) => [b.id, await api.getClassification(b.id)] as const),
      )
      setClassifications(Object.fromEntries(entries))
    })
    api.getConversations().then(setConversations)
    api.getDashboardMetrics().then(setKpi).catch(() => {})
  }, [])

  const counts = {
    전체: batons.length,
    '발송 완료': batons.filter((b) => b.status === 'COMPLETED' || b.status === 'EXECUTED').length,
    대기중: kpi?.activeBatons ?? batons.filter((b) => b.status === 'WAITING').length,
    '검토 필요': kpi?.needsAttention ?? batons.filter((b) => b.status === 'PENDING_REVIEW').length,
  }

  return (
    <AppShell>
      <div className="flex items-start justify-between">
        <div>
          <h1 className="font-suit text-2xl font-semibold text-ink">바통 홈</h1>
          <p className="font-suit mt-1 text-sm text-muted-2">마지막 동기화: 2분 전 (14:32 KST)</p>
        </div>
        <Link className={buttonClasses('primary', 'rounded-[5px] px-6 py-3 text-base')} to="/conversations">
          새 바통 만들기
        </Link>
      </div>

      <div className="mt-8 grid grid-cols-4 gap-6">
        {Object.entries(counts).map(([label, value], i) => (
          <div
            className={`rounded-[10px] border p-6 ${i === 0 ? 'border-primary bg-primary-soft' : 'border-border-strong bg-white'}`}
            key={label}
          >
            <p className="font-suit text-sm text-ink">{label}</p>
            <p className="font-suit mt-6 text-3xl text-ink">{value}</p>
          </div>
        ))}
      </div>

      <div className="mt-6 flex flex-col gap-2">
        {batons.map((baton) => {
          const conversation = conversations.find((c) => c.id === baton.conversationId)
          const name = conversation?.counterpartName ?? conversation?.title ?? baton.conversationId
          const meta = statusMeta(baton, classifications[baton.id] ?? null)
          return (
            <Link
              className={`flex items-center justify-between rounded-[6px] border border-y border-r border-border border-l-[3px] bg-white p-4 hover:bg-primary-soft/30 ${meta.border}`}
              key={baton.id}
              to={meta.to}
            >
              <div className="flex items-center gap-3">
                <span className="flex size-[30px] items-center justify-center rounded-full bg-border-strong text-[10px] font-medium text-white">
                  {initials(name)}
                </span>
                <div>
                  <p className="text-[13px] font-bold text-ink">{name}</p>
                  <p className="text-[13px] text-ink">
                    {conversation?.conversationType === 'CHANNEL'
                      ? `# ${conversation.title ?? ''}`
                      : conversation?.title
                        ? `DM · # ${conversation.title}`
                        : 'DM'}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-[13px] text-ink">{meta.label}</span>
                <span className="text-[13px] text-muted">{meta.action}</span>
              </div>
              <div className="flex gap-4 text-[11px] text-[#94a3b8]">
                <span>{formatLocalTime(conversation?.counterpartTimezone ?? null)}</span>
                <span>{formatElapsed(baton.activatedAt)}</span>
              </div>
            </Link>
          )
        })}
      </div>
    </AppShell>
  )
}
