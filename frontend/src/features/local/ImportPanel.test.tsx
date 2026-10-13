import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../../test/renderApp'
import { server } from '../../test/server'
import { errorBody, logInAs, TEST_TOKEN } from '../../test/session'
import type { PokemonDetail } from '../catalog/types'
import type { LocalPokemon } from './types'

const pikachu: PokemonDetail = {
  id: 25,
  name: 'pikachu',
  imageUrl: 'https://img.example/artwork/25.png',
  spriteUrl: 'https://img.example/25.png',
  category: 'Mouse Pokémon',
  types: ['electric'],
  heightM: 0.4,
  weightKg: 6,
  abilities: ['static'],
  stats: [{ name: 'hp', baseValue: 35 }],
  description: 'It stores electricity in its cheeks.',
  evolution: [
    { stage: 1, speciesId: 172, name: 'pichu', evolvesFrom: null },
    { stage: 2, speciesId: 25, name: 'pikachu', evolvesFrom: 'pichu' },
  ],
}

const localPikachu: LocalPokemon = {
  ...pikachu,
  id: 7,
  pokeapiId: 25,
  localName: null,
  localRegion: null,
  tags: [],
  version: 0,
  createdAt: '2026-10-13T10:00:00Z',
  updatedAt: '2026-10-13T10:00:00Z',
}

/**
 * A fake backend with a shared local database: the detail says whether Pikachu is imported (for requests
 * with a token only, like the real API), and importing it changes that.
 */
function backend({ imported = false, importDelay = 0 } = {}) {
  const state = { imported, importRequests: 0, detailTokens: [] as (string | null)[] }
  server.use(
    http.get('/api/pokemon/:idOrName', ({ request }) => {
      const authorization = request.headers.get('Authorization')
      state.detailTokens.push(authorization)
      if (!authorization) return HttpResponse.json(pikachu)
      return HttpResponse.json(
        state.imported ? { ...pikachu, imported: true, localId: 7 } : { ...pikachu, imported: false },
      )
    }),
    http.post('/api/local-pokemon', async ({ request }) => {
      state.importRequests++
      const body = (await request.json()) as { idOrName: string }
      if (importDelay) await delay(importDelay)
      if (body.idOrName !== 'pikachu') return HttpResponse.json(errorBody(404, 'Pokémon not found'), { status: 404 })
      if (state.imported) {
        return HttpResponse.json(errorBody(409, 'This Pokémon is already imported'), { status: 409 })
      }
      state.imported = true
      return HttpResponse.json(localPikachu, { status: 201 })
    }),
    http.get('/api/local-pokemon/:id', () => HttpResponse.json(localPikachu)),
  )
  return state
}

describe('Import on the detail screen', () => {
  it('shows no Import button to visitors, and sends them no token', async () => {
    const api = backend()

    renderApp('/pokemon/pikachu')

    expect(await screen.findByRole('heading', { level: 1, name: 'Pikachu' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Import' })).not.toBeInTheDocument()
    expect(screen.queryByText('Already imported')).not.toBeInTheDocument()
    expect(api.detailTokens).toEqual([null])
  })

  it('shows the Import button to a logged-in user when the Pokémon is not imported', async () => {
    const api = backend()
    logInAs('ash')

    renderApp('/pokemon/pikachu')

    expect(await screen.findByRole('button', { name: 'Import' })).toBeEnabled()
    expect(api.detailTokens).toEqual([`Bearer ${TEST_TOKEN}`])
  })

  it('shows "Already imported" and a link to the local version instead of the button', async () => {
    backend({ imported: true })
    logInAs('ash')

    renderApp('/pokemon/pikachu')

    expect(await screen.findByText('Already imported')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View local version' })).toHaveAttribute('href', '/local/7')
    expect(screen.queryByRole('button', { name: 'Import' })).not.toBeInTheDocument()
  })

  it('imports the Pokémon and then shows it as imported', async () => {
    const user = userEvent.setup()
    const api = backend()
    logInAs('ash')
    renderApp('/pokemon/pikachu')

    await user.click(await screen.findByRole('button', { name: 'Import' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Pikachu was imported')
    expect(await screen.findByText('Already imported')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View local version' })).toHaveAttribute('href', '/local/7')
    expect(api.importRequests).toBe(1)
  })

  it('disables the Import button while importing, so a double click imports once', async () => {
    const user = userEvent.setup()
    const api = backend({ importDelay: 100 })
    logInAs('ash')
    renderApp('/pokemon/pikachu')

    const button = await screen.findByRole('button', { name: 'Import' })
    await user.click(button)

    const pending = screen.getByRole('button', { name: 'Importing…' })
    expect(pending).toBeDisabled()
    await user.click(pending)
    expect(await screen.findByText('Already imported')).toBeInTheDocument()
    expect(api.importRequests).toBe(1)
  })

  it('when someone else imported it first (409), refreshes to "Already imported – View local version"', async () => {
    const user = userEvent.setup()
    const api = backend()
    logInAs('ash')
    renderApp('/pokemon/pikachu')
    await screen.findByRole('button', { name: 'Import' })

    // Another user imports Pikachu while this screen still shows the Import button.
    api.imported = true
    await user.click(screen.getByRole('button', { name: 'Import' }))

    expect(await screen.findByText('Already imported')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View local version' })).toHaveAttribute('href', '/local/7')
    expect(screen.getByRole('status')).toHaveTextContent('Another user imported this Pokémon first')
    expect(screen.queryByRole('button', { name: 'Import' })).not.toBeInTheDocument()
  })

  it('shows the error message when the import fails for another reason', async () => {
    const user = userEvent.setup()
    backend()
    server.use(
      http.post('/api/local-pokemon', () =>
        HttpResponse.json(errorBody(502, 'PokeAPI is unavailable, please try again later'), { status: 502 }),
      ),
    )
    logInAs('ash')
    renderApp('/pokemon/pikachu')

    await user.click(await screen.findByRole('button', { name: 'Import' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('PokeAPI is unavailable, please try again later')
    expect(screen.getByRole('button', { name: 'Import' })).toBeEnabled()
  })

  it('after an import, going back to the catalog detail shows "Already imported"', async () => {
    const user = userEvent.setup()
    backend()
    logInAs('ash')
    const { router } = renderApp('/pokemon/pikachu')

    await user.click(await screen.findByRole('button', { name: 'Import' }))
    await user.click(await screen.findByRole('link', { name: 'View local version' }))
    expect(await screen.findByRole('heading', { name: 'Local data' })).toBeInTheDocument()

    await router.navigate(-1)

    expect(await screen.findByText('Already imported')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Import' })).not.toBeInTheDocument()
  })

  it('a logged-in user whose token is rejected is told the session expired', async () => {
    server.use(
      http.get('/api/pokemon/:idOrName', ({ request }) =>
        request.headers.get('Authorization')
          ? HttpResponse.json(errorBody(401, 'Your session has expired or is invalid, please log in again'), {
              status: 401,
            })
          : HttpResponse.json(pikachu),
      ),
    )
    logInAs('ash')

    renderApp('/pokemon/pikachu')

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Your session expired'))
  })
})
