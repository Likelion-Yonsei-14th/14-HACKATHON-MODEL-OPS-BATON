import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { modelLabApi } from '../../api/modelLab/client'
import type { ClassificationMetrics, EvalResult, EvalRun } from '../../types/modelLab'
import { EmptyState, MlBadge, MlPanel, PageHeader, formatPercent, statusTone } from './ui'

export function EvalRunsList() {
  const [runs, setRuns] = useState<EvalRun[]>([])
  const [compareIds, setCompareIds] = useState<string[]>([])

  useEffect(() => {
    modelLabApi.listClassificationRuns().then(setRuns)
  }, [])

  function toggleCompare(id: string) {
    setCompareIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : prev.length < 2 ? [...prev, id] : [prev[1], id]))
  }

  const compareRuns = runs.filter((r) => compareIds.includes(r.id))

  return (
    <div>
      <PageHeader description="모든 Classification Eval Run. 최대 2개 행을 선택해 나란히 비교할 수 있습니다." title="Eval Runs" />
      <div className="p-6">
        <MlPanel title={`Runs (${runs.length})`}>
          {runs.length === 0 ? (
            <EmptyState>아직 Eval Run이 없습니다.</EmptyState>
          ) : (
            <table className="w-full text-left text-[12px]">
              <thead>
                <tr className="border-b border-[#1f2328] text-[10px] uppercase text-[#5b6270]">
                  <th className="py-1.5"></th>
                  <th>Run</th>
                  <th>Split</th>
                  <th>상태</th>
                  <th>False Auto-Send</th>
                  <th>Accuracy</th>
                  <th>생성일</th>
                </tr>
              </thead>
              <tbody>
                {runs.map((r) => {
                  const m = r.aggregateMetrics as ClassificationMetrics | null
                  return (
                    <tr className="border-b border-[#161a1f]" key={r.id}>
                      <td>
                        <input checked={compareIds.includes(r.id)} onChange={() => toggleCompare(r.id)} type="checkbox" />
                      </td>
                      <td className="py-1.5">
                        <a className="text-[#5b8def] hover:underline" href={`/batons/models/eval-runs/${r.id}`}>
                          #{r.id}
                        </a>
                      </td>
                      <td>{r.split}</td>
                      <td>
                        <MlBadge tone={statusTone(r.status)}>{r.status}</MlBadge>
                      </td>
                      <td className={m && m.false_auto_send_rate > 0 ? 'text-[#f87171]' : ''}>{m ? formatPercent(m.false_auto_send_rate) : '—'}</td>
                      <td>{m ? formatPercent(m.branch_match_accuracy) : '—'}</td>
                      <td className="text-[#5b6270]">{new Date(r.createdAt).toLocaleString()}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
        </MlPanel>

        {compareRuns.length === 2 && (
          <MlPanel className="mt-4" title="비교">
            <div className="grid grid-cols-2 gap-4">
              {compareRuns.map((r) => {
                const m = r.aggregateMetrics as ClassificationMetrics | null
                return (
                  <div key={r.id}>
                    <div className="mb-2 text-[12px] font-medium text-white">
                      Run #{r.id} ({r.split})
                    </div>
                    {m ? (
                      <table className="w-full text-[11px] text-[#c4c9d1]">
                        <tbody>
                          <MetricRow label="False Auto-Send" value={formatPercent(m.false_auto_send_rate)} />
                          <MetricRow label="Auto-Send Coverage" value={formatPercent(m.auto_send_coverage)} />
                          <MetricRow label="Branch Accuracy" value={formatPercent(m.branch_match_accuracy)} />
                          <MetricRow label="Ambiguous Recall" value={formatPercent(m.ambiguous_detection_recall)} />
                          <MetricRow label="New Question Recall" value={formatPercent(m.new_question_detection_recall)} />
                          <MetricRow label="Out-of-Scope Recall" value={formatPercent(m.out_of_scope_detection_recall)} />
                          <MetricRow label="Avg Latency" value={`${Math.round(m.average_latency_ms)} ms`} />
                          <MetricRow label="Est. Cost" value={`$${m.estimated_cost_total.toFixed(4)}`} />
                        </tbody>
                      </table>
                    ) : (
                      <EmptyState>메트릭이 없습니다.</EmptyState>
                    )}
                  </div>
                )
              })}
            </div>
          </MlPanel>
        )}
      </div>
    </div>
  )
}

function MetricRow({ label, value }: { label: string; value: string }) {
  return (
    <tr className="border-b border-[#161a1f]">
      <td className="py-1 text-[#8b93a1]">{label}</td>
      <td className="py-1 text-right font-medium text-white">{value}</td>
    </tr>
  )
}

export function EvalRunDetail() {
  const { id } = useParams<{ id: string }>()
  const [params] = useSearchParams()
  const isGeneration = params.get('task') === 'BRANCH_GENERATION'
  const [run, setRun] = useState<EvalRun | null>(null)
  const [results, setResults] = useState<EvalResult[]>([])

  useEffect(() => {
    if (!id) return
    const getRun = isGeneration ? modelLabApi.getGenerationRun : modelLabApi.getClassificationRun
    const getResults = isGeneration ? modelLabApi.getGenerationResults : (runId: string) => modelLabApi.getClassificationResults(runId, false)
    getRun(id).then(setRun)
    getResults(id).then(setResults)
  }, [id, isGeneration])

  if (!run) return <EmptyState>불러오는 중…</EmptyState>

  return (
    <div>
      <PageHeader description={`${run.taskType} · dataset #${run.datasetId} · split ${run.split}`} title={`Run #${run.id}`} />
      <div className="space-y-4 p-6">
        <MlPanel title="Snapshot (실행 시점의 config/prompt — 이후 config가 수정되어도 절대 바뀌지 않음)">
          <pre className="max-h-64 overflow-auto whitespace-pre-wrap rounded-[3px] bg-[#0b0d10] p-2 font-mono text-[11px] text-[#c4c9d1]">{JSON.stringify(run.modelSnapshot, null, 2)}</pre>
        </MlPanel>
        <MlPanel title="종합 메트릭">
          <pre className="whitespace-pre-wrap font-mono text-[11px] text-[#c4c9d1]">{JSON.stringify(run.aggregateMetrics, null, 2)}</pre>
        </MlPanel>
        <MlPanel title={`Results (${results.length})`}>
          {results.length === 0 ? (
            <EmptyState>결과가 없습니다.</EmptyState>
          ) : (
            <ul className="space-y-1 text-[11px] text-[#8b93a1]">
              {results.map((r) => (
                <li key={r.id}>
                  #{r.id} · passed={String(r.passed)} {r.errorMessage ? `· error: ${r.errorMessage}` : ''}
                </li>
              ))}
            </ul>
          )}
        </MlPanel>
      </div>
    </div>
  )
}
