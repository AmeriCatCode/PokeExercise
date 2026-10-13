import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import type { LoginLocationState } from './LoginPage'
import { sessionHasExpired, useSession } from './sessionStore'

/** A protected screen: visitors are sent to the login screen, and come back here after logging in. */
export function RequireSession({ children }: { children: ReactNode }) {
  const session = useSession()
  const location = useLocation()

  if (!session) {
    // Same state as the "session expired" redirect, so the message survives whichever redirect runs last.
    const state: LoginLocationState = {
      from: location.pathname + location.search,
      reason: sessionHasExpired() ? 'expired' : undefined,
    }
    return <Navigate to="/login" replace state={state} />
  }
  return children
}
