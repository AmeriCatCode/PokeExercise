import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../test/server'
import { ApiError, getJson } from './http'

describe('getJson', () => {
  it('returns the parsed body of a successful response', async () => {
    server.use(http.get('/api/thing', () => HttpResponse.json({ name: 'pikachu' })))

    await expect(getJson('/api/thing')).resolves.toEqual({ name: 'pikachu' })
  })

  it('sends the query parameters', async () => {
    server.use(
      http.get('/api/thing', ({ request }) =>
        HttpResponse.json({ query: new URL(request.url).searchParams.toString() }),
      ),
    )

    await expect(getJson('/api/thing', { params: { page: 2, size: 20 } })).resolves.toEqual({
      query: 'page=2&size=20',
    })
  })

  it('throws an ApiError with the status, message and field errors of the common error body', async () => {
    server.use(
      http.get('/api/thing', () =>
        HttpResponse.json(
          {
            status: 400,
            message: 'Invalid request',
            fieldErrors: [{ field: 'size', message: 'must be less than or equal to 50' }],
            timestamp: '2026-10-12T10:00:00Z',
          },
          { status: 400 },
        ),
      ),
    )

    const error = await getJson('/api/thing').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      message: 'Invalid request',
      fieldErrors: [{ field: 'size', message: 'must be less than or equal to 50' }],
    })
  })

  it('throws an ApiError with a generic message when the error body is not JSON', async () => {
    server.use(http.get('/api/thing', () => new HttpResponse('Bad gateway', { status: 502 })))

    await expect(getJson('/api/thing')).rejects.toMatchObject({
      status: 502,
      message: 'Something went wrong, please try again',
    })
  })
})
