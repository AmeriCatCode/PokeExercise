import { describe, expect, it } from 'vitest'
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
})
