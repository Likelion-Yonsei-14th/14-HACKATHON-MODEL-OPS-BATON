import type { ReactNode } from 'react'

// Small local UI primitives for the Model Lab dark dev-tool surface. Deliberately not the
// customer-app's Badge/Button/Panel (see ModelLabLayout.tsx) — those are light-themed and tuned for
// the Figma consumer design system. These are intentionally minimal: no new dependency, just Tailwind.

export function MlPanel({ title, action, children, className = '' }: { title?: string; action?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-[4px] border border-[#1f2328] bg-[#0d0f12] ${className}`}>
      {(title || action) && (
        <div className="flex items-center justify-between border-b border-[#1f2328] px-4 py-2">
          {title && <h3 className="text-[12px] font-semibold uppercase tracking-wide text-[#9aa4b2]">{title}</h3>}
          {action}
        </div>
      )}
      <div className="p-4">{children}</div>
    </div>
  )
}

type Tone = 'neutral' | 'good' | 'bad' | 'warn' | 'info'
const toneClasses: Record<Tone, string> = {
  neutral: 'bg-[#1a1e24] text-[#9aa4b2] border-[#2a2f37]',
  good: 'bg-[#0d2318] text-[#4ade80] border-[#1a4230]',
  bad: 'bg-[#2a1414] text-[#f87171] border-[#4a2020]',
  warn: 'bg-[#2a2210] text-[#facc15] border-[#4a3d1a]',
  info: 'bg-[#101d2e] text-[#5b8def] border-[#1c3252]',
}
export function MlBadge({ tone = 'neutral', children }: { tone?: Tone; children: ReactNode }) {
  return <span className={`inline-flex items-center rounded-[3px] border px-1.5 py-0.5 text-[11px] font-medium ${toneClasses[tone]}`}>{children}</span>
}

export function statusTone(status: string): Tone {
  switch (status) {
    case 'PRODUCTION':
    case 'COMPLETED':
    case 'SUCCEEDED':
      return 'good'
    case 'FAILED':
    case 'ARCHIVED':
      return 'bad'
    case 'RUNNING':
    case 'EVALUATING':
    case 'QUEUED':
      return 'warn'
    case 'STAGING':
      return 'info'
    default:
      return 'neutral'
  }
}

export function MlButton({ children, variant = 'default', ...props }: { children: ReactNode; variant?: 'default' | 'primary' | 'danger' } & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  const base = 'rounded-[3px] border px-3 py-1.5 text-[12px] font-medium disabled:cursor-not-allowed disabled:opacity-40'
  const variants: Record<string, string> = {
    default: 'border-[#2a2f37] bg-[#151a20] text-[#d4d8dd] hover:bg-[#1a1f26]',
    primary: 'border-[#5b8def] bg-[#5b8def] text-white hover:bg-[#4a7ade]',
    danger: 'border-[#c0392b] bg-[#c0392b] text-white hover:bg-[#a83226]',
  }
  return (
    <button className={`${base} ${variants[variant]}`} type="button" {...props}>
      {children}
    </button>
  )
}

export function MlInput(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={`w-full rounded-[3px] border border-[#2a2f37] bg-[#0b0d10] px-2 py-1.5 text-[13px] text-[#d4d8dd] outline-none focus:border-[#5b8def] ${props.className ?? ''}`} />
}

export function MlSelect({ children, ...props }: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select {...props} className={`w-full rounded-[3px] border border-[#2a2f37] bg-[#0b0d10] px-2 py-1.5 text-[13px] text-[#d4d8dd] outline-none focus:border-[#5b8def] ${props.className ?? ''}`}>
      {children}
    </select>
  )
}

export function MlTextarea(props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={`w-full rounded-[3px] border border-[#2a2f37] bg-[#0b0d10] px-2 py-1.5 font-mono text-[12px] text-[#d4d8dd] outline-none focus:border-[#5b8def] ${props.className ?? ''}`} />
}

export function MetricTile({ label, value, tone = 'neutral', hint }: { label: string; value: string; tone?: Tone; hint?: string }) {
  const valueTone: Record<Tone, string> = { neutral: 'text-[#d4d8dd]', good: 'text-[#4ade80]', bad: 'text-[#f87171]', warn: 'text-[#facc15]', info: 'text-[#5b8def]' }
  return (
    <div className="rounded-[3px] border border-[#1f2328] bg-[#0b0d10] px-3 py-2">
      <div className="text-[10px] uppercase tracking-wide text-[#5b6270]">{label}</div>
      <div className={`mt-0.5 text-[18px] font-semibold tabular-nums ${valueTone[tone]}`}>{value}</div>
      {hint && <div className="mt-0.5 text-[10px] text-[#5b6270]">{hint}</div>}
    </div>
  )
}

export function PageHeader({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="flex items-start justify-between border-b border-[#1f2328] px-6 py-4">
      <div>
        <h1 className="text-[15px] font-semibold text-white">{title}</h1>
        {description && <p className="mt-0.5 text-[12px] text-[#8b93a1]">{description}</p>}
      </div>
      {action}
    </div>
  )
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <div className="rounded-[3px] border border-dashed border-[#2a2f37] px-4 py-6 text-center text-[12px] text-[#5b6270]">{children}</div>
}

export function ConfirmDialog({ title, description, confirmLabel, danger, onConfirm, onCancel }: { title: string; description: ReactNode; confirmLabel: string; danger?: boolean; onConfirm: () => void; onCancel: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="w-full max-w-md rounded-[4px] border border-[#2a2f37] bg-[#12151a] p-5">
        <h2 className="text-[14px] font-semibold text-white">{title}</h2>
        <div className="mt-3 space-y-2 text-[12px] text-[#9aa4b2]">{description}</div>
        <div className="mt-5 flex justify-end gap-2">
          <MlButton onClick={onCancel}>취소</MlButton>
          <MlButton onClick={onConfirm} variant={danger ? 'danger' : 'primary'}>
            {confirmLabel}
          </MlButton>
        </div>
      </div>
    </div>
  )
}

export function formatPercent(n: number | undefined | null): string {
  if (n == null) return '—'
  return `${(n * 100).toFixed(1)}%`
}
