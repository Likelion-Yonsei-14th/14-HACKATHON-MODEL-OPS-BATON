import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import type { Branch, Classification, Message } from '../types'
import { AppShell } from '../components/layout/AppShell'
import { Panel } from '../components/ui/Panel'
import { Button } from '../components/ui/Button'
import { Dialog } from '../components/ui/Dialog'

export function PendingResponse() {
  const { batonId } = useParams()
  const navigate = useNavigate()

  const [classification, setClassification] = useState<Classification | null>(null)
  const [branches, setBranches] = useState<Branch[]>([])
  const [replyMessage, setReplyMessage] = useState<Message | null>(null)
  const [customText, setCustomText] = useState('')
  const [selectedBranchId, setSelectedBranchId] = useState<string | null>(null)
  const [confirming, setConfirming] = useState(false)

  useEffect(() => {
    if (!batonId) return
    api.getClassification(batonId).then(async (c) => {
      setClassification(c)
      const baton = await api.getBaton(batonId)
      if (baton.replyMessageId) {
        const messages = await api.getMessages(baton.conversationId)
        setReplyMessage(messages.find((m) => m.id === baton.replyMessageId) ?? null)
      }
    })
    api.getBranches(batonId).then(setBranches)
  }, [batonId])

  if (!classification) {
    return (
      <AppShell>
        <p className="text-muted-2">불러오는 중...</p>
      </AppShell>
    )
  }

  // isAmbiguous가 true일 때만 분기 선택 UI를 쓴다. 그 외(containsNewQuestion 포함, 그리고
  // NO_MATCH/GUARDRAIL_REJECTED처럼 둘 다 false인 경우)는 항상 직접 작성으로 폴백한다.
  const useManualPath = !classification.isAmbiguous || classification.containsNewQuestion
  // candidateBranchIds는 실제 API에 없는 필드라 항상 null이다 — 폴백으로 해당 바통의
  // branches 전체를 후보로 보여준다 (docs/api-integration.md 참고).
  const candidateBranches = classification.candidateBranchIds
    ? branches.filter((b) => classification.candidateBranchIds?.includes(b.id))
    : branches
  const canSend = useManualPath ? !!customText.trim() : !!selectedBranchId

  async function handleConfirmSend() {
    if (!batonId) return
    if (useManualPath) {
      await api.resolveBaton(batonId, { customText })
    } else if (selectedBranchId) {
      await api.resolveBaton(batonId, { branchId: selectedBranchId })
    }
    navigate('/home')
  }

  return (
    <AppShell>
      <div className="font-suit flex items-center gap-2 text-sm">
        <Link className="text-muted-2 hover:text-ink" to="/home">
          바통 홈으로
        </Link>
        <span className="text-ink">›</span>
        <span className="font-bold text-ink">보류 응답 처리</span>
      </div>

      <Panel className="mt-6 max-w-3xl border-amber-200 bg-amber-50">
        <h1 className="font-suit text-3xl font-bold text-ink">⚠ 자동 발송 보류됨</h1>
        <p className="mt-3 text-sm text-muted">{classification.reasoningSummary}</p>
      </Panel>

      <h2 className="font-suit mt-6 text-base font-semibold text-ink">상대 답장</h2>
      {replyMessage && (
        <Panel className="mt-2 max-w-3xl">
          <p className="text-sm font-semibold text-ink">원문</p>
          <p className="mt-2 text-sm text-muted">{replyMessage.content}</p>
          {replyMessage.translatedContent && (
            <>
              <p className="mt-4 text-sm font-semibold text-ink">번역 (한국어)</p>
              <p className="mt-2 text-sm text-muted">{replyMessage.translatedContent}</p>
            </>
          )}
        </Panel>
      )}

      {useManualPath ? (
        <div className="mt-6 max-w-3xl">
          <h2 className="font-suit text-base font-semibold text-ink">상대의 새 질문에 직접 응답 작성</h2>
          <p className="mt-2 text-sm text-muted">
            {classification.containsNewQuestion
              ? '새 질문이 포함되어 있어 확신도와 무관하게 분기 선택 없이 직접 작성해야 합니다.'
              : 'AI가 어느 분기와도 명확히 매칭하지 못했습니다. 직접 응답을 작성해 주세요.'}
          </p>
          <textarea
            className="font-suit mt-3 w-full rounded-[6px] border border-border p-4 text-sm outline-none focus:border-primary"
            onChange={(e) => setCustomText(e.target.value)}
            placeholder="직접 응답 내용을 입력하세요"
            rows={5}
            value={customText}
          />
        </div>
      ) : (
        <div className="mt-6 max-w-3xl">
          <h2 className="font-suit text-base font-semibold text-ink">준비된 응답 분기</h2>
          <p className="mt-2 text-sm text-muted">아래 후보 분기 중 상황에 맞는 분기를 선택하세요. 선택한 분기의 응답 초안이 발송됩니다.</p>
          <div className="mt-3 flex flex-col gap-2">
            {candidateBranches.map((branch) => (
              <label
                className={`flex cursor-pointer items-start gap-3 rounded-[7px] border p-4 ${
                  selectedBranchId === branch.id ? 'border-primary bg-primary-soft' : 'border-border bg-white'
                }`}
                key={branch.id}
              >
                <input
                  checked={selectedBranchId === branch.id}
                  className="mt-1 accent-primary"
                  name="branch"
                  onChange={() => setSelectedBranchId(branch.id)}
                  type="radio"
                />
                <span>
                  <strong className="text-ink">{branch.name}</strong>
                  <p className="mt-1 text-sm text-muted">{branch.responseText}</p>
                </span>
              </label>
            ))}
          </div>
        </div>
      )}

      <Panel className="mt-6 max-w-3xl">
        <p className="font-semibold text-ink">발송 전 확인</p>
        <p className="mt-3 text-sm text-muted">선택한 분기 또는 직접 작성한 내용이 Slack 스레드에 발송됩니다.</p>
        <p className="mt-3 text-sm text-muted">자동 발송 사실과 이후 답장은 Slack에서 직접 확인해야 합니다.</p>
        <div className="mt-4 flex justify-end gap-3">
          <Link className="font-suit self-center text-sm text-muted-2 hover:text-ink" to="/home">
            취소
          </Link>
          <Button disabled={!canSend} onClick={() => setConfirming(true)}>
            응답 발송
          </Button>
        </div>
      </Panel>

      {confirming && (
        <Dialog
          confirmLabel="발송 확정"
          description={
            <>
              <p>선택한 내용을 Slack 스레드에 발송합니다. 발송 후에는 되돌리기가 제한될 수 있습니다.</p>
              <p>자동 발송 사실과 이후 답장은 Slack에서 직접 확인해야 합니다.</p>
            </>
          }
          onCancel={() => setConfirming(false)}
          onConfirm={handleConfirmSend}
          title="응답 발송 확인"
        />
      )}
    </AppShell>
  )
}
