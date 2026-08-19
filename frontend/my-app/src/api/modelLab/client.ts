// BATON Model Lab API client. Talks to /api/model-lab/** on the same backend as the rest of the
// app (Authorization: Bearer <api_key>, admin-gated server-side) — reuses the existing `request()`
// wrapper from src/api/http/request.ts so auth/error handling stays in one place. Unlike the rest
// of the app there is no mock implementation: Model Lab is an internal tool that only makes sense
// against a real backend + real admin account, so it is not part of the demo-without-a-backend path.
import { request } from '../http/request'
import type {
  ClassificationMetrics,
  DatasetSplit,
  DeploymentAction,
  DeploymentHistoryEntry,
  EvalDataset,
  EvalReplyCase,
  EvalResult,
  EvalRun,
  EvalScenario,
  FineTuningJob,
  GenerationHumanReview,
  GoldenBranch,
  ModelConfig,
  ModelConfigStatus,
  ModelLabOverview,
  ModelLabProvider,
  ModelLabTaskType,
  PromptVersion,
  SchemaVersion,
} from '../../types/modelLab'

// ---------------------------------------------------------------------------
// Raw (snake_case) wire shapes -> camelCase mappers, mirroring src/api/http/mappers.ts
// ---------------------------------------------------------------------------

interface RawPromptVersion {
  id: number
  task_type: ModelLabTaskType
  version: number
  system_prompt: string
  developer_prompt_or_template: string | null
  notes: string | null
  created_by: number | null
  created_at: string
}
function mapPromptVersion(r: RawPromptVersion): PromptVersion {
  return {
    id: String(r.id),
    taskType: r.task_type,
    version: r.version,
    systemPrompt: r.system_prompt,
    developerPromptOrTemplate: r.developer_prompt_or_template,
    notes: r.notes,
    createdBy: r.created_by != null ? String(r.created_by) : null,
    createdAt: r.created_at,
  }
}

interface RawSchemaVersion {
  id: number
  task_type: ModelLabTaskType
  version: number
  json_schema: string
  notes: string | null
  created_at: string
}
function mapSchemaVersion(r: RawSchemaVersion): SchemaVersion {
  return { id: String(r.id), taskType: r.task_type, version: r.version, jsonSchema: r.json_schema, notes: r.notes, createdAt: r.created_at }
}

interface RawModelConfig {
  id: number
  name: string
  task_type: ModelLabTaskType
  provider: ModelLabProvider
  base_model: string
  fine_tuned_model_id: string | null
  prompt_version_id: number
  schema_version_id: number | null
  temperature: number
  confidence_threshold: number | null
  status: ModelConfigStatus
  created_by: number | null
  created_at: string
  updated_at: string
}
function mapModelConfig(r: RawModelConfig): ModelConfig {
  return {
    id: String(r.id),
    name: r.name,
    taskType: r.task_type,
    provider: r.provider,
    baseModel: r.base_model,
    fineTunedModelId: r.fine_tuned_model_id,
    promptVersionId: String(r.prompt_version_id),
    schemaVersionId: r.schema_version_id != null ? String(r.schema_version_id) : null,
    temperature: r.temperature,
    confidenceThreshold: r.confidence_threshold,
    status: r.status,
    createdBy: r.created_by != null ? String(r.created_by) : null,
    createdAt: r.created_at,
    updatedAt: r.updated_at,
  }
}

interface RawEvalDataset {
  id: number
  name: string
  task_type: ModelLabTaskType
  version: number
  description: string | null
  created_at: string
}
function mapDataset(r: RawEvalDataset): EvalDataset {
  return { id: String(r.id), name: r.name, taskType: r.task_type, version: r.version, description: r.description, createdAt: r.created_at }
}

