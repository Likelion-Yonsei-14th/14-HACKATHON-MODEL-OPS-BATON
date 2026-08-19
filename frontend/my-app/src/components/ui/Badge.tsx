import type { ReactNode } from 'react'

type Tone = 'warning' | 'success' | 'info' | 'neutral'

const toneClasses: Record<Tone, string> = {
  warning: 'bg-amber-50 text-amber-700',
  success: 'bg-emerald-50 text-emerald-700',
  info: 'bg-primary-soft text-primary',
  neutral: 'bg-gray-100 text-muted',
}

export function Badge({ tone = 'neutral', children }: { tone?: Tone; children: ReactNode }) {
  return (
    <span className={`font-suit inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-semibold ${toneClasses[tone]}`}>
      {children}
    </span>
  )
}
