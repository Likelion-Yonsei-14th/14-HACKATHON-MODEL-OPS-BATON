import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Conversation } from '../types'
import { AppShell } from '../components/layout/AppShell'
import { Panel } from '../components/ui/Panel'
import { buttonClasses } from '../lib/buttonClasses'

function conversationLabel(conversation: Conversation): string {
  return conversation.title ?? conversation.counterpartName ?? ''
}

/**
 * 대화 이름에 검색어 글자가 순서대로(연속일 필요는 없음) 다 들어있으면 매치로 본다 —
 * 오타나 일부만 입력해도 비슷한 채널/DM을 찾을 수 있게 하기 위한 fuzzy 매치.
 * 매치 품질은 글자가 얼마나 붙어있는지(연속 매치일수록 높은 점수)로 정렬한다.
 */
function fuzzyScore(label: string, query: string): number | null {
  const text = label.toLowerCase()
  const q = query.toLowerCase()
  if (q === '') return 0

  let score = 0
  let textIndex = 0
  let consecutiveRun = 0
  for (const char of q) {
    const foundAt = text.indexOf(char, textIndex)
    if (foundAt === -1) return null
    consecutiveRun = foundAt === textIndex ? consecutiveRun + 1 : 1
    score += consecutiveRun
    textIndex = foundAt + 1
  }
  return score
}

export function ConversationPicker() {
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [search, setSearch] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    async function load() {
      const conn = await api.getPlatformConnection().catch(() => null)
      if (!conn || conn.connectionStatus !== 'CONNECTED') {
        navigate('/connect', { replace: true })
        return
      }
      try {
        await api.syncConversations(conn.id)
      } catch { /* 동기화 실패해도 기존 목록은 보여줌 */ }
      api.getConversations().then(setConversations)
    }
    load()
  }, [navigate])

  const filteredConversations = useMemo(() => {
    if (search.trim() === '') return conversations
    return conversations
      .map((conversation) => ({ conversation, score: fuzzyScore(conversationLabel(conversation), search.trim()) }))
      .filter((entry): entry is { conversation: Conversation; score: number } => entry.score !== null)
      .sort((a, b) => b.score - a.score)
      .map((entry) => entry.conversation)
  }, [conversations, search])

  return (
    <AppShell>
      <h1 className="font-suit text-2xl font-semibold text-ink">대화 선택</h1>
      <p className="font-suit mt-2 text-muted-2">바통을 만들 Slack 채널 또는 DM을 선택하세요.</p>

      <div className="mt-6 flex gap-4">
        <input
          className="font-suit w-full max-w-lg rounded-[6px] border border-border bg-primary-soft px-4 py-2 text-sm outline-none"
          onChange={(e) => setSearch(e.target.value)}
          placeholder="검색"
          type="text"
          value={search}
        />
      </div>

      <div className="mt-6 flex flex-col gap-3">
        {filteredConversations.map((conversation) => (
          <Panel className="flex items-center justify-between" key={conversation.id}>
            <div>
              <p className="font-semibold text-ink">
                {conversation.conversationType === 'CHANNEL' ? '# ' : '@ '}
                {conversationLabel(conversation)}
              </p>
              <p className="mt-1 text-sm text-muted">
                {conversation.conversationType === 'CHANNEL' ? '채널' : 'DM'}
              </p>
            </div>
            <Link className={buttonClasses('primary')} to={`/conversations/${conversation.id}/compose`}>
              대화 선택
            </Link>
          </Panel>
        ))}
        {search.trim() !== '' && filteredConversations.length === 0 && (
          <p className="font-suit text-sm text-muted">'{search}'와(과) 비슷한 채널/DM을 찾지 못했어요.</p>
        )}
      </div>
    </AppShell>
  )
}
