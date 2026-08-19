import { Navigate, Route, Routes } from 'react-router-dom'
import { Landing } from './pages/Landing'
import { Login } from './pages/Login'
import { Signup } from './pages/Signup'
import { SlackConnect } from './pages/SlackConnect'
import { ConnectCallback } from './pages/ConnectCallback'
import { ConnectError } from './pages/ConnectError'
import { Home } from './pages/Home'
import { ConversationPicker } from './pages/ConversationPicker'
import { ComposeBaton } from './pages/ComposeBaton'
import { BranchPrep } from './pages/BranchPrep'
import { SendConfirm } from './pages/SendConfirm'
import { BatonDetail } from './pages/BatonDetail'
import { PendingResponse } from './pages/PendingResponse'
import { ResultConfirm } from './pages/ResultConfirm'
import { SyncError } from './pages/SyncError'
import { Settings } from './pages/Settings'
import { ModelLabLayout } from './pages/modellab/ModelLabLayout'
import { Overview as ModelLabOverview } from './pages/modellab/Overview'
import { Prompts as ModelLabPrompts } from './pages/modellab/Prompts'
import { Datasets as ModelLabDatasets } from './pages/modellab/Datasets'
import { ClassificationWorkspace } from './pages/modellab/ClassificationWorkspace'
import { GenerationWorkspace } from './pages/modellab/GenerationWorkspace'
import { EvalRunsList, EvalRunDetail } from './pages/modellab/EvalRuns'
import { Deployment as ModelLabDeployment } from './pages/modellab/Deployment'
import { FineTuning as ModelLabFineTuning } from './pages/modellab/FineTuning'

function App() {
  return (
    <Routes>
      <Route element={<Landing />} path="/" />
      <Route element={<Signup />} path="/signup" />
      <Route element={<Login />} path="/login" />
      <Route element={<SlackConnect />} path="/connect" />
      <Route element={<ConnectCallback />} path="/connect/callback" />
      <Route element={<ConnectError />} path="/connect/error" />
      <Route element={<Home />} path="/home" />
      <Route element={<ConversationPicker />} path="/conversations" />
      <Route element={<ComposeBaton />} path="/conversations/:conversationId/compose" />
      <Route element={<BranchPrep />} path="/conversations/:conversationId/branches" />
      <Route element={<SendConfirm />} path="/conversations/:conversationId/confirm" />
      <Route element={<BatonDetail />} path="/batons/:batonId" />
      <Route element={<PendingResponse />} path="/batons/:batonId/pending" />
      <Route element={<ResultConfirm />} path="/batons/:batonId/result" />
      <Route element={<SyncError />} path="/sync-error" />
      <Route element={<Settings />} path="/settings" />

      {/* BATON Model Lab — internal AI eval/ops console, admin-gated server-side (spec section 0/27). */}
      <Route element={<ModelLabLayout />} path="/batons/models">
        <Route element={<ModelLabOverview />} index />
        <Route element={<ModelLabPrompts />} path="prompts" />
        <Route element={<ModelLabDatasets />} path="datasets" />
        <Route element={<ClassificationWorkspace />} path="classification" />
        <Route element={<GenerationWorkspace />} path="generation" />
        <Route element={<EvalRunsList />} path="eval-runs" />
        <Route element={<EvalRunDetail />} path="eval-runs/:id" />
        <Route element={<ModelLabDeployment />} path="deployment" />
        <Route element={<ModelLabFineTuning />} path="fine-tuning" />
      </Route>

      <Route element={<Navigate replace to="/" />} path="*" />
    </Routes>
  )
}

export default App
