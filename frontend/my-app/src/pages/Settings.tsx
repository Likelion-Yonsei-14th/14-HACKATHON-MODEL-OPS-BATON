import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { API_KEY_STORAGE_KEY } from '../api/http/config'
import type { LlmProvider, PlatformConnection } from '../types'
import { AppShell } from '../components/layout/AppShell'
import { Button } from '../components/ui/Button'

function ChevronDown() {
  return (
    <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
      <path d="M2 4.5L6 8.5L10 4.5" stroke="#94a3b8" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

interface SelectFieldProps {
  label: string
  value: string
  onChange: (v: string) => void
  options: { value: string; label: string }[]
}

function SelectField({ label, value, onChange, options }: SelectFieldProps) {
  return (
    <div className="flex w-full flex-col gap-1.5">
      <label className="font-suit text-sm font-semibold leading-[1.5] text-ink">{label}</label>
      <div className="relative w-full">
        <select
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-full appearance-none rounded-[6px] border border-border bg-white px-3 py-2.5 font-suit text-sm font-semibold text-ink outline-none focus:border-primary cursor-pointer"
        >
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        <div className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2">
          <ChevronDown />
        </div>
      </div>
    </div>
  )
}

const LANGUAGE_OPTIONS = [
  { value: 'ko', label: '한국어' },
  { value: 'en', label: 'English' },
]

const TIMEZONE_OPTIONS = [
  { value: 'Asia/Seoul', label: 'Asia/Seoul (UTC+9)' },
  { value: 'America/New_York', label: 'America/New_York (UTC-5)' },
  { value: 'Europe/London', label: 'Europe/London (UTC+0)' },
  { value: 'Asia/Tokyo', label: 'Asia/Tokyo (UTC+9)' },
  { value: 'America/Los_Angeles', label: 'America/Los_Angeles (UTC-8)' },
]

const WAIT_TIME_OPTIONS = [
  { value: '1h', label: '1시간' },
  { value: '3h', label: '3시간' },
  { value: '6h', label: '6시간' },
  { value: '12h', label: '12시간' },
  { value: '24h', label: '24시간' },
  { value: '48h', label: '48시간' },
]

const LLM_PROVIDER_OPTIONS = [
  { value: 'LOCAL', label: '로컬 모델 (Qwen3 0.6B) — 무료' },
  { value: 'OPENAI', label: 'OpenAI (gpt-4o-mini) — API 토큰 사용' },
]

const INITIAL = {
  language: 'ko',
  timezone: 'Asia/Seoul',
  autoSendEnabled: false,
  confidenceThreshold: '0.8',
  maxWaitTime: '24h',
  llmProvider: 'LOCAL' as LlmProvider,
}

const panel = 'rounded-[8px] border border-border bg-white'

export function Settings() {
  const navigate = useNavigate()
  const [form, setForm] = useState(INITIAL)
  const [committed, setCommitted] = useState(INITIAL)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [platform, setPlatform] = useState<PlatformConnection | null>(null)

  useEffect(() => {
    Promise.all([api.getCurrentUser(), api.getPlatformConnection()])
      .then(([user, conn]) => {
        const loaded = {
          ...INITIAL,
          language: user.language ?? INITIAL.language,
          timezone: user.timezone ?? INITIAL.timezone,
          llmProvider: user.llmProvider ?? INITIAL.llmProvider,
        }
        setForm(loaded)
        setCommitted(loaded)
        setPlatform(conn)
      })
      .catch(() => setLoadError('설정을 불러오지 못했습니다. 페이지를 새로고침해 주세요.'))
      .finally(() => setLoading(false))
  }, [])

  function set<K extends keyof typeof INITIAL>(key: K, value: (typeof INITIAL)[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  function handleCancel() {
    setForm(committed)
    setSaveError(null)
  }

  async function handleReconnect() {
    if (!platform) return
    try {
      await api.disconnectPlatform(platform.id)
    } catch { /* 해제 실패해도 OAuth 재시작 시도 */ }
    const { redirectUrl } = await api.startSlackConnect()
    window.location.href = redirectUrl
  }

  async function handleLogout() {
    try {
      await api.logout()
    } finally {
      localStorage.removeItem(API_KEY_STORAGE_KEY)
      navigate('/login', { replace: true })
    }
  }

  async function handleSave() {
    setSaving(true)
    setSaveError(null)
    try {
      await api.updateUser({ language: form.language, timezone: form.timezone, llmProvider: form.llmProvider })
      setCommitted(form)
    } catch {
      setSaveError('저장에 실패했습니다. 다시 시도해주세요.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <AppShell>
        <p className="font-suit text-sm text-muted">설정을 불러오는 중...</p>
      </AppShell>
    )
  }

  if (loadError) {
    return (
      <AppShell>
        <p className="font-suit text-sm text-red-500">{loadError}</p>
      </AppShell>
    )
  }

  return (
    <AppShell>
      <div className="flex flex-col gap-6">
        <h1 className="font-suit text-2xl font-semibold leading-[1.5] text-ink">개인 설정</h1>

        {/* 작업 환경 */}
        <section className="flex flex-col gap-3">
          <h2 className="font-suit text-base font-semibold leading-[1.5] text-ink">작업 환경</h2>
          <div className={`${panel} flex flex-col gap-4 p-5`}>
            <SelectField
              label="작업 언어"
              value={form.language}
              onChange={(v) => set('language', v)}
              options={LANGUAGE_OPTIONS}
            />
            <SelectField
              label="타임존"
              value={form.timezone}
              onChange={(v) => set('timezone', v)}
              options={TIMEZONE_OPTIONS}
            />
          </div>
        </section>

        {/* AI 모델 */}
        <section className="flex flex-col gap-3">
          <h2 className="font-suit text-base font-semibold leading-[1.5] text-ink">AI 모델</h2>
          <div className={`${panel} flex flex-col gap-4 p-5`}>
            <SelectField
              label="답장 분류 · 분기 생성에 사용할 모델"
              value={form.llmProvider}
              onChange={(v) => set('llmProvider', v as LlmProvider)}
              options={LLM_PROVIDER_OPTIONS}
            />
            <p className="font-suit text-[13px] font-semibold leading-[1.5] text-muted">
              기본은 서버에 올라간 로컬 모델(Qwen3 0.6B)을 씁니다. OpenAI로 바꾸면 더 정확하지만 API 토큰을 소모합니다.
            </p>
          </div>
        </section>

        {/* 대기 시간 설정 */}
        <section className="flex flex-col gap-3">
          <h2 className="font-suit text-base font-semibold leading-[1.5] text-ink">대기 시간 설정</h2>
          <div className={`${panel} flex flex-col gap-4 p-5`}>
            <SelectField
              label="최대 대기 시간"
              value={form.maxWaitTime}
              onChange={(v) => set('maxWaitTime', v)}
              options={WAIT_TIME_OPTIONS}
            />
            <p className="font-suit text-[13px] font-semibold leading-[1.5] text-muted">
              설정한 시간 안에 상대 답장이 없으면 바통이 만료됩니다. 만료 후 자동 응답은 실행되지 않습니다.
            </p>
          </div>
        </section>

        {/* 연결된 플랫폼 */}
        <section className="flex flex-col gap-3">
          <h2 className="font-suit text-base font-semibold leading-[1.5] text-ink">연결된 플랫폼</h2>
          <div className={`${panel} px-5 py-1`}>
            <div className="flex items-center gap-4 py-4">
              <div className="flex flex-col gap-1">
                <span className="font-suit text-sm font-semibold leading-[1.5] text-ink">Slack 워크스페이스</span>
                <span className="font-suit text-[13px] font-semibold leading-[1.5] text-muted">
                  {platform?.workspaceName ?? platform?.workspaceId ?? '—'}
                </span>
              </div>
              <div className="flex-1" />
              <span className="font-suit text-sm font-semibold leading-[1.5] text-ink">
                {platform?.connectionStatus === 'CONNECTED' ? '연결됨' : '연결 안 됨'}
              </span>
              <button
                type="button"
                onClick={handleReconnect}
                disabled={!platform}
                className="cursor-pointer rounded-[6px] border border-border bg-white px-3.5 py-2 font-suit text-[13px] font-semibold text-ink transition hover:bg-primary-soft disabled:opacity-40"
              >
                재연결
              </button>
            </div>
            <div className="h-px bg-border" />
            <div className="flex items-center gap-4 py-4">
              <div className="flex flex-col gap-1">
                <span className="font-suit text-sm font-semibold leading-[1.5] text-ink">Microsoft Teams</span>
                <span className="font-suit text-[13px] font-semibold leading-[1.5] text-muted">지원 예정</span>
              </div>
              <div className="flex-1" />
              <span className="font-suit text-sm font-semibold leading-[1.5] text-muted-2">준비 중</span>
            </div>
            <div className="h-px bg-border" />
            <div className="flex items-center gap-4 py-4">
              <div className="flex flex-col gap-1">
                <span className="font-suit text-sm font-semibold leading-[1.5] text-ink">Google Chat</span>
                <span className="font-suit text-[13px] font-semibold leading-[1.5] text-muted">지원 예정</span>
              </div>
              <div className="flex-1" />
              <span className="font-suit text-sm font-semibold leading-[1.5] text-muted-2">준비 중</span>
            </div>
          </div>
        </section>

        {/* 하단 액션 */}
        {saveError && (
          <p className="text-right font-suit text-sm text-red-500">{saveError}</p>
        )}
        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={handleCancel}
            disabled={saving}
            className="cursor-pointer font-suit text-sm font-semibold leading-[1.5] text-muted transition hover:text-ink disabled:opacity-40"
          >
            변경사항 취소
          </button>
          <Button onClick={handleSave} disabled={saving}>
            {saving ? '저장 중...' : '설정 저장'}
          </Button>
        </div>

        <div className="h-px bg-border" />

        <div className="flex items-center justify-between">
          <div className="flex flex-col gap-0.5">
            <span className="font-suit text-sm font-semibold text-ink">로그아웃</span>
            <span className="font-suit text-[13px] text-muted">현재 기기에서 로그아웃합니다.</span>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="cursor-pointer rounded-[6px] border border-red-200 bg-white px-3.5 py-2 font-suit text-[13px] font-semibold text-red-500 transition hover:bg-red-50"
          >
            로그아웃
          </button>
        </div>
      </div>
    </AppShell>
  )
}
