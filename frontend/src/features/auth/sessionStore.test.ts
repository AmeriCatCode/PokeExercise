import { afterEach, describe, expect, it, vi } from 'vitest'
import { clearSession, getSession, onSessionExpired, sessionExpired, setSession, type Session } from './sessionStore'

const inOneHour = () => new Date(Date.now() + 60 * 60 * 1000).toISOString()

function session(overrides: Partial<Session> = {}): Session {
  return {
    token: 'jwt-token',
    expiresAt: inOneHour(),
    user: { id: 1, username: 'ash', email: 'ash@example.com', role: 'USER' },
    ...overrides,
  }
}

describe('sessionStore', () => {
  afterEach(() => clearSession())

  it('keeps the session in the browser so it survives a page reload', () => {
    setSession(session())

    expect(getSession()?.user.username).toBe('ash')
    expect(JSON.parse(localStorage.getItem('pokedex.session') ?? 'null')).toMatchObject({ token: 'jwt-token' })
  })

  it('deletes the token from the browser on logout', () => {
    setSession(session())

    clearSession()

    expect(getSession()).toBeNull()
    expect(localStorage.getItem('pokedex.session')).toBeNull()
  })

  it('treats an already expired session as logged out', () => {
    setSession(session({ expiresAt: new Date(Date.now() - 1000).toISOString() }))

    expect(getSession()).toBeNull()
  })

  it('notifies the listeners when the session expires, and clears it', () => {
    const listener = vi.fn()
    const unsubscribe = onSessionExpired(listener)
    setSession(session())

    sessionExpired()

    expect(listener).toHaveBeenCalledOnce()
    expect(getSession()).toBeNull()
    unsubscribe()
  })
})
