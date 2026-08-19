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
      <PageHeader description="Phase 5 (spec 기준 최저 우선순위). Job Record는 실제로 저장되지만, OpenAI로의 실제 제출은 아직 연결되어 있지 않습니다." title="Fine-tuning" />
      <div className="space-y-4 p-6">
        <div className="rounded-[3px] border border-[#4a3d1a] bg-[#2a2210] px-4 py-3 text-[12px] text-[#facc15]">
          Fine-tuning 실행은 아직 구현되지 않았습니다. 메타데이터(작업, base model, training dataset)를 예약해두기 위한 Job Record는 생성할 수 있지만, OpenAI의 Fine-tuning API로 제출하는 부분은
          백엔드(FineTuningService.submit)에 아직 TODO로 남아있는 스텁입니다 — 이 화면은 그 사실을 숨기지 않습니다.
        </div>

        <MlPanel title="새 Job Record">
          <div className="grid grid-cols-3 gap-3">
            <MlSelect onChange={(e) => setTaskType(e.target.value as ModelLabTaskType)} value={taskType}>
              <option value="REPLY_CLASSIFICATION">Classification</option>
              <option value="BRANCH_GENERATION">Generation</option>
            </MlSelect>
            <MlInput onChange={(e) => setBaseModel(e.target.value)} value={baseModel} />
            <MlButton onClick={createJob} variant="primary">
              Job Record 생성
            </MlButton>
          </div>
        </MlPanel>

        <MlPanel title={`Jobs (${jobs.length})`}>
          {jobs.length === 0 ? (
            <EmptyState>아직 Fine-tuning Job이 없습니다.</EmptyState>
          ) : (
            <table className="w-full text-left text-[12px]">
              <thead>
                <tr className="border-b border-[#1f2328] text-[10px] uppercase text-[#5b6270]">
                  <th className="py-1.5">Task</th>
                  <th>Base Model</th>
                  <th>상태</th>
                  <th>Fine-tuned Model</th>
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
                            setMessage('Fine-tuning 실행이 아직 구현되지 않았습니다 — 이 호출은 실패하는 것이 정상입니다 (backend MODELLAB-016).')
                          }
                        }}
                        type="button"
                      >
                        제출
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
