import { getSession, sessionExpired } from '../features/auth/sessionStore'

export type FieldError = { field: string; message: string }

/** The common error body returned by every endpoint of the API. */
type ErrorBody = { status: number; message: string; fieldErrors: FieldError[]; timestamp: string }

/** An error answer from our API, with the message ready to show to the user. */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: FieldError[]

  constructor(status: number, message: string, fieldErrors: FieldError[]) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

const GENERIC_ERROR = 'Something went wrong, please try again'

type RequestOptions = {
  params?: Record<string, string | number>
  signal?: AbortSignal
  /**
   * A protected request: sends the login token. If the API then answers 401, the session has
   * expired: it is cleared and the app opens the login screen.
   * Public requests never send the token, so an expired one can't break them.
   */
  auth?: boolean
}

export function getJson<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return request<T>('GET', path, undefined, options)
}

export function postJson<T>(path: string, body: unknown, options: RequestOptions = {}): Promise<T> {
  return request<T>('POST', path, body, options)
}

async function request<T>(method: string, path: string, body: unknown, options: RequestOptions): Promise<T> {
  // Absolute URL on the current origin: works in the browser and in tests (Node's fetch needs one).
  const url = new URL(path, window.location.origin)
  for (const [key, value] of Object.entries(options.params ?? {})) {
    url.searchParams.set(key, String(value))
  }

  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = options.auth ? getSession()?.token : undefined
  if (token) headers.Authorization = `Bearer ${token}`

  const response = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: options.signal,
  })
  if (!response.ok) {
    if (response.status === 401 && options.auth) {
      sessionExpired()
    }
    throw await toApiError(response)
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as Partial<ErrorBody>
    if (typeof body.message === 'string') {
      return new ApiError(response.status, body.message, body.fieldErrors ?? [])
    }
  } catch {
    // Not our JSON error body (e.g. a proxy error page): fall back to a generic message.
  }
  return new ApiError(response.status, GENERIC_ERROR, [])
}