interface RawEvalScenario {
  id: number
  dataset_id: number
  external_key: string
  title: string
  split: DatasetSplit
  question: string
  context: unknown
  tags: string[] | null
  golden_branches: GoldenBranch[] | null
  notes: string | null
  created_at: string
  updated_at: string
}
function mapScenario(r: RawEvalScenario): EvalScenario {
  return {
    id: String(r.id),
    datasetId: String(r.dataset_id),
    externalKey: r.external_key,
    title: r.title,
    split: r.split,
    question: r.question,
    context: r.context,
    tags: r.tags,
    goldenBranches: r.golden_branches,
    notes: r.notes,
    createdAt: r.created_at,
    updatedAt: r.updated_at,
  }
}

interface RawEvalReplyCase {
  id: number
  scenario_id: number
  reply_messages: string[]
  expected_branch_key: string | null
  expected_ambiguous: boolean
  expected_new_question: boolean
  expected_out_of_scope: boolean
  expected_no_match: boolean
  expected_guardrail: unknown
  tags: string[] | null
  notes: string | null
  created_at: string
  updated_at: string
}
function mapReplyCase(r: RawEvalReplyCase): EvalReplyCase {
  return {
    id: String(r.id),
    scenarioId: String(r.scenario_id),
    replyMessages: r.reply_messages ?? [],
    expectedBranchKey: r.expected_branch_key,
    expectedAmbiguous: r.expected_ambiguous,
    expectedNewQuestion: r.expected_new_question,
    expectedOutOfScope: r.expected_out_of_scope,
    expectedNoMatch: r.expected_no_match,
    expectedGuardrail: r.expected_guardrail,
    tags: r.tags,
    notes: r.notes,
    createdAt: r.created_at,
    updatedAt: r.updated_at,
  }
}

interface RawEvalRun {
  id: number
  task_type: ModelLabTaskType
  dataset_id: number
  split: DatasetSplit
  model_config_id: number
  prompt_version_id: number
  schema_version_id: number | null
  threshold_snapshot: number | null
  model_snapshot: Record<string, unknown> | null
  status: EvalRun['status']
  started_at: string | null
  finished_at: string | null
  aggregate_metrics: ClassificationMetrics | Record<string, unknown> | null
  error_message: string | null
  created_at: string
}
function mapRun(r: RawEvalRun): EvalRun {
  return {
    id: String(r.id),
    taskType: r.task_type,
    datasetId: String(r.dataset_id),
    split: r.split,
    modelConfigId: String(r.model_config_id),
    promptVersionId: String(r.prompt_version_id),
    schemaVersionId: r.schema_version_id != null ? String(r.schema_version_id) : null,
    thresholdSnapshot: r.threshold_snapshot,
    modelSnapshot: r.model_snapshot,
    status: r.status,
    startedAt: r.started_at,
    finishedAt: r.finished_at,
    aggregateMetrics: r.aggregate_metrics,
    errorMessage: r.error_message,
    createdAt: r.created_at,
  }
}

interface RawEvalResult {
  id: number
  run_id: number
  scenario_id: number
  reply_case_id: number | null
  input_snapshot: unknown
  expected: unknown
  actual: unknown
  passed: boolean
  auto_send_expected: boolean | null
  auto_send_actual: boolean | null
  latency_ms: number | null
  input_tokens: number | null
  output_tokens: number | null
  estimated_cost: number | null
  error_message: string | null
  created_at: string
}
function mapResult(r: RawEvalResult): EvalResult {
  return {
    id: String(r.id),
    runId: String(r.run_id),
    scenarioId: String(r.scenario_id),
    replyCaseId: r.reply_case_id != null ? String(r.reply_case_id) : null,
    inputSnapshot: r.input_snapshot,
    expected: r.expected,
    actual: r.actual,
    passed: r.passed,
    autoSendExpected: r.auto_send_expected,
    autoSendActual: r.auto_send_actual,
    latencyMs: r.latency_ms,
    inputTokens: r.input_tokens,
    outputTokens: r.output_tokens,
    estimatedCost: r.estimated_cost,
    errorMessage: r.error_message,
    createdAt: r.created_at,
  }
}

