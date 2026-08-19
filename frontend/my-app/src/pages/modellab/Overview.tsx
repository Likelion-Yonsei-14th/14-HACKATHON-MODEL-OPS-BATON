import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { modelLabApi } from '../../api/modelLab/client'
import type { ClassificationMetrics, ModelLabOverview } from '../../types/modelLab'
import { EmptyState, MetricTile, MlBadge, MlPanel, PageHeader, formatPercent, statusTone } from './ui'

export function Overview() {
  const [data, setData] = useState<ModelLabOverview | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    modelLabApi.getOverview().then(setData).catch((e) => setError(String(e.message ?? e)))
  }, [])

  const clsMetrics = data?.recentEvalRuns.find((r) => r.taskType === 'REPLY_CLASSIFICATION' && r.status === 'COMPLETED')?.aggregateMetrics as ClassificationMetrics | undefined

  return (
    <div>
      <PageHeader description="현재 Production 설정, 최근 활동, Classification 품질 지표 요약." title="Overview" />
      <div className="space-y-6 p-6">
        {error && <EmptyState>Overview를 불러오지 못했습니다: {error}</EmptyState>}

        <div className="grid grid-cols-2 gap-4">
          <MlPanel title="Production · Classification">
            {data?.productionClassificationConfig ? (
              <div>
                <div className="text-[13px] font-medium text-white">{data.productionClassificationConfig.name}</div>
                <div className="mt-1 text-[12px] text-[#8b93a1]">
                  {data.productionClassificationConfig.baseModel} · threshold {data.productionClassificationConfig.confidenceThreshold ?? '—'}
                </div>
              </div>
            ) : (
              <EmptyState>아직 Production Config가 없습니다. Deployment에서 승격하세요.</EmptyState>
            )}
          </MlPanel>
          <MlPanel title="Production · Generation">
            {data?.productionGenerationConfig ? (
              <div>
                <div className="text-[13px] font-medium text-white">{data.productionGenerationConfig.name}</div>
                <div className="mt-1 text-[12px] text-[#8b93a1]">{data.productionGenerationConfig.baseModel}</div>
              </div>
            ) : (
              <EmptyState>아직 Production Config가 없습니다. Deployment에서 승격하세요.</EmptyState>
            )}
          </MlPanel>
        </div>

        <MlPanel title="Classification 품질 (가장 최근 완료된 Run)">
          {clsMetrics ? (
            <div className="grid grid-cols-5 gap-3">
              <MetricTile label="False Auto-Send" tone={clsMetrics.false_auto_send_rate > 0 ? 'bad' : 'good'} value={formatPercent(clsMetrics.false_auto_send_rate)} />
              <MetricTile label="Auto-Send Coverage" value={formatPercent(clsMetrics.auto_send_coverage)} />
              <MetricTile label="Branch Accuracy" value={formatPercent(clsMetrics.branch_match_accuracy)} />
              <MetricTile label="Ambiguous Recall" value={formatPercent(clsMetrics.ambiguous_detection_recall)} />
              <MetricTile label="New Question Recall" value={formatPercent(clsMetrics.new_question_detection_recall)} />
            </div>
          ) : (
            <EmptyState>완료된 Classification Eval Run이 아직 없습니다.</EmptyState>
          )}
        </MlPanel>

        <div className="grid grid-cols-3 gap-4">
          <MlPanel title="최근 Eval Run">
            {data?.recentEvalRuns.length ? (
              <ul className="space-y-2">
                {data.recentEvalRuns.slice(0, 6).map((r) => (
                  <li className="flex items-center justify-between text-[12px]" key={r.id}>
                    <Link className="text-[#5b8def] hover:underline" to={`/batons/models/eval-runs/${r.id}?task=${r.taskType}`}>
                      {r.taskType === 'REPLY_CLASSIFICATION' ? 'CLS' : 'GEN'} #{r.id} · {r.split}
                    </Link>
                    <MlBadge tone={statusTone(r.status)}>{r.status}</MlBadge>
                  </li>
                ))}
              </ul>
            ) : (
              <EmptyState>아직 Eval Run이 없습니다.</EmptyState>
            )}
          </MlPanel>
          <MlPanel title="최근 Fine-tuning Job">
            {data?.recentFineTuningJobs.length ? (
              <ul className="space-y-2">
                {data.recentFineTuningJobs.map((j) => (
                  <li className="flex items-center justify-between text-[12px]" key={j.id}>
                    <span>
                      {j.taskType} · {j.baseModel}
                    </span>
                    <MlBadge tone={statusTone(j.status)}>{j.status}</MlBadge>
                  </li>
                ))}
              </ul>
            ) : (
              <EmptyState>아직 Fine-tuning Job이 없습니다.</EmptyState>
            )}
          </MlPanel>
          <MlPanel title="최근 Deployment">
            {data?.recentDeployments.length ? (
              <ul className="space-y-2">
                {data.recentDeployments.map((d) => (
                  <li className="text-[12px]" key={d.id}>
                    <MlBadge tone={d.action === 'PROMOTE' ? 'good' : 'warn'}>{d.action}</MlBadge>{' '}
                    <span className="text-[#8b93a1]">
                      {d.taskType} → config #{d.toConfigId}
                    </span>
                  </li>
                ))}
              </ul>
            ) : (
              <EmptyState>아직 Deployment 이력이 없습니다.</EmptyState>
            )}
          </MlPanel>
        </div>
      </div>
    </div>
  )
}
