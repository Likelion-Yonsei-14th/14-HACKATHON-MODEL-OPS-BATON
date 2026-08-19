import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { API_KEY_STORAGE_KEY } from '../../api/http/config'

// Model Lab is a deliberately different visual register from the rest of the Baton app (spec
// section 19-20): an internal dev tool in the Linear/Vercel/GitHub-Actions register, not the
// customer-facing product — dense, thin borders, small radius, one accent color, no gradients,
// no glow, no hero. It does not reuse AppShell/Header's Figma-derived branding on purpose.

const navGroups: { label: string; items: { label: string; to: string }[] }[] = [
  { label: '', items: [{ label: '개요', to: '/batons/models' }] },
  {
    label: '모델',
    items: [
      { label: '분류', to: '/batons/models/classification' },
      { label: '생성', to: '/batons/models/generation' },
    ],
  },
  {
    label: '',
    items: [
      { label: '프롬프트', to: '/batons/models/prompts' },
      { label: '데이터셋', to: '/batons/models/datasets' },
      { label: '평가 실행', to: '/batons/models/eval-runs' },
      { label: '파인튜닝', to: '/batons/models/fine-tuning' },
      { label: '배포', to: '/batons/models/deployment' },
    ],
  },
]

export function ModelLabLayout() {
  const location = useLocation()
  // Model Lab lives on its own origin/port, separate from the main Baton app, so a session logged
  // in there does not carry over here — read the key straight from localStorage rather than trusting
  // a stale flag, since this only needs to gate the initial render.
  const [apiKey, setApiKey] = useState(() => localStorage.getItem(API_KEY_STORAGE_KEY))

  function handleLogout() {
    localStorage.removeItem(API_KEY_STORAGE_KEY)
    setApiKey(null)
  }

  if (!apiKey) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#0b0d10] text-[13px] text-[#d4d8dd]">
        <div className="w-[320px] rounded-lg border border-[#1f2328] bg-[#0d0f12] p-6 text-center">
          <div className="font-mono text-[11px] font-semibold tracking-wider text-[#9aa4b2]">BATON AI LAB</div>
          <p className="mt-3 text-[13px] text-[#9aa4b2]">관리자 계정으로 로그인해야 Model Lab을 사용할 수 있습니다.</p>
          <Link
            className="mt-5 block w-full rounded-md bg-[#5b8def] px-4 py-2 text-center text-[13px] font-medium text-white hover:bg-[#4a7ade]"
            to={`/login?redirect=${encodeURIComponent(location.pathname)}`}
          >
            로그인
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen bg-[#0b0d10] text-[13px] text-[#d4d8dd]">
      <aside className="flex w-[220px] shrink-0 flex-col border-r border-[#1f2328] bg-[#0d0f12]">
        <div className="flex h-12 items-center border-b border-[#1f2328] px-4">
          <Link className="font-mono text-[11px] font-semibold tracking-wider text-[#9aa4b2]" to="/batons/models">
            BATON AI LAB
          </Link>
        </div>
        <nav className="flex-1 overflow-y-auto py-3">
          {navGroups.map((group, i) => (
            <div className="mb-3" key={i}>
              {group.label && <div className="px-4 pb-1 text-[10px] font-semibold uppercase tracking-wider text-[#5b6270]">{group.label}</div>}
              {group.items.map((item) => {
                const active = item.to === '/batons/models' ? location.pathname === item.to : location.pathname.startsWith(item.to)
                return (
                  <Link
                    className={`block px-4 py-1.5 text-[13px] ${
                      active ? 'border-l-2 border-[#5b8def] bg-[#151a20] font-medium text-white' : 'border-l-2 border-transparent text-[#9aa4b2] hover:bg-[#12151a] hover:text-[#d4d8dd]'
                    }`}
                    key={item.to}
                    to={item.to}
                  >
                    {item.label}
                  </Link>
                )
              })}
            </div>
          ))}
        </nav>
        <div className="border-t border-[#1f2328] px-4 py-3 text-[11px] text-[#5b6270]">
          내부 도구 · 관리자 전용
          <br />
          <button className="mt-1 text-[#5b8def] hover:underline" onClick={handleLogout} type="button">
            로그아웃
          </button>
          <br />
          <Link className="text-[#5b8def] hover:underline" to="/home">
            ← Baton으로 돌아가기
          </Link>
        </div>
      </aside>
      <main className="min-w-0 flex-1 overflow-x-auto">
        <Outlet />
      </main>
    </div>
  )
}
