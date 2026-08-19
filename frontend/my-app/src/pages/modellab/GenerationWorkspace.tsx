import { useEffect, useState } from 'react'
import { modelLabApi } from '../../api/modelLab/client'
import type { DatasetSplit, EvalDataset, EvalResult, EvalRun, GenerationMetrics, ModelConfig } from '../../types/modelLab'
import { EmptyState, MetricTile, MlBadge, MlButton, MlPanel, MlSelect, PageHeader, formatPercent } from './ui'

const SPLITS: DatasetSplit[] = ['SMOKE', 'CORE', 'HOLDOUT']
const SCORE_FIELDS = [
  ['coverageScore', 'Coverage'],
  ['separationScore', 'Separation'],
  ['granularityScore', 'Granularity'],
  ['predecidabilityScore', 'Pre-decidability'],
  ['naturalnessScore', 'Naturalness'],
  ['safetyScore', 'Safety'],
  ['overallScore', 'Overall'],
] as const

export function GenerationWorkspace() {
  const [configs, setConfigs] = useState<ModelConfig[]>([])
  const [datasets, setDatasets] = useState<EvalDataset[]>([])
  const [configId, setConfigId] = useState('')
  const [datasetId, setDatasetId] = useState('')
  const [split, setSplit] = useState<DatasetSplit>('SMOKE')
  const [running, setRunning] = useState(false)
  const [run, setRun] = useState<EvalRun | null>(null)
  const [results, setResults] = useState<EvalResult[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    modelLabApi.listModelConfigs('BRANCH_GENERATION').then((cs) => {
      setConfigs(cs)
      if (cs.length) setConfigId(cs[0].id)
    })
    modelLabApi.listDatasets('BRANCH_GENERATION').then((ds) => {
      setDatasets(ds)
      if (ds.length) setDatasetId(ds[0].id)
    })
  }, [])

  async function runEval() {
    if (!configId || !datasetId) return
    setRunning(true)
    setError(null)
    try {
      const r = await modelLabApi.runGenerationEval(datasetId, split, configId)
      setRun(r)
      setResults(await modelLabApi.getGenerationResults(r.id))
    } catch (e) {
      setError(String((e as Error).message ?? e))
    } finally {
      setRunning(false)
    }
  }

  const metrics = run?.aggregateMetrics as GenerationMetrics | null

  return (
    <div>
      <PageHeader description="Human-review-first Generation eval (spec section 4.3): hard rule은 자동으로 실행되고, 품질 평가는 LLM judge가 아닌 사람이 직접 점수를 매깁니다." title="Generation" />
      <div className="space-y-6 p-6">
        <div className="grid grid-cols-3 gap-4">
          <MlPanel title="Config">
            <MlSelect onChange={(e) => setConfigId(e.target.value)} value={configId}>
              {configs.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.status})
                </option>
              ))}
            </MlSelect>
          </MlPanel>
          <MlPanel title="Dataset & Split">
            <MlSelect onChange={(e) => setDatasetId(e.target.value)} value={datasetId}>
              {datasets.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </MlSelect>
            <div className="mt-2 flex gap-1">
              {SPLITS.map((s) => (
                <button className={`rounded-[3px] border px-2 py-1 text-[11px] ${s === split ? 'border-[#5b8def] bg-[#101d2e] text-[#5b8def]' : 'border-[#2a2f37] text-[#9aa4b2]'}`} key={s} onClick={() => setSplit(s)} type="button">
                  {s}
                </button>
              ))}
            </div>
          </MlPanel>
          <MlPanel title="Run">
            <MlButton disabled={running || !configId || !datasetId} onClick={runEval} variant="primary">
              {running ? '실행 중…' : `${split} 생성`}
            </MlButton>
            {error && <p className="mt-2 text-[11px] text-[#f87171]">{error}</p>}
          </MlPanel>
        </div>

        {metrics && (
          <MlPanel title="Hard-rule Check (자동 검사, 품질 점수 아님)">
            <div className="grid grid-cols-2 gap-3">
              <MetricTile label="Scenarios" value={String(metrics.total_scenarios)} />
              <MetricTile label="Hard-rule Pass Rate" tone={metrics.hard_rule_pass_rate < 1 ? 'warn' : 'good'} value={formatPercent(metrics.hard_rule_pass_rate)} />
            </div>
          </MlPanel>
        )}

        <MlPanel title={`생성된 Branch (${results.length})`}>
          {results.length === 0 ? (
            <EmptyState>Generation을 실행하면 결과가 여기 표시됩니다.</EmptyState>
          ) : (
            <div className="space-y-4">
              {results.map((r) => (
                <GenerationResultRow key={r.id} result={r} />
              ))}
            </div>
          )}
        </MlPanel>
      </div>
    </div>
  )
}

