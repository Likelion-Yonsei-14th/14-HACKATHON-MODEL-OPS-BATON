const steps = ['메시지 작성', '분기 검토', '발송 확인'] as const

export function StepTabs({ current }: { current: 1 | 2 | 3 }) {
  return (
    <div className="font-suit flex items-center gap-1.5 text-sm">
      {steps.map((label, i) => {
        const step = (i + 1) as 1 | 2 | 3
        const done = step < current
        const active = step === current
        return (
          <div className="flex items-center gap-1.5" key={label}>
            {i > 0 && <span className="text-muted-2">›</span>}
            <span
              className={`flex size-5 items-center justify-center rounded-full text-[11px] font-bold ${
                done ? 'bg-emerald-600 text-white' : active ? 'bg-primary text-white' : 'border border-primary-soft bg-white text-muted-2'
              }`}
            >
              {done ? '✓' : step}
            </span>
            <span className={active ? 'font-semibold text-primary' : 'text-muted-2'}>{label}</span>
          </div>
        )
      })}
    </div>
  )
}
