import { useEffect, useState } from 'react'
import { modelLabApi } from '../../api/modelLab/client'
import type { DeploymentHistoryEntry, ModelConfig, ModelLabTaskType } from '../../types/modelLab'
import { ConfirmDialog, EmptyState, MlBadge, MlButton, MlPanel, MlSelect, PageHeader } from './ui'

export function Deployment() {
  const [taskType, setTaskType] = useState<ModelLabTaskType>('REPLY_CLASSIFICATION')
  const [production, setProduction] = useState<ModelConfig | null>(null)
  const [candidates, setCandidates] = useState<ModelConfig[]>([])
  const [candidateId, setCandidateId] = useState('')
  const [history, setHistory] = useState<DeploymentHistoryEntry[]>([])
  const [confirming, setConfirming] = useState<'promote' | 'rollback' | null>(null)
  const [error, setError] = useState<string | null>(null)

  function reload() {
    modelLabApi.getProductionConfig(taskType).then(setProduction)
    modelLabApi.listModelConfigs(taskType).then((cs) => {
      setCandidates(cs.filter((c) => c.status !== 'PRODUCTION'))
    })
    modelLabApi.getDeploymentHistory(taskType).then(setHistory)
  }

  useEffect(reload, [taskType])

  async function doPromote() {
    setError(null)
    try {
      await modelLabApi.promote(candidateId)
      setConfirming(null)
      reload()
    } catch (e) {
      setError(String((e as Error).message ?? e))
    }
  }

  async function doRollback() {
    setError(null)
    try {
      await modelLabApi.rollback(taskType)
      setConfirming(null)
      reload()
    } catch (e) {
      setError(String((e as Error).message ?? e))
    }
  }

  const candidate = candidates.find((c) => c.id === candidateId)

  return (
    <div>
      <PageHeader description="Production은 작업 유형당 정확히 하나의 config입니다. Promote 전에 현재 Production과 나란히 비교해서 보여줍니다." title="Deployment" />
      <div className="p-6">
        <div className="mb-4 flex gap-1">
          {(['REPLY_CLASSIFICATION', 'BRANCH_GENERATION'] as ModelLabTaskType[]).map((t) => (
            <button className={`rounded-[3px] border px-2 py-1 text-[11px] ${t === taskType ? 'border-[#5b8def] bg-[#101d2e] text-[#5b8def]' : 'border-[#2a2f37] text-[#9aa4b2]'}`} key={t} onClick={() => setTaskType(t)} type="button">
              {t === 'REPLY_CLASSIFICATION' ? 'Classification' : 'Generation'}
            </button>
          ))}
        </div>

        <div className="grid grid-cols-2 gap-4">
          <MlPanel title="현재 Production">
            {production ? (
              <div className="text-[12px]">
                <div className="font-medium text-white">{production.name}</div>
                <div className="mt-1 text-[#8b93a1]">
                  {production.baseModel} · threshold {production.confidenceThreshold ?? '—'} · temp {production.temperature}
                </div>
                <MlButton className="mt-3" disabled={!history.some((h) => h.action === 'PROMOTE')} onClick={() => setConfirming('rollback')} variant="danger">
                  롤백
                </MlButton>
              </div>
            ) : (
              <EmptyState>아직 이 작업에 대한 Production Config가 없습니다.</EmptyState>
            )}
          </MlPanel>

          <MlPanel title="후보 승격">
            <MlSelect onChange={(e) => setCandidateId(e.target.value)} value={candidateId}>
              <option value="">Config 선택…</option>
              {candidates.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.status})
                </option>
              ))}
            </MlSelect>
            <MlButton className="mt-3" disabled={!candidateId} onClick={() => setConfirming('promote')} variant="primary">
              Production으로 승격
            </MlButton>
            {error && <p className="mt-2 text-[11px] text-[#f87171]">{error}</p>}
          </MlPanel>
        </div>

        <MlPanel className="mt-4" title="Deployment 이력">
          {history.length === 0 ? (
            <EmptyState>아직 Deployment 이력이 없습니다.</EmptyState>
          ) : (
            <ul className="space-y-1.5 text-[12px]">
              {history.map((h) => (
                <li className="flex items-center gap-2" key={h.id}>
                  <MlBadge tone={h.action === 'PROMOTE' ? 'good' : 'warn'}>{h.action}</MlBadge>
                  <span className="text-[#8b93a1]">
                    {h.fromConfigId ? `#${h.fromConfigId} → ` : ''}#{h.toConfigId} by user #{h.performedBy} · {new Date(h.createdAt).toLocaleString()}
                  </span>
                  {h.note && <span className="text-[#5b6270]">— {h.note}</span>}
                </li>
              ))}
            </ul>
          )}
        </MlPanel>
      </div>

      {confirming === 'promote' && candidate && (
        <ConfirmDialog
          confirmLabel="승격"
          description={
            <div>
              <p>
                <strong className="text-white">{candidate.name}</strong>을(를) {taskType}의 production으로 승격합니다.
              </p>
              {production && (
                <p className="mt-2">
                  현재 production인 <strong className="text-white">{production.name}</strong>은(는) 삭제되지 않고 archive 처리됩니다 — 나중에 다시 롤백할 수 있습니다.
                </p>
              )}
            </div>
          }
          onCancel={() => setConfirming(null)}
          onConfirm={doPromote}
          title="승격 확인"
        />
      )}
      {confirming === 'rollback' && production && (
        <ConfirmDialog
          confirmLabel="롤백"
          danger
          description={
            <p>
              {taskType}의 <strong className="text-white">{production.name}</strong>을(를) 이전 production config로 롤백합니다.
            </p>
          }
          onCancel={() => setConfirming(null)}
          onConfirm={doRollback}
          title="롤백 확인"
        />
      )}
    </div>
  )
}
