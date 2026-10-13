import { useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router'
import type { LoginLocationState } from './LoginPage'
import { onSessionExpired } from './sessionStore'

/**
 * When a protected request finds the session expired, open the login screen with
 * "Your session expired, please log in again", and come back to the same page after logging in.
 */
export function useSessionExpiredRedirect() {
  const navigate = useNavigate()
  const location = useLocation()
  const currentPath = useRef(location.pathname + location.search)

  useEffect(() => {
    currentPath.current = location.pathname + location.search
  }, [location])

  useEffect(
    () =>
      onSessionExpired(() => {
        const state: LoginLocationState = { reason: 'expired', from: currentPath.current }
        void navigate('/login', { state })
      }),
    [navigate],
  )
}
