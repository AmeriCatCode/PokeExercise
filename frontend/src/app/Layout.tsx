import { Link, NavLink, Outlet } from 'react-router'
import { clearSession, useSession } from '../features/auth/sessionStore'
import { useSessionExpiredRedirect } from '../features/auth/useSessionExpiredRedirect'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-red-700 text-white' : 'text-red-50 hover:bg-red-500'}`

export function Layout() {
  useSessionExpiredRedirect()

  return (
    <div className="min-h-svh bg-slate-50 text-slate-900">
      <header className="bg-red-600 shadow">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-x-4 gap-y-2 px-4 py-3">
          <Link to="/" className="text-xl font-bold text-white">
            Pokédex
          </Link>
          <nav aria-label="Main" className="flex flex-wrap items-center gap-2">
            <NavLink to="/" end className={navLinkClass}>
              Catalog
            </NavLink>
            <AccountMenu />
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}

function AccountMenu() {
  const session = useSession()

  if (!session) {
    return (
      <>
        <NavLink to="/login" className={navLinkClass}>
          Log in
        </NavLink>
        <NavLink to="/register" className={navLinkClass}>
          Register
        </NavLink>
      </>
    )
  }

  return (
    <>
      <NavLink to="/local" className={navLinkClass}>
        My Pokémon
      </NavLink>
      <span className="px-2 text-sm text-red-50">
        Signed in as <strong className="font-semibold text-white">{session.user.username}</strong>
      </span>
      <button
        type="button"
        onClick={clearSession}
        className="rounded-md border border-red-300 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-500"
      >
        Log out
      </button>
    </>
  )
}
