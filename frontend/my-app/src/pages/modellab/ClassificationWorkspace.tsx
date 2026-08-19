import { useEffect, useState } from 'react'
import { modelLabApi } from '../../api/modelLab/client'
import type { DatasetSplit, EvalDataset, EvalResult, EvalRun, ModelConfig, PromptVersion } from '../../types/modelLab'
import { EmptyState, MetricTile, MlBadge, MlButton, MlInput, MlPanel, MlSelect, PageHeader, formatPercent, statusTone } from './ui'

const SPLITS: DatasetSplit[] = ['SMOKE', 'CORE', 'HOLDOUT']

export function ClassificationWorkspace() {
  const [configs, setConfigs] = useState<ModelConfig[]>([])
  const [promptVersions, setPromptVersions] = useState<PromptVersion[]>([])
  const [datasets, setDatasets] = useState<EvalDataset[]>([])
  const [configId, setConfigId] = useState<string>('')
  const [datasetId, setDatasetId] = useState<string>('')
  const [split, setSplit] = useState<DatasetSplit>('SMOKE')
  const [previewCount, setPreviewCount] = useState<number | null>(null)
  const [running, setRunning] = useState(false)
  const [lastRun, setLastRun] = useState<EvalRun | null>(null)
  const [failedCases, setFailedCases] = useState<EvalResult[]>([])
  const [selectedCase, setSelectedCase] = useState<EvalResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [showNewConfig, setShowNewConfig] = useState(false)

  useEffect(() => {
    modelLabApi.listModelConfigs('REPLY_CLASSIFICATION').then((cs) => {
      setConfigs(cs)
      if (cs.length && !configId) setConfigId(cs[0].id)
    })
    modelLabApi.listPromptVersions('REPLY_CLASSIFICATION').then(setPromptVersions)
    modelLabApi.listDatasets('REPLY_CLASSIFICATION').then((ds) => {
      setDatasets(ds)
      if (ds.length && !datasetId) setDatasetId(ds[0].id)
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (datasetId && split) {
      modelLabApi.previewClassificationRun(datasetId, split).then(setPreviewCount).catch(() => setPreviewCount(null))
    }
  }, [datasetId, split])

  const selectedConfig = configs.find((c) => c.id === configId) ?? null

  async function runEval() {
    if (!configId || !datasetId) return
    setRunning(true)
    setError(null)
    try {
      const run = await modelLabApi.runClassificationEval(datasetId, split, configId)
      setLastRun(run)
      const failed = await modelLabApi.getClassificationResults(run.id, true)
      setFailedCases(failed)
    } catch (e) {
      setError(String((e as Error).message ?? e))
    } finally {
      setRunning(false)
    }
  }

  const metrics = lastRun?.aggregateMetrics as Record<string, number> | null

  return (
    <div>
      <PageHeader
        action={
          <div className="flex gap-2">
            <MlButton onClick={() => setShowNewConfig((s) => !s)}>{showNewConfig ? '취소' : '+ 새 Config'}</MlButton>
          </div>
        }
        description="Config 기반 Classification eval 루프: prompt/model/threshold 편집기, Eval Runner, 메트릭, Failed Cases."
        title="Classification"
      />

      <div className="space-y-6 p-6">
        {showNewConfig && (
          <NewConfigForm
            onCreated={(c) => {
              setConfigs((prev) => [c, ...prev])
              setConfigId(c.id)
              setShowNewConfig(false)
            }}
            promptVersions={promptVersions}
          />
        )}

        <div className="grid grid-cols-3 gap-4">
          <MlPanel title="Config">
            <MlSelect onChange={(e) => setConfigId(e.target.value)} value={configId}>
              {configs.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.status})
                </option>
              ))}
            </MlSelect>
            {selectedConfig && (
              <div className="mt-3 space-y-1 text-[11px] text-[#8b93a1]">
                <div>
                  Status: <MlBadge tone={statusTone(selectedConfig.status)}>{selectedConfig.status}</MlBadge>
                </div>
                <div>Model: {selectedConfig.baseModel}</div>
                <div>Prompt Version ID: {selectedConfig.promptVersionId}</div>
                <div>Temperature: {selectedConfig.temperature}</div>
                <div>Threshold: {selectedConfig.confidenceThreshold ?? '—'}</div>
              </div>
            )}
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
                <button
                  className={`rounded-[3px] border px-2 py-1 text-[11px] ${s === split ? 'border-[#5b8def] bg-[#101d2e] text-[#5b8def]' : 'border-[#2a2f37] text-[#9aa4b2]'}`}
                  key={s}
                  onClick={() => setSplit(s)}
                  type="button"
                >
                  {s}
                </button>
              ))}
            </div>
          </MlPanel>

          <MlPanel title="Run">
            {/* Cost control: expected case count shown before executing (spec section 37). */}
            <p className="text-[12px] text-[#8b93a1]">
              이 Run은 OpenAI를 <span className="font-semibold text-white">{previewCount ?? '—'}</span>회 호출합니다.
            </p>
            <MlButton className="mt-3" disabled={running || !previewCount} onClick={runEval} variant="primary">
              {running ? '실행 중…' : `${split} 실행`}
            </MlButton>
            {error && <p className="mt-2 text-[11px] text-[#f87171]">{error}</p>}
          </MlPanel>
        </div>

        <MlPanel title="최근 Run 메트릭">
          {!metrics ? (
            <EmptyState>Eval을 실행하면 메트릭이 여기 표시됩니다.</EmptyState>
          ) : (
            <div className="grid grid-cols-6 gap-3">
              <MetricTile hint="단독 confidence가 아닌 조건들의 결합" label="False Auto-Send" tone={metrics.false_auto_send_rate > 0 ? 'bad' : 'good'} value={formatPercent(metrics.false_auto_send_rate)} />
              <MetricTile label="Auto-Send Coverage" value={formatPercent(metrics.auto_send_coverage)} />
              <MetricTile label="Branch Accuracy" value={formatPercent(metrics.branch_match_accuracy)} />
              <MetricTile label="Ambiguous Recall" value={formatPercent(metrics.ambiguous_detection_recall)} />
              <MetricTile label="New Question Recall" value={formatPercent(metrics.new_question_detection_recall)} />
              <MetricTile label="Out-of-Scope Recall" value={formatPercent(metrics.out_of_scope_detection_recall)} />
              <MetricTile label="No Match Recall" value={formatPercent(metrics.no_match_detection_recall)} />
              <MetricTile label="Schema Validity" value={formatPercent(metrics.schema_validity)} />
              <MetricTile label="Avg Latency" value={`${Math.round(metrics.average_latency_ms ?? 0)} ms`} />
              <MetricTile label="Avg Input Tokens" value={String(Math.round(metrics.average_input_tokens ?? 0))} />
              <MetricTile label="Avg Output Tokens" value={String(Math.round(metrics.average_output_tokens ?? 0))} />
              <MetricTile label="Est. Cost" value={`$${(metrics.estimated_cost_total ?? 0).toFixed(4)}`} />
            </div>
          )}
        </MlPanel>

        <MlPanel title={`Failed Cases (${failedCases.length})`}>
          {failedCases.length === 0 ? (
            <EmptyState>최근 Run에서 실패한 case가 없습니다 (또는 아직 Run이 없습니다).</EmptyState>
          ) : (
            <table className="w-full text-left text-[12px]">
              <thead>
                <tr className="border-b border-[#1f2328] text-[10px] uppercase text-[#5b6270]">
                  <th className="py-1.5">Case</th>
                  <th>Auto-send 기대값</th>
                  <th>Auto-send 실제값</th>
                  <th>Latency</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {failedCases.map((c) => (
                  <tr className="cursor-pointer border-b border-[#161a1f] hover:bg-[#12151a]" key={c.id} onClick={() => setSelectedCase(c)}>
                    <td className="py-1.5">#{c.id}</td>
                    <td>
                      <MlBadge tone={c.autoSendExpected ? 'good' : 'neutral'}>{String(c.autoSendExpected)}</MlBadge>
                    </td>
                    <td>
                      <MlBadge tone={c.autoSendActual && !c.autoSendExpected ? 'bad' : 'neutral'}>{String(c.autoSendActual)}</MlBadge>
                    </td>
                    <td>{c.latencyMs ?? '—'} ms</td>
                    <td className="text-[#5b8def]">보기 →</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </MlPanel>
      </div>

      {selectedCase && <FailedCaseDrawer onClose={() => setSelectedCase(null)} result={selectedCase} />}
    </div>
  )
}

function NewConfigForm({ promptVersions, onCreated }: { promptVersions: PromptVersion[]; onCreated: (c: ModelConfig) => void }) {
  const [name, setName] = useState('')
  const [baseModel, setBaseModel] = useState('gpt-4o-mini')
  const [promptVersionId, setPromptVersionId] = useState(promptVersions[0]?.id ?? '')
  const [temperature, setTemperature] = useState('0.2')
  const [threshold, setThreshold] = useState('0.7')
  const [busy, setBusy] = useState(false)

  return (
    <MlPanel title="새 Classification Config">
      <div className="grid grid-cols-5 gap-3">
        <div>
          <label className="mb-1 block text-[11px] text-[#8b93a1]">이름</label>
          <MlInput onChange={(e) => setName(e.target.value)} placeholder="CLS-v2" value={name} />
        </div>
        <div>
          <label className="mb-1 block text-[11px] text-[#8b93a1]">Base Model</label>
          <MlInput onChange={(e) => setBaseModel(e.target.value)} value={baseModel} />
        </div>
        <div>
          <label className="mb-1 block text-[11px] text-[#8b93a1]">Prompt Version</label>
          <MlSelect onChange={(e) => setPromptVersionId(e.target.value)} value={promptVersionId}>
            {promptVersions.map((v) => (
              <option key={v.id} value={v.id}>
                v{v.version}
              </option>
            ))}
          </MlSelect>
        </div>
        <div>
          <label className="mb-1 block text-[11px] text-[#8b93a1]">Temperature</label>
          <MlInput onChange={(e) => setTemperature(e.target.value)} value={temperature} />
        </div>
        <div>
          <label className="mb-1 block text-[11px] text-[#8b93a1]">Threshold</label>
          <MlInput onChange={(e) => setThreshold(e.target.value)} value={threshold} />
        </div>
      </div>
      <MlButton
        className="mt-3"
        disabled={busy || !name.trim() || !promptVersionId}
        onClick={async () => {
          setBusy(true)
          try {
            const c = await modelLabApi.createModelConfig({
              name,
              taskType: 'REPLY_CLASSIFICATION',
              provider: 'OPENAI',
              baseModel,
              promptVersionId,
              temperature: Number(temperature),
              confidenceThreshold: Number(threshold),
            })
            onCreated(c)
          } finally {
            setBusy(false)
          }
        }}
        variant="primary"
      >
        생성
      </MlButton>
    </MlPanel>
  )
}

function FailedCaseDrawer({ result, onClose }: { result: EvalResult; onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/60">
      <div className="h-full w-full max-w-lg overflow-y-auto border-l border-[#2a2f37] bg-[#0d0f12] p-5">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-[14px] font-semibold text-white">Failed Case #{result.id}</h2>
          <button className="text-[#8b93a1] hover:text-white" onClick={onClose} type="button">
            ✕
          </button>
        </div>
        <div className="space-y-4 text-[12px]">
          <Field label="Input Snapshot" value={result.inputSnapshot} />
          <Field label="Expected" value={result.expected} />
          <Field label="Actual" value={result.actual} />
          <div className="grid grid-cols-2 gap-2">
            <MetricTile label="Auto-send 기대값" value={String(result.autoSendExpected)} />
            <MetricTile label="Auto-send 실제값" tone={result.autoSendActual && !result.autoSendExpected ? 'bad' : 'neutral'} value={String(result.autoSendActual)} />
            <MetricTile label="Latency" value={`${result.latencyMs ?? '—'} ms`} />
            <MetricTile label="Tokens (in/out)" value={`${result.inputTokens ?? '—'} / ${result.outputTokens ?? '—'}`} />
          </div>
          {result.errorMessage && (
            <div>
              <div className="mb-1 text-[10px] uppercase text-[#5b6270]">Error</div>
              <p className="text-[#f87171]">{result.errorMessage}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function Field({ label, value }: { label: string; value: unknown }) {
  return (
    <div>
      <div className="mb-1 text-[10px] uppercase text-[#5b6270]">{label}</div>
      <pre className="max-h-48 overflow-auto whitespace-pre-wrap rounded-[3px] border border-[#1f2328] bg-[#0b0d10] p-2 font-mono text-[11px] text-[#c4c9d1]">{JSON.stringify(value, null, 2)}</pre>
    </div>
  )
}
