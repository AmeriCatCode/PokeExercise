import { useSyncExternalStore } from 'react'

export type SessionUser = { id: number; username: string; email: string; role: string }

export type Session = {
  token: string
  /** ISO date; after it, the API rejects the token. */
  expiresAt: string
  user: SessionUser
}

/**
 * The logged-in user and their JWT, kept in localStorage so a page reload keeps the session.
 * Logging out deletes it from the browser (the server does not keep sessions; a token stays valid until it
 * expires, which the short expiry time limits).
 */
const STORAGE_KEY = 'pokedex.session'

const listeners = new Set<() => void>()
const expiredListeners = new Set<() => void>()
let current: Session | null = load()

function isActive(session: Session | null): session is Session {
  return session !== null && Date.parse(session.expiresAt) > Date.now()
}

function load(): Session | null {
  try {
    const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null') as Session | null
    return isActive(stored) ? stored : null
  } catch {
    return null
  }
}

function persist(session: Session | null) {
  try {
    if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Storage unavailable (private mode): the session still works until the page is closed.
  }
}

function notify() {
  listeners.forEach((listener) => listener())
}

/** The active session, or `null` when logged out or when the token has expired. */
export function getSession(): Session | null {
  return isActive(current) ? current : null
}

export function setSession(session: Session) {
  current = session
  persist(session)
  notify()
}

/** Logout: forget the token. */
export function clearSession() {
  current = null
  persist(null)
  notify()
}

/** The API rejected the token: forget it and tell the app, which opens the login screen. */
export function sessionExpired() {
  clearSession()
  expiredListeners.forEach((listener) => listener())
}

export function onSessionExpired(listener: () => void): () => void {
  expiredListeners.add(listener)
  return () => expiredListeners.delete(listener)
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

/** The current session, re-rendering the component when it changes. */
export function useSession(): Session | null {
  return useSyncExternalStore(subscribe, getSession)
}
