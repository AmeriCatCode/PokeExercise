import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../../test/renderApp'
import { server } from '../../test/server'
import { getSession } from './sessionStore'

const ASH = { id: 1, username: 'ash', email: 'ash@example.com', role: 'USER' }

function loginApi({ wait = 0 } = {}) {
  return http.post('/api/auth/login', async ({ request }) => {
    if (wait) await delay(wait)
    const body = (await request.json()) as { username: string; password: string }
    if (body.username === 'ash' && body.password === 'Pikachu25') {
      return HttpResponse.json({
        token: 'jwt-token',
        tokenType: 'Bearer',
        expiresAt: new Date(Date.now() + 3600_000).toISOString(),
        user: ASH,
      })
    }
    return HttpResponse.json(
      { status: 401, message: 'Invalid username or password', fieldErrors: [], timestamp: '' },
      { status: 401 },
    )
  })
}

const catalogApi = http.get('/api/pokemon', () =>
  HttpResponse.json({ items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 }),
)

describe('LoginPage', () => {
  it('shows a validation message for each empty field', async () => {
    const user = userEvent.setup()
    renderApp('/login')

    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByText('Enter your username')).toBeInTheDocument()
    expect(screen.getByText('Enter your password')).toBeInTheDocument()
  })

  it('logs in, keeps the token and shows the user in the header', async () => {
    const user = userEvent.setup()
    server.use(loginApi(), catalogApi)
    renderApp('/login')

    await user.type(screen.getByLabelText('Username'), 'ash')
    await user.type(screen.getByLabelText('Password'), 'Pikachu25')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByRole('heading', { name: 'Catalog' })).toBeInTheDocument()
    expect(screen.getByRole('banner')).toHaveTextContent('ash')
    expect(screen.getByRole('button', { name: 'Log out' })).toBeInTheDocument()
    expect(getSession()?.token).toBe('jwt-token')
  })

  it('shows an error for wrong credentials and stays on the form', async () => {
    const user = userEvent.setup()
    server.use(loginApi())
    renderApp('/login')

    await user.type(screen.getByLabelText('Username'), 'ash')
    await user.type(screen.getByLabelText('Password'), 'WrongPassword1')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password')
    expect(getSession()).toBeNull()
  })

  it('disables the button while logging in, so the form cannot be sent twice', async () => {
    const user = userEvent.setup()
    server.use(loginApi({ wait: 200 }), catalogApi)
    renderApp('/login')

    await user.type(screen.getByLabelText('Username'), 'ash')
    await user.type(screen.getByLabelText('Password'), 'Pikachu25')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(screen.getByRole('button', { name: 'Logging in…' })).toBeDisabled()
    await waitFor(() => expect(getSession()).not.toBeNull())
  })

  it('links to the registration form', () => {
    renderApp('/login')

    expect(screen.getByRole('link', { name: 'Create an account' })).toHaveAttribute('href', '/register')
  })
})
