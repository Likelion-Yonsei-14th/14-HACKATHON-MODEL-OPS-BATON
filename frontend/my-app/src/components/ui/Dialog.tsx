import type { ReactNode } from 'react'
import { Button } from './Button'

interface DialogProps {
  title: string
  description: ReactNode
  confirmLabel: string
  onConfirm: () => void
  onCancel: () => void
}

export function Dialog({ title, description, confirmLabel, onConfirm, onCancel }: DialogProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4">
      <div className="w-full max-w-md rounded-[10px] border border-border bg-white p-8">
        <h2 className="font-suit text-lg font-semibold text-ink">{title}</h2>
        <div className="mt-4 space-y-3 text-sm text-muted">{description}</div>
        <div className="mt-6 flex justify-end gap-3">
          <button className="font-suit text-sm text-muted-2 hover:text-ink" onClick={onCancel} type="button">
            취소
          </button>
          <Button onClick={onConfirm}>{confirmLabel}</Button>
        </div>
      </div>
    </div>
  )
}
