import { NavLink, Outlet } from 'react-router'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-red-700 text-white' : 'text-red-50 hover:bg-red-500'}`

export function Layout() {
  return (
    <div className="min-h-svh bg-slate-50 text-slate-900">
      <header className="bg-red-600 shadow">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <span className="text-xl font-bold text-white">Pokédex</span>
          <nav aria-label="Main" className="flex gap-2">
            <NavLink to="/" end className={navLinkClass}>
              Catalog
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
