import { describe, expect, it } from 'vitest'
import { ApiError } from './http'
import { createQueryClient } from './queryClient'

describe('createQueryClient', () => {
  const queries = createQueryClient().getDefaultOptions().queries

  it('treats data as stale immediately, so every screen loads fresh data when it opens', () => {
    expect(queries?.staleTime).toBe(0)
    expect(queries?.refetchOnMount).toBe(true)
  })

  it('reloads data when the user returns to the browser tab', () => {
    expect(queries?.refetchOnWindowFocus).toBe(true)
  })

  describe('retry', () => {
    const retry = queries?.retry as (failureCount: number, error: Error) => boolean

    it('never retries a client error (4xx): the same request would fail again', () => {
      expect(retry(0, new ApiError(400, 'Invalid request', []))).toBe(false)
      expect(retry(0, new ApiError(404, 'Not Found', []))).toBe(false)
    })

    it('retries a server or network error once', () => {
      expect(retry(0, new ApiError(502, 'PokeAPI is unavailable', []))).toBe(true)
      expect(retry(0, new TypeError('Failed to fetch'))).toBe(true)
      expect(retry(1, new ApiError(502, 'PokeAPI is unavailable', []))).toBe(false)
    })
  })
})
