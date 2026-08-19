import { Link, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { Header } from './Header'

const navItems = [
  { label: '바통 홈', to: '/home' },
  { label: '새 바통', to: '/conversations' },
  { label: '개인설정', to: '/settings' },
]

export function AppShell({ children }: { children: ReactNode }) {
  const location = useLocation()

  return (
    <div className="min-h-screen bg-white">
      <Header />

      <div className="flex">
        <aside className="flex h-[calc(100vh-76px)] w-[260px] flex-col justify-between border-r border-border-strong">
          <nav className="flex flex-col gap-2 p-4">
            {navItems.map((item) => {
              const isActive = location.pathname.startsWith(item.to)
              return (
                <Link
                  className={`font-suit rounded-[6px] px-4 py-2 text-sm tracking-[0.3px] ${
                    isActive ? 'bg-primary-soft font-semibold text-ink' : 'text-muted-2 hover:bg-primary-soft/50'
                  }`}
                  key={item.to}
                  to={item.to}
                >
                  {item.label}
                </Link>
              )
            })}
          </nav>
          <div className="border-t border-border-strong p-4">
            <span className="font-suit block px-4 py-2 text-sm text-muted-2">사용 방법</span>
          </div>
        </aside>

        <main className="flex-1 p-10">{children}</main>
      </div>
    </div>
  )
}
