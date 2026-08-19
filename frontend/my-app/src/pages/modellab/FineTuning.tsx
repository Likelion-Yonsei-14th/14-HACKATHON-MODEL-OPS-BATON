import { useEffect, useState } from 'react'
import { modelLabApi } from '../../api/modelLab/client'
import type { FineTuningJob, ModelLabTaskType } from '../../types/modelLab'
import { EmptyState, MlBadge, MlButton, MlInput, MlPanel, MlSelect, PageHeader, statusTone } from './ui'

export function FineTuning() {
  const [jobs, setJobs] = useState<FineTuningJob[]>([])
  const [taskType, setTaskType] = useState<ModelLabTaskType>('REPLY_CLASSIFICATION')
  const [baseModel, setBaseModel] = useState('gpt-4o-mini')
  const [message, setMessage] = useState<string | null>(null)

  function reload() {
    modelLabApi.listFineTuningJobs().then(setJobs)
  }
  useEffect(reload, [])

  async function createJob() {
    await modelLabApi.createFineTuningJob({ taskType, provider: 'OPENAI', baseModel })
    reload()
  }

  return (
    <div>
      <PageHeader description="Phase 5 (lowest priority per spec). Job records are real; actually submitting to OpenAI is not wired up yet." title="Fine-tuning" />
      <div className="space-y-4 p-6">
        <div className="rounded-[3px] border border-[#4a3d1a] bg-[#2a2210] px-4 py-3 text-[12px] text-[#facc15]">
          Fine-tuning execution is not yet implemented. You can create a job record to reserve metadata (task, base model, training dataset), but submitting it to OpenAI's Fine-tuning API is a stubbed
          TODO in the backend (FineTuningService.submit) — this screen will not pretend otherwise.
        </div>

        <MlPanel title="New job record">
          <div className="grid grid-cols-3 gap-3">
            <MlSelect onChange={(e) => setTaskType(e.target.value as ModelLabTaskType)} value={taskType}>
              <option value="REPLY_CLASSIFICATION">Classification</option>
              <option value="BRANCH_GENERATION">Generation</option>
            </MlSelect>
            <MlInput onChange={(e) => setBaseModel(e.target.value)} value={baseModel} />
            <MlButton onClick={createJob} variant="primary">
              Create job record
            </MlButton>
          </div>
        </MlPanel>

        <MlPanel title={`Jobs (${jobs.length})`}>
          {jobs.length === 0 ? (
            <EmptyState>No fine-tuning jobs yet.</EmptyState>
          ) : (
            <table className="w-full text-left text-[12px]">
              <thead>
                <tr className="border-b border-[#1f2328] text-[10px] uppercase text-[#5b6270]">
                  <th className="py-1.5">Task</th>
                  <th>Base model</th>
                  <th>Status</th>
                  <th>Fine-tuned model</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {jobs.map((j) => (
                  <tr className="border-b border-[#161a1f]" key={j.id}>
                    <td className="py-1.5">{j.taskType}</td>
                    <td>{j.baseModel}</td>
                    <td>
                      <MlBadge tone={statusTone(j.status)}>{j.status}</MlBadge>
                    </td>
                    <td>{j.fineTunedModelId ?? '—'}</td>
                    <td>
                      <button
                        className="text-[11px] text-[#5b8def] hover:underline"
                        onClick={async () => {
                          try {
                            await modelLabApi.submitFineTuningJob(j.id)
                          } catch {
                            setMessage('Fine-tuning execution not yet implemented — this call is expected to fail (backend MODELLAB-016).')
                          }
                        }}
                        type="button"
                      >
                        Submit
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {message && <p className="mt-2 text-[11px] text-[#facc15]">{message}</p>}
        </MlPanel>
      </div>
    </div>
  )
}
