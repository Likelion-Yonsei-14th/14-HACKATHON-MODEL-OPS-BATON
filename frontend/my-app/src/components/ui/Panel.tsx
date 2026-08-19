import type { HTMLAttributes } from 'react'

export function Panel({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={`rounded-[8px] border border-border bg-white p-4 ${className}`} {...props} />
}
