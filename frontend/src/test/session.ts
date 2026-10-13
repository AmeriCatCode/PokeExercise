import { setSession } from '../features/auth/sessionStore'

export const TEST_TOKEN = 'jwt-token'

/** Starts the test logged in, as if the user had logged in before. */
export function logInAs(username = 'ash') {
  setSession({
    token: TEST_TOKEN,
    expiresAt: new Date(Date.now() + 3600_000).toISOString(),
    user: { id: 1, username, email: `${username}@example.com`, role: 'USER' },
  })
}

/** The common error body of the API. */
export function errorBody(status: number, message: string) {
  return { status, message, fieldErrors: [], timestamp: '2026-10-13T10:00:00Z' }
}