function GenerationResultRow({ result }: { result: EvalResult }) {
  const branches = (result.actual as { branches?: { name: string; condition_text: string; decision_text: string; response_text: string }[] } | null)?.branches ?? []
  const [scores, setScores] = useState<Record<string, string>>({})
  const [note, setNote] = useState('')
  const [saved, setSaved] = useState(false);

  async function save() {
    await modelLabApi.submitGenerationHumanReview(result.id, {
      coverageScore: scores.coverageScore ? Number(scores.coverageScore) : undefined,
      separationScore: scores.separationScore ? Number(scores.separationScore) : undefined,
      granularityScore: scores.granularityScore ? Number(scores.granularityScore) : undefined,
      predecidabilityScore: scores.predecidabilityScore ? Number(scores.predecidabilityScore) : undefined,
      naturalnessScore: scores.naturalnessScore ? Number(scores.naturalnessScore) : undefined,
      safetyScore: scores.safetyScore ? Number(scores.safetyScore) : undefined,
      overallScore: scores.overallScore ? Number(scores.overallScore) : undefined,
      note: note || undefined,
    })
    setSaved(true)
  }

  return (
    <div className="grid grid-cols-2 gap-4 rounded-[3px] border border-[#1f2328] p-3">
      <div>
        <div className="mb-2 flex items-center gap-2">
          <MlBadge tone={result.passed ? 'good' : 'bad'}>{result.passed ? 'Hard Rule 통과' : 'Hard Rule 실패'}</MlBadge>
          {result.errorMessage && <span className="text-[11px] text-[#f87171]">{result.errorMessage}</span>}
        </div>
        {branches.length === 0 ? (
          <p className="text-[11px] text-[#5b6270]">파싱된 branch가 없습니다.</p>
        ) : (
          <div className="space-y-2">
            {branches.map((b, i) => (
              <div className="rounded-[3px] bg-[#0b0d10] p-2 text-[11px]" key={i}>
                <div className="font-medium text-white">{b.name}</div>
                <div className="text-[#8b93a1]">condition: {b.condition_text}</div>
                <div className="text-[#8b93a1]">decision: {b.decision_text}</div>
                <div className="text-[#8b93a1]">response: {b.response_text}</div>
              </div>
            ))}
          </div>
        )}
      </div>
      <div>
        <div className="mb-1 text-[10px] uppercase text-[#5b6270]">Human Review</div>
        <div className="grid grid-cols-4 gap-1.5">
          {SCORE_FIELDS.map(([key, label]) => (
            <label className="text-[10px] text-[#8b93a1]" key={key}>
              {label}
              <input
                className="mt-0.5 w-full rounded-[3px] border border-[#2a2f37] bg-[#0b0d10] px-1 py-0.5 text-[11px] text-[#d4d8dd]"
                max={5}
                min={1}
                onChange={(e) => setScores((s) => ({ ...s, [key]: e.target.value }))}
                type="number"
                value={scores[key] ?? ''}
              />
            </label>
          ))}
        </div>
        <textarea
          className="mt-2 w-full rounded-[3px] border border-[#2a2f37] bg-[#0b0d10] px-2 py-1 text-[11px] text-[#d4d8dd]"
          onChange={(e) => setNote(e.target.value)}
          placeholder="리뷰어 메모"
          rows={2}
          value={note}
        />
        <MlButton className="mt-2" onClick={save}>
          {saved ? '저장됨 ✓' : '리뷰 저장'}
        </MlButton>
      </div>
    </div>
  )
}
