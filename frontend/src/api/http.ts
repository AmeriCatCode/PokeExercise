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
}

export async function getJson<T>(path: string, { params, signal }: RequestOptions = {}): Promise<T> {
  // Absolute URL on the current origin: works in the browser and in tests (Node's fetch needs one).
  const url = new URL(path, window.location.origin)
  for (const [key, value] of Object.entries(params ?? {})) {
    url.searchParams.set(key, String(value))
  }

  const response = await fetch(url, { headers: { Accept: 'application/json' }, signal })
  if (!response.ok) {
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