interface RawDeploymentHistory {
  id: number
  task_type: ModelLabTaskType
  action: DeploymentAction
  from_config_id: number | null
  to_config_id: number
  performed_by: number
  note: string | null
  created_at: string
}
function mapDeployment(r: RawDeploymentHistory): DeploymentHistoryEntry {
  return {
    id: String(r.id),
    taskType: r.task_type,
    action: r.action,
    fromConfigId: r.from_config_id != null ? String(r.from_config_id) : null,
    toConfigId: String(r.to_config_id),
    performedBy: String(r.performed_by),
    note: r.note,
    createdAt: r.created_at,
  }
}

interface RawFineTuningJob {
  id: number
  task_type: ModelLabTaskType
  provider: ModelLabProvider
  base_model: string
  training_dataset_id: number | null
  training_file_ref: string | null
  provider_job_id: string | null
  fine_tuned_model_id: string | null
  status: FineTuningJob['status']
  created_at: string
  updated_at: string
}
function mapFineTuningJob(r: RawFineTuningJob): FineTuningJob {
  return {
    id: String(r.id),
    taskType: r.task_type,
    provider: r.provider,
    baseModel: r.base_model,
    trainingDatasetId: r.training_dataset_id != null ? String(r.training_dataset_id) : null,
    trainingFileRef: r.training_file_ref,
    providerJobId: r.provider_job_id,
    fineTunedModelId: r.fine_tuned_model_id,
    status: r.status,
    createdAt: r.created_at,
    updatedAt: r.updated_at,
  }
}

interface RawGenerationHumanReview {
  id: number
  eval_result_id: number
  coverage_score: number | null
  separation_score: number | null
  granularity_score: number | null
  predecidability_score: number | null
  naturalness_score: number | null
  safety_score: number | null
  overall_score: number | null
  note: string | null
  reviewer_id: number
  created_at: string
}
function mapHumanReview(r: RawGenerationHumanReview): GenerationHumanReview {
  return {
    id: String(r.id),
    evalResultId: String(r.eval_result_id),
    coverageScore: r.coverage_score,
    separationScore: r.separation_score,
    granularityScore: r.granularity_score,
    predecidabilityScore: r.predecidability_score,
    naturalnessScore: r.naturalness_score,
    safetyScore: r.safety_score,
    overallScore: r.overall_score,
    note: r.note,
    reviewerId: String(r.reviewer_id),
    createdAt: r.created_at,
  }
}

interface RawOverview {
  production_classification_config: RawModelConfig | null
  production_generation_config: RawModelConfig | null
  recent_eval_runs: RawEvalRun[]
  recent_fine_tuning_jobs: RawFineTuningJob[]
  recent_deployments: RawDeploymentHistory[]
}

// ---------------------------------------------------------------------------
// API surface
// ---------------------------------------------------------------------------

