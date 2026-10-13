import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../../test/renderApp'
import { server } from '../../test/server'
import { getSession } from './sessionStore'

const ASH = { id: 1, username: 'ash', email: 'ash@example.com', role: 'USER' }

const registerApi = http.post('/api/auth/register', async ({ request }) => {
  const body = (await request.json()) as { username: string }
  if (body.username === 'gary') {
    return HttpResponse.json(
      {
        status: 409,
        message: 'This username is already taken',
        fieldErrors: [{ field: 'username', message: 'This username is already taken' }],
        timestamp: '',
      },
      { status: 409 },
    )
  }
  return HttpResponse.json(ASH, { status: 201 })
})

const loginApi = http.post('/api/auth/login', () =>
  HttpResponse.json({
    token: 'jwt-token',
    tokenType: 'Bearer',
    expiresAt: new Date(Date.now() + 3600_000).toISOString(),
    user: ASH,
  }),
)

const catalogApi = http.get('/api/pokemon', () =>
  HttpResponse.json({ items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 }),
)

async function fillForm(fields: { username?: string; email?: string; password?: string; confirm?: string }) {
  const user = userEvent.setup()
  if (fields.username) await user.type(screen.getByLabelText('Username'), fields.username)
  if (fields.email) await user.type(screen.getByLabelText('Email'), fields.email)
  if (fields.password) await user.type(screen.getByLabelText('Password'), fields.password)
  if (fields.confirm) await user.type(screen.getByLabelText('Confirm password'), fields.confirm)
  await user.click(screen.getByRole('button', { name: 'Create account' }))
}

/** The validation message shown under a field. */
function errorOf(label: string) {
  const input = screen.getByLabelText(label)
  return within(input.closest('div') as HTMLElement).queryByRole('alert')
}

describe('RegisterPage', () => {
  it('validates the form before sending it', async () => {
    renderApp('/register')

    await fillForm({ username: 'a!', email: 'not-an-email', password: 'short', confirm: 'other' })

    expect(await screen.findByText(/3 to 30 characters/)).toBeInTheDocument()
    expect(errorOf('Email')).toHaveTextContent('Enter a valid email address')
    expect(errorOf('Password')).toHaveTextContent('at least 8 characters')
    expect(errorOf('Confirm password')).toHaveTextContent('The passwords do not match')
  })

  it('asks for a password with letters and digits', async () => {
    renderApp('/register')

    await fillForm({ username: 'ash', email: 'ash@example.com', password: 'onlyletters', confirm: 'onlyletters' })

    expect(await screen.findByText(/at least one letter and one digit/)).toBeInTheDocument()
  })

  it('shows the server error on the field when the username is taken', async () => {
    server.use(registerApi)
    renderApp('/register')

    await fillForm({ username: 'gary', email: 'gary@example.com', password: 'Eevee133', confirm: 'Eevee133' })

    expect(await screen.findByText('This username is already taken')).toBeInTheDocument()
    expect(errorOf('Username')).toHaveTextContent('This username is already taken')
    expect(getSession()).toBeNull()
  })

  it('creates the account and logs the user in', async () => {
    server.use(registerApi, loginApi, catalogApi)
    renderApp('/register')

    await fillForm({ username: 'ash', email: 'ash@example.com', password: 'Pikachu25', confirm: 'Pikachu25' })

    expect(await screen.findByRole('heading', { name: 'Catalog' })).toBeInTheDocument()
    expect(screen.getByRole('banner')).toHaveTextContent('ash')
    expect(getSession()?.user.username).toBe('ash')
  })
})
