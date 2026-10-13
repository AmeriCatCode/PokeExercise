import { postJson } from '../../api/http'
import type { SessionUser } from './sessionStore'

export type LoginResponse = {
  token: string
  tokenType: 'Bearer'
  expiresAt: string
  user: SessionUser
}

export type Credentials = { username: string; password: string }

export type Registration = Credentials & { email: string }

export function login(credentials: Credentials) {
  return postJson<LoginResponse>('/api/auth/login', credentials)
}

export function register(registration: Registration) {
  return postJson<SessionUser>('/api/auth/register', registration)
}
