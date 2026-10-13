import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { clearSession } from '../features/auth/sessionStore'
import { server } from './server'

// Any request without a handler fails the test, so no test silently hits a real API.
beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  cleanup()
  server.resetHandlers()
  // Every test starts logged out.
  clearSession()
  localStorage.clear()
})
afterAll(() => server.close())
