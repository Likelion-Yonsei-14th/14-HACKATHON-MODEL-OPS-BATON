// BATON Model Lab domain types. Kept in a dedicated file (not merged into the existing
// baton/branch/classification types) because Model Lab is an entirely separate track — its
// datasets and entities must never be confused with production Baton/Branch/Classification data.
//
// The backend's DTOs already return camelCase JSON field values are as-is (the backend record
// field names, which Spring's global SNAKE_CASE Jackson strategy turns into snake_case on the
// wire) — see src/api/modelLab/client.ts for the snake_case -> camelCase mapping, mirroring the
// pattern in src/api/http/mappers.ts for the rest of the app.

export type ModelLabTaskType = 'BRANCH_GENERATION' | 'REPLY_CLASSIFICATION'

export type ModelLabProvider = 'OPENAI' | 'OLLAMA'

export type ModelConfigStatus = 'DRAFT' | 'EVALUATING' | 'STAGING' | 'PRODUCTION' | 'ARCHIVED'

export type DatasetSplit = 'SMOKE' | 'CORE' | 'HOLDOUT'

export type EvalRunStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'

export type FineTuningJobStatus = 'NOT_STARTED' | 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'

export type DeploymentAction = 'PROMOTE' | 'ROLLBACK'

export interface PromptVersion {
  id: string
  taskType: ModelLabTaskType
  version: number
  systemPrompt: string
  developerPromptOrTemplate: string | null
  notes: string | null
  createdBy: string | null
  createdAt: string
}

export interface SchemaVersion {
  id: string
  taskType: ModelLabTaskType
  version: number
  jsonSchema: string
  notes: string | null
  createdAt: string
}

export interface ModelConfig {
  id: string
  name: string
  taskType: ModelLabTaskType
  provider: ModelLabProvider
  baseModel: string
  fineTunedModelId: string | null
  promptVersionId: string
  schemaVersionId: string | null
  temperature: number
  confidenceThreshold: number | null
  status: ModelConfigStatus
  createdBy: string | null
  createdAt: string
  updatedAt: string
}

export interface EvalDataset {
  id: string
  name: string
  taskType: ModelLabTaskType
  version: number
  description: string | null
  createdAt: string
}

export interface EvalScenario {
  id: string
  datasetId: string
  externalKey: string
  title: string
  split: DatasetSplit
  question: string
  context: unknown
  tags: string[] | null
  goldenBranches: GoldenBranch[] | null
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface GoldenBranch {
  id?: string | number
  key?: string
  name: string
  condition_text: string
  decision_text: string
  response_text: string
}

export interface EvalReplyCase {
  id: string
  scenarioId: string
  replyMessages: string[]
  expectedBranchKey: string | null
  expectedAmbiguous: boolean
  expectedNewQuestion: boolean
  expectedOutOfScope: boolean
  expectedNoMatch: boolean
  expectedGuardrail: unknown
  tags: string[] | null
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface EvalRun {
  id: string
  taskType: ModelLabTaskType
  datasetId: string
  split: DatasetSplit
  modelConfigId: string
  promptVersionId: string
  schemaVersionId: string | null
  thresholdSnapshot: number | null
  modelSnapshot: Record<string, unknown> | null
  status: EvalRunStatus
  startedAt: string | null
  finishedAt: string | null
  aggregateMetrics: ClassificationMetrics | GenerationMetrics | Record<string, unknown> | null
  errorMessage: string | null
  createdAt: string
}

/** Field names match the backend's aggregate_metrics_json keys (spec section 11). */
export interface ClassificationMetrics {
  total_cases: number
  schema_validity: number
  branch_match_accuracy: number
  ambiguous_detection_recall: number
  new_question_detection_recall: number
  out_of_scope_detection_recall: number
  no_match_detection_recall: number
  false_auto_send_rate: number
  auto_send_coverage: number
  average_latency_ms: number
  average_input_tokens: number
  average_output_tokens: number
  estimated_cost_total: number
}

export interface GenerationMetrics {
  total_scenarios: number
  hard_rule_pass_rate: number
}

export interface EvalResult {
  id: string
  runId: string
  scenarioId: string
  replyCaseId: string | null
  inputSnapshot: unknown
  expected: unknown
  actual: unknown
  passed: boolean
  autoSendExpected: boolean | null
  autoSendActual: boolean | null
  latencyMs: number | null
  inputTokens: number | null
  outputTokens: number | null
  estimatedCost: number | null
  errorMessage: string | null
  createdAt: string
}

export interface GenerationHumanReview {
  id: string
  evalResultId: string
  coverageScore: number | null
  separationScore: number | null
  granularityScore: number | null
  predecidabilityScore: number | null
  naturalnessScore: number | null
  safetyScore: number | null
  overallScore: number | null
  note: string | null
  reviewerId: string
  createdAt: string
}

export interface DeploymentHistoryEntry {
  id: string
  taskType: ModelLabTaskType
  action: DeploymentAction
  fromConfigId: string | null
  toConfigId: string
  performedBy: string
  note: string | null
  createdAt: string
}

export interface FineTuningJob {
  id: string
  taskType: ModelLabTaskType
  provider: ModelLabProvider
  baseModel: string
  trainingDatasetId: string | null
  trainingFileRef: string | null
  providerJobId: string | null
  fineTunedModelId: string | null
  status: FineTuningJobStatus
  createdAt: string
  updatedAt: string
}

export interface ModelLabOverview {
  productionClassificationConfig: ModelConfig | null
  productionGenerationConfig: ModelConfig | null
  recentEvalRuns: EvalRun[]
  recentFineTuningJobs: FineTuningJob[]
  recentDeployments: DeploymentHistoryEntry[]
}