export const modelLabApi = {
  async getOverview(): Promise<ModelLabOverview> {
    const r = await request<RawOverview>('/model-lab/overview')
    return {
      productionClassificationConfig: r.production_classification_config ? mapModelConfig(r.production_classification_config) : null,
      productionGenerationConfig: r.production_generation_config ? mapModelConfig(r.production_generation_config) : null,
      recentEvalRuns: r.recent_eval_runs.map(mapRun),
      recentFineTuningJobs: r.recent_fine_tuning_jobs.map(mapFineTuningJob),
      recentDeployments: r.recent_deployments.map(mapDeployment),
    }
  },

  async listPromptVersions(taskType: ModelLabTaskType): Promise<PromptVersion[]> {
    const r = await request<RawPromptVersion[]>(`/model-lab/prompt-versions?taskType=${taskType}`)
    return r.map(mapPromptVersion)
  },
  async createPromptVersion(input: { taskType: ModelLabTaskType; systemPrompt: string; developerPromptOrTemplate?: string; notes?: string }): Promise<PromptVersion> {
    const r = await request<RawPromptVersion>('/model-lab/prompt-versions', {
      method: 'POST',
      body: JSON.stringify({ task_type: input.taskType, system_prompt: input.systemPrompt, developer_prompt_or_template: input.developerPromptOrTemplate, notes: input.notes }),
    })
    return mapPromptVersion(r)
  },

  async listSchemaVersions(taskType: ModelLabTaskType): Promise<SchemaVersion[]> {
    const r = await request<RawSchemaVersion[]>(`/model-lab/schema-versions?taskType=${taskType}`)
    return r.map(mapSchemaVersion)
  },
  async createSchemaVersion(input: { taskType: ModelLabTaskType; jsonSchema: string; notes?: string }): Promise<SchemaVersion> {
    const r = await request<RawSchemaVersion>('/model-lab/schema-versions', {
      method: 'POST',
      body: JSON.stringify({ task_type: input.taskType, json_schema: input.jsonSchema, notes: input.notes }),
    })
    return mapSchemaVersion(r)
  },

  async listModelConfigs(taskType: ModelLabTaskType): Promise<ModelConfig[]> {
    const r = await request<RawModelConfig[]>(`/model-lab/model-configs?taskType=${taskType}`)
    return r.map(mapModelConfig)
  },
  async getModelConfig(id: string): Promise<ModelConfig> {
    return mapModelConfig(await request<RawModelConfig>(`/model-lab/model-configs/${id}`))
  },
  async createModelConfig(input: {
    name: string
    taskType: ModelLabTaskType
    provider: ModelLabProvider
    baseModel: string
    fineTunedModelId?: string
    promptVersionId: string
    schemaVersionId?: string
    temperature: number
    confidenceThreshold?: number
  }): Promise<ModelConfig> {
    const r = await request<RawModelConfig>('/model-lab/model-configs', {
      method: 'POST',
      body: JSON.stringify({
        name: input.name,
        task_type: input.taskType,
        provider: input.provider,
        base_model: input.baseModel,
        fine_tuned_model_id: input.fineTunedModelId,
        prompt_version_id: Number(input.promptVersionId),
        schema_version_id: input.schemaVersionId ? Number(input.schemaVersionId) : undefined,
        temperature: input.temperature,
        confidence_threshold: input.confidenceThreshold,
      }),
    })
    return mapModelConfig(r)
  },
  async updateModelConfig(id: string, patch: Partial<{ name: string; baseModel: string; fineTunedModelId: string; promptVersionId: string; schemaVersionId: string; temperature: number; confidenceThreshold: number }>): Promise<ModelConfig> {
    const r = await request<RawModelConfig>(`/model-lab/model-configs/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({
        name: patch.name,
        base_model: patch.baseModel,
        fine_tuned_model_id: patch.fineTunedModelId,
        prompt_version_id: patch.promptVersionId ? Number(patch.promptVersionId) : undefined,
        schema_version_id: patch.schemaVersionId ? Number(patch.schemaVersionId) : undefined,
        temperature: patch.temperature,
        confidence_threshold: patch.confidenceThreshold,
      }),
    })
    return mapModelConfig(r)
  },
  async cloneModelConfig(id: string, name?: string): Promise<ModelConfig> {
    const qs = name ? `?name=${encodeURIComponent(name)}` : ''
    return mapModelConfig(await request<RawModelConfig>(`/model-lab/model-configs/${id}/clone${qs}`, { method: 'POST' }))
  },

  async listDatasets(taskType: ModelLabTaskType): Promise<EvalDataset[]> {
    const r = await request<RawEvalDataset[]>(`/model-lab/datasets?taskType=${taskType}`)
    return r.map(mapDataset)
  },
  async createDataset(name: string, taskType: ModelLabTaskType, description?: string): Promise<EvalDataset> {
    const qs = new URLSearchParams({ name, taskType, ...(description ? { description } : {}) })
    return mapDataset(await request<RawEvalDataset>(`/model-lab/datasets?${qs.toString()}`, { method: 'POST' }))
  },
  async listScenarios(datasetId: string): Promise<EvalScenario[]> {
    const r = await request<RawEvalScenario[]>(`/model-lab/datasets/${datasetId}/scenarios`)
    return r.map(mapScenario)
  },
  async getScenario(id: string): Promise<EvalScenario> {
    return mapScenario(await request<RawEvalScenario>(`/model-lab/scenarios/${id}`))
  },
  async createScenario(datasetId: string, input: { externalKey: string; title: string; split: DatasetSplit; question: string; context?: unknown; tags?: unknown; goldenBranches?: unknown; notes?: string }): Promise<EvalScenario> {
    const r = await request<RawEvalScenario>(`/model-lab/datasets/${datasetId}/scenarios`, {
      method: 'POST',
      body: JSON.stringify({ external_key: input.externalKey, title: input.title, split: input.split, question: input.question, context: input.context, tags: input.tags, golden_branches: input.goldenBranches, notes: input.notes }),
    })
    return mapScenario(r)
  },
  async listReplyCases(scenarioId: string): Promise<EvalReplyCase[]> {
    const r = await request<RawEvalReplyCase[]>(`/model-lab/scenarios/${scenarioId}/reply-cases`)
    return r.map(mapReplyCase)
  },
  async createReplyCase(scenarioId: string, input: { replyMessages: string[]; expectedBranchKey?: string; expectedAmbiguous?: boolean; expectedNewQuestion?: boolean; expectedOutOfScope?: boolean; expectedNoMatch?: boolean; tags?: unknown; notes?: string }): Promise<EvalReplyCase> {
    const r = await request<RawEvalReplyCase>(`/model-lab/scenarios/${scenarioId}/reply-cases`, {
      method: 'POST',
      body: JSON.stringify({
        reply_messages: input.replyMessages,
        expected_branch_key: input.expectedBranchKey,
        expected_ambiguous: input.expectedAmbiguous ?? false,
        expected_new_question: input.expectedNewQuestion ?? false,
        expected_out_of_scope: input.expectedOutOfScope ?? false,
        expected_no_match: input.expectedNoMatch ?? false,
        tags: input.tags,
        notes: input.notes,
      }),
    })
    return mapReplyCase(r)
  },

  async previewClassificationRun(datasetId: string, split: DatasetSplit): Promise<number> {
    const r = await request<{ case_count: number }>(`/model-lab/classification-eval-runs/preview?datasetId=${datasetId}&split=${split}`)
    return r.case_count
  },
  async runClassificationEval(datasetId: string, split: DatasetSplit, modelConfigId: string): Promise<EvalRun> {
    const r = await request<RawEvalRun>('/model-lab/classification-eval-runs', {
      method: 'POST',
      body: JSON.stringify({ dataset_id: Number(datasetId), split, model_config_id: Number(modelConfigId) }),
    })
    return mapRun(r)
  },
  async listClassificationRuns(): Promise<EvalRun[]> {
    const r = await request<RawEvalRun[]>('/model-lab/classification-eval-runs')
    return r.map(mapRun)
  },
  async getClassificationRun(id: string): Promise<EvalRun> {
    return mapRun(await request<RawEvalRun>(`/model-lab/classification-eval-runs/${id}`))
  },
  async getClassificationResults(id: string, failedOnly = false): Promise<EvalResult[]> {
    const r = await request<RawEvalResult[]>(`/model-lab/classification-eval-runs/${id}/results?failedOnly=${failedOnly}`)
    return r.map(mapResult)
  },

  async runGenerationEval(datasetId: string, split: DatasetSplit, modelConfigId: string): Promise<EvalRun> {
    const r = await request<RawEvalRun>('/model-lab/generation-eval-runs', {
      method: 'POST',
      body: JSON.stringify({ dataset_id: Number(datasetId), split, model_config_id: Number(modelConfigId) }),
    })
    return mapRun(r)
  },
  async getGenerationRun(id: string): Promise<EvalRun> {
    return mapRun(await request<RawEvalRun>(`/model-lab/generation-eval-runs/${id}`))
  },
  async getGenerationResults(id: string): Promise<EvalResult[]> {
    const r = await request<RawEvalResult[]>(`/model-lab/generation-eval-runs/${id}/results`)
    return r.map(mapResult)
  },
  async submitGenerationHumanReview(evalResultId: string, input: Partial<{ coverageScore: number; separationScore: number; granularityScore: number; predecidabilityScore: number; naturalnessScore: number; safetyScore: number; overallScore: number; note: string }>): Promise<GenerationHumanReview> {
    const r = await request<RawGenerationHumanReview>(`/model-lab/eval-results/${evalResultId}/review`, {
      method: 'POST',
      body: JSON.stringify({
        coverage_score: input.coverageScore,
        separation_score: input.separationScore,
        granularity_score: input.granularityScore,
        predecidability_score: input.predecidabilityScore,
        naturalness_score: input.naturalnessScore,
        safety_score: input.safetyScore,
        overall_score: input.overallScore,
        note: input.note,
      }),
    })
    return mapHumanReview(r)
  },

  async getProductionConfig(taskType: ModelLabTaskType): Promise<ModelConfig | null> {
    const r = await request<RawModelConfig | null>(`/model-lab/deployment/production?taskType=${taskType}`)
    return r ? mapModelConfig(r) : null
  },
  async getDeploymentHistory(taskType?: ModelLabTaskType): Promise<DeploymentHistoryEntry[]> {
    const qs = taskType ? `?taskType=${taskType}` : ''
    const r = await request<RawDeploymentHistory[]>(`/model-lab/deployment/history${qs}`)
    return r.map(mapDeployment)
  },
  async promote(targetConfigId: string, note?: string): Promise<DeploymentHistoryEntry> {
    const r = await request<RawDeploymentHistory>('/model-lab/deployment/promote', {
      method: 'POST',
      body: JSON.stringify({ target_config_id: Number(targetConfigId), note }),
    })
    return mapDeployment(r)
  },
  async rollback(taskType: ModelLabTaskType, note?: string): Promise<DeploymentHistoryEntry> {
    const r = await request<RawDeploymentHistory>('/model-lab/deployment/rollback', {
      method: 'POST',
      body: JSON.stringify({ task_type: taskType, note }),
    })
    return mapDeployment(r)
  },

  async listFineTuningJobs(): Promise<FineTuningJob[]> {
    const r = await request<RawFineTuningJob[]>('/model-lab/fine-tuning-jobs')
    return r.map(mapFineTuningJob)
  },
  async createFineTuningJob(input: { taskType: ModelLabTaskType; provider: ModelLabProvider; baseModel: string; trainingDatasetId?: string }): Promise<FineTuningJob> {
    const r = await request<RawFineTuningJob>('/model-lab/fine-tuning-jobs', {
      method: 'POST',
      body: JSON.stringify({ task_type: input.taskType, provider: input.provider, base_model: input.baseModel, training_dataset_id: input.trainingDatasetId ? Number(input.trainingDatasetId) : undefined }),
    })
    return mapFineTuningJob(r)
  },
  /** Always fails with MODELLAB-016 today — FineTuningService.submit is a deliberate not-yet-implemented stub. */
  async submitFineTuningJob(id: string): Promise<FineTuningJob> {
    return mapFineTuningJob(await request<RawFineTuningJob>(`/model-lab/fine-tuning-jobs/${id}/submit`, { method: 'POST' }))
  },
}
