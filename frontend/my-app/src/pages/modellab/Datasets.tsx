import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { modelLabApi } from '../../api/modelLab/client'
import type { EvalDataset, EvalReplyCase, EvalScenario, ModelLabTaskType } from '../../types/modelLab'
import { EmptyState, MlBadge, MlButton, MlInput, MlPanel, PageHeader } from './ui'

export function Datasets() {
  const [params, setParams] = useSearchParams()
  const taskType = (params.get('task') as ModelLabTaskType) ?? 'REPLY_CLASSIFICATION'
  const datasetId = params.get('dataset')
  const scenarioId = params.get('scenario')

  const [datasets, setDatasets] = useState<EvalDataset[]>([])
  const [scenarios, setScenarios] = useState<EvalScenario[]>([])
  const [replyCases, setReplyCases] = useState<EvalReplyCase[]>([])
  const [newDatasetName, setNewDatasetName] = useState('')

  useEffect(() => {
    modelLabApi.listDatasets(taskType).then(setDatasets)
  }, [taskType])

  useEffect(() => {
    if (datasetId) modelLabApi.listScenarios(datasetId).then(setScenarios)
    else setScenarios([])
  }, [datasetId])

  useEffect(() => {
    if (scenarioId) modelLabApi.listReplyCases(scenarioId).then(setReplyCases)
    else setReplyCases([])
  }, [scenarioId])

  const selectedScenario = scenarios.find((s) => s.id === scenarioId)

  async function createDataset() {
    if (!newDatasetName.trim()) return
    const d = await modelLabApi.createDataset(newDatasetName, taskType)
    setNewDatasetName('')
    setDatasets((prev) => [d, ...prev])
  }

  return (
    <div>
      <PageHeader description="Scenario/reply-case fixtures. Split (SMOKE/CORE/HOLDOUT) controls which eval runs use a given case (spec section 12)." title="Datasets" />
      <div className="grid grid-cols-[220px_260px_1fr] gap-0 divide-x divide-[#1f2328] border-t border-[#1f2328]">
        <div className="p-4">
          <div className="mb-3 flex gap-1">
            {(['REPLY_CLASSIFICATION', 'BRANCH_GENERATION'] as ModelLabTaskType[]).map((t) => (
              <button
                className={`rounded-[3px] border px-2 py-1 text-[11px] ${t === taskType ? 'border-[#5b8def] bg-[#101d2e] text-[#5b8def]' : 'border-[#2a2f37] text-[#9aa4b2]'}`}
                key={t}
                onClick={() => setParams({ task: t })}
                type="button"
              >
                {t === 'REPLY_CLASSIFICATION' ? 'CLS' : 'GEN'}
              </button>
            ))}
          </div>
          <ul className="space-y-1">
            {datasets.map((d) => (
              <li key={d.id}>
                <button
                  className={`w-full rounded-[3px] px-2 py-1.5 text-left text-[12px] ${d.id === datasetId ? 'bg-[#151a20] text-white' : 'text-[#9aa4b2] hover:bg-[#12151a]'}`}
                  onClick={() => setParams({ task: taskType, dataset: d.id })}
                  type="button"
                >
                  {d.name} <span className="text-[10px] text-[#5b6270]">v{d.version}</span>
                </button>
              </li>
            ))}
          </ul>
          <div className="mt-4 flex gap-1">
            <MlInput onChange={(e) => setNewDatasetName(e.target.value)} placeholder="New dataset name" value={newDatasetName} />
            <MlButton onClick={createDataset}>+</MlButton>
          </div>
        </div>

        <div className="p-4">
          {!datasetId ? (
            <EmptyState>Select a dataset.</EmptyState>
          ) : scenarios.length === 0 ? (
            <EmptyState>No scenarios yet.</EmptyState>
          ) : (
            <ul className="space-y-1">
              {scenarios.map((s) => (
                <li key={s.id}>
                  <button
                    className={`w-full rounded-[3px] px-2 py-1.5 text-left text-[12px] ${s.id === scenarioId ? 'bg-[#151a20] text-white' : 'text-[#9aa4b2] hover:bg-[#12151a]'}`}
                    onClick={() => setParams({ task: taskType, dataset: datasetId, scenario: s.id })}
                    type="button"
                  >
                    <div className="flex items-center justify-between">
                      <span>{s.externalKey}</span>
                      <MlBadge tone={s.split === 'HOLDOUT' ? 'warn' : 'neutral'}>{s.split}</MlBadge>
                    </div>
                    <div className="truncate text-[11px] text-[#5b6270]">{s.title}</div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="p-4">
          {!selectedScenario ? (
            <EmptyState>Select a scenario to view its question, golden branches, and reply cases.</EmptyState>
          ) : (
            <ScenarioDetail replyCases={replyCases} scenario={selectedScenario} />
          )}
        </div>
      </div>
    </div>
  )
}

function ScenarioDetail({ scenario, replyCases }: { scenario: EvalScenario; replyCases: EvalReplyCase[] }) {
  return (
    <div className="space-y-4">
      <div>
        <div className="mb-1 flex items-center gap-2">
          <h2 className="text-[13px] font-semibold text-white">{scenario.title}</h2>
          <MlBadge>{scenario.split}</MlBadge>
        </div>
        <p className="text-[12px] text-[#c4c9d1]">{scenario.question}</p>
      </div>

      {scenario.goldenBranches && scenario.goldenBranches.length > 0 && (
        <MlPanel title={`Golden branches (${scenario.goldenBranches.length})`}>
          <div className="space-y-2">
            {scenario.goldenBranches.map((b, i) => (
              <div className="rounded-[3px] border border-[#1f2328] p-2 text-[11px]" key={i}>
                <div className="font-medium text-white">
                  #{b.id ?? b.key} · {b.name}
                </div>
                <div className="text-[#8b93a1]">condition: {b.condition_text}</div>
                <div className="text-[#8b93a1]">decision: {b.decision_text}</div>
                <div className="text-[#8b93a1]">response: {b.response_text}</div>
              </div>
            ))}
          </div>
        </MlPanel>
      )}

      <MlPanel title={`Reply cases (${replyCases.length})`}>
        {replyCases.length === 0 ? (
          <EmptyState>No reply cases for this scenario.</EmptyState>
        ) : (
          <div className="space-y-3">
            {replyCases.map((c) => (
              <div className="rounded-[3px] border border-[#1f2328] p-3" key={c.id}>
                {/* Multi-message replies rendered as a visually distinct sequence (spec section 7/24). */}
                <div className="mb-2 space-y-1">
                  {c.replyMessages.map((m, i) => (
                    <div className="rounded-[3px] bg-[#0b0d10] px-2 py-1 text-[12px] text-[#d4d8dd]" key={i}>
                      {m}
                    </div>
                  ))}
                </div>
                <div className="flex flex-wrap gap-1 text-[10px]">
                  <MlBadge tone={c.expectedBranchKey ? 'good' : 'neutral'}>expected branch: {c.expectedBranchKey ?? 'none'}</MlBadge>
                  {c.expectedAmbiguous && <MlBadge tone="warn">AMBIGUOUS</MlBadge>}
                  {c.expectedNewQuestion && <MlBadge tone="warn">NEW_QUESTION</MlBadge>}
                  {c.expectedOutOfScope && <MlBadge tone="warn">OUT_OF_SCOPE</MlBadge>}
                  {c.expectedNoMatch && <MlBadge tone="bad">NO_MATCH</MlBadge>}
                  {c.tags?.map((t) => (
                    <MlBadge key={t}>{t}</MlBadge>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </MlPanel>
    </div>
  )
}
