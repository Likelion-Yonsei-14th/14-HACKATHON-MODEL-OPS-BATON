import { Link, Outlet, useLocation } from 'react-router-dom'

// Model Lab is a deliberately different visual register from the rest of the Baton app (spec
// section 19-20): an internal dev tool in the Linear/Vercel/GitHub-Actions register, not the
// customer-facing product — dense, thin borders, small radius, one accent color, no gradients,
// no glow, no hero. It does not reuse AppShell/Header's Figma-derived branding on purpose.

const navGroups: { label: string; items: { label: string; to: string }[] }[] = [
  { label: '', items: [{ label: 'Overview', to: '/batons/models' }] },
  {
    label: 'Models',
    items: [
      { label: 'Classification', to: '/batons/models/classification' },
      { label: 'Generation', to: '/batons/models/generation' },
    ],
  },
  {
    label: '',
    items: [
      { label: 'Prompts', to: '/batons/models/prompts' },
      { label: 'Datasets', to: '/batons/models/datasets' },
      { label: 'Eval Runs', to: '/batons/models/eval-runs' },
      { label: 'Fine-tuning', to: '/batons/models/fine-tuning' },
      { label: 'Deployment', to: '/batons/models/deployment' },
    ],
  },
]

export function ModelLabLayout() {
  const location = useLocation()

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
          Internal tool · admin only
          <br />
          <Link className="text-[#5b8def] hover:underline" to="/home">
            ← Back to Baton
          </Link>
        </div>
      </aside>
      <main className="min-w-0 flex-1 overflow-x-auto">
        <Outlet />
      </main>
    </div>
  )
}
