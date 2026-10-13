import { act, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { getJson } from '../../api/http'
import { renderApp } from '../../test/renderApp'
import { server } from '../../test/server'
import { getSession, setSession } from './sessionStore'

function logInAsAsh() {
  setSession({
    token: 'jwt-token',
    expiresAt: new Date(Date.now() + 3600_000).toISOString(),
    user: { id: 1, username: 'ash', email: 'ash@example.com', role: 'USER' },
  })
}

const catalogApi = http.get('/api/pokemon', () =>
  HttpResponse.json({ items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 }),
)

describe('session', () => {
  it('shows "Log in" and "Register" to visitors', () => {
    server.use(catalogApi)
    renderApp('/')

    expect(screen.getByRole('link', { name: 'Log in' })).toHaveAttribute('href', '/login')
    expect(screen.getByRole('link', { name: 'Register' })).toHaveAttribute('href', '/register')
  })

  it('keeps the user logged in after a page reload', () => {
    server.use(catalogApi)
    logInAsAsh()

    renderApp('/')

    expect(screen.getByRole('banner')).toHaveTextContent('ash')
  })

  it('logs out: deletes the token and shows "Log in" again', async () => {
    const user = userEvent.setup()
    server.use(catalogApi)
    logInAsAsh()
    renderApp('/')

    await user.click(screen.getByRole('button', { name: 'Log out' }))

    expect(screen.getByRole('link', { name: 'Log in' })).toBeInTheDocument()
    expect(getSession()).toBeNull()
    expect(localStorage.getItem('pokedex.session')).toBeNull()
  })

  it('sends the token on protected requests', async () => {
    logInAsAsh()
    let authorization: string | null = null
    server.use(
      http.get('/api/auth/me', ({ request }) => {
        authorization = request.headers.get('Authorization')
        return HttpResponse.json({ id: 1, username: 'ash', role: 'USER' })
      }),
    )

    await getJson('/api/auth/me', { auth: true })

    expect(authorization).toBe('Bearer jwt-token')
  })

  it('does not send the token on public requests', async () => {
    logInAsAsh()
    let authorization: string | null = 'not checked'
    server.use(
      http.get('/api/pokemon', ({ request }) => {
        authorization = request.headers.get('Authorization')
        return HttpResponse.json({ items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 })
      }),
    )

    await getJson('/api/pokemon')

    expect(authorization).toBeNull()
  })

  it('when the session expires on a protected action, says so and opens the login screen', async () => {
    server.use(
      catalogApi,
      http.get('/api/auth/me', () =>
        HttpResponse.json(
          {
            status: 401,
            message: 'Your session has expired or is invalid, please log in again',
            fieldErrors: [],
            timestamp: '',
          },
          { status: 401 },
        ),
      ),
    )
    logInAsAsh()
    renderApp('/?page=2')

    await act(async () => {
      await getJson('/api/auth/me', { auth: true }).catch(() => undefined)
    })

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Your session expired, please log in again')
    expect(getSession()).toBeNull()
    expect(screen.getByRole('link', { name: 'Log in' })).toBeInTheDocument()
  })
})
