import { useEffect, useState } from 'react'
import { modelLabApi } from '../../api/modelLab/client'
import type { ModelLabTaskType, PromptVersion } from '../../types/modelLab'
import { EmptyState, MlBadge, MlButton, MlPanel, MlTextarea, PageHeader } from './ui'

export function Prompts() {
  const [taskType, setTaskType] = useState<ModelLabTaskType>('REPLY_CLASSIFICATION')
  const [versions, setVersions] = useState<PromptVersion[]>([])
  const [systemPrompt, setSystemPrompt] = useState('')
  const [notes, setNotes] = useState('')
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function reload(t: ModelLabTaskType) {
    modelLabApi.listPromptVersions(t).then(setVersions).catch((e) => setError(String(e.message ?? e)))
  }

  useEffect(() => reload(taskType), [taskType])

  async function create() {
    if (!systemPrompt.trim()) return
    setCreating(true)
    setError(null)
    try {
      await modelLabApi.createPromptVersion({ taskType, systemPrompt, notes: notes || undefined })
      setSystemPrompt('')
      setNotes('')
      reload(taskType)
    } catch (e) {
      setError(String((e as Error).message ?? e))
    } finally {
      setCreating(false)
    }
  }

  return (
    <div>
      <PageHeader description="Prompt Version은 추가 전용(append-only)입니다 — 새 버전을 만들어도 기존 버전은 수정되지 않습니다 (spec section 13)." title="Prompts" />
      <div className="grid grid-cols-[280px_1fr] gap-4 p-6">
        <div>
          <label className="mb-1 block text-[11px] uppercase tracking-wide text-[#5b6270]">Task Type</label>
          <div className="flex gap-1">
            {(['REPLY_CLASSIFICATION', 'BRANCH_GENERATION'] as ModelLabTaskType[]).map((t) => (
              <button
                className={`rounded-[3px] border px-2 py-1 text-[11px] ${t === taskType ? 'border-[#5b8def] bg-[#101d2e] text-[#5b8def]' : 'border-[#2a2f37] text-[#9aa4b2]'}`}
                key={t}
                onClick={() => setTaskType(t)}
                type="button"
              >
                {t === 'REPLY_CLASSIFICATION' ? 'Classification' : 'Generation'}
              </button>
            ))}
          </div>

          <MlPanel className="mt-4" title="새 버전">
            <div className="space-y-3">
              <div>
                <label className="mb-1 block text-[11px] text-[#8b93a1]">System Prompt</label>
                <MlTextarea onChange={(e) => setSystemPrompt(e.target.value)} rows={10} value={systemPrompt} />
              </div>
              <div>
                <label className="mb-1 block text-[11px] text-[#8b93a1]">메모</label>
                <MlTextarea onChange={(e) => setNotes(e.target.value)} rows={2} value={notes} />
              </div>
              <MlButton disabled={creating || !systemPrompt.trim()} onClick={create} variant="primary">
                {creating ? '생성 중…' : '버전 생성'}
              </MlButton>
              {error && <p className="text-[11px] text-[#f87171]">{error}</p>}
            </div>
          </MlPanel>
        </div>

        <MlPanel title={`버전 (${versions.length})`}>
          {versions.length === 0 ? (
            <EmptyState>이 작업 유형에 대한 Prompt Version이 아직 없습니다.</EmptyState>
          ) : (
            <div className="space-y-3">
              {versions.map((v) => (
                <div className="rounded-[3px] border border-[#1f2328] p-3" key={v.id}>
                  <div className="mb-1 flex items-center gap-2">
                    <MlBadge tone="info">v{v.version}</MlBadge>
                    <span className="text-[11px] text-[#5b6270]">{new Date(v.createdAt).toLocaleString()}</span>
                  </div>
                  {v.notes && <p className="mb-1 text-[11px] text-[#8b93a1]">{v.notes}</p>}
                  <pre className="max-h-40 overflow-auto whitespace-pre-wrap rounded-[3px] bg-[#0b0d10] p-2 font-mono text-[11px] text-[#c4c9d1]">{v.systemPrompt}</pre>
                </div>
              ))}
            </div>
          )}
        </MlPanel>
      </div>
    </div>
  )
}
