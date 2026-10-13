import { act, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { focusManager } from '@tanstack/react-query'
import { delay, http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'
import { renderApp } from '../../test/renderApp'
import { server } from '../../test/server'
import { errorBody, logInAs, TEST_TOKEN } from '../../test/session'
import type { LocalPokemon, LocalPokemonSummary } from './types'

function summary(id: number, overrides: Partial<LocalPokemonSummary> = {}): LocalPokemonSummary {
  return {
    id,
    pokeapiId: id * 10,
    name: `pokemon-${id}`,
    spriteUrl: `https://img.example/${id}.png`,
    category: 'Seed Pokémon',
    types: ['grass'],
    localName: null,
    localRegion: null,
    tags: [],
    ...overrides,
  }
}

const eevee: LocalPokemon = {
  id: 7,
  pokeapiId: 133,
  name: 'eevee',
  imageUrl: 'https://img.example/artwork/133.png',
  spriteUrl: 'https://img.example/133.png',
  category: 'Evolution Pokémon',
  types: ['normal'],
  heightM: 0.3,
  weightKg: 6.5,
  abilities: ['run-away', 'adaptability'],
  stats: [{ name: 'hp', baseValue: 55 }],
  description: 'Its genetic code is irregular.',
  evolution: [
    { stage: 1, speciesId: 133, name: 'eevee', evolvesFrom: null },
    { stage: 2, speciesId: 134, name: 'vaporeon', evolvesFrom: 'eevee' },
  ],
  localName: 'Évoli',
  localRegion: 'Kanto',
  tags: ['fan-favorite', 'starter'],
  version: 2,
  createdAt: '2026-10-13T10:00:00Z',
  updatedAt: '2026-10-13T11:00:00Z',
}

/** A fake local list of `totalItems` Pokémon, served 3 per page. Setting `renamed` changes the data served from then on. */
function localList({ totalItems = 9, delays = {} as Record<number, number> } = {}) {
  const state = { calls: 0, renamed: false, tokens: [] as (string | null)[] }
  server.use(
    http.get('/api/local-pokemon', async ({ request }) => {
      state.calls++
      state.tokens.push(request.headers.get('Authorization'))
      const page = Number(new URL(request.url).searchParams.get('page') ?? 0)
      if (delays[page]) await delay(delays[page])
      const size = 3
      const ids = Array.from({ length: size }, (_, i) => page * size + i + 1).filter((id) => id <= totalItems)
      return HttpResponse.json({
        items: ids.map((id) => summary(id, state.renamed ? { localName: `Renamed ${id}` } : {})),
        page,
        size,
        totalItems,
        totalPages: Math.ceil(totalItems / size),
      })
    }),
  )
  return state
}

afterEach(() => {
  focusManager.setFocused(undefined)
})

describe('My local Pokémon (list)', () => {
  it('opens the login screen for visitors', async () => {
    renderApp('/local')

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
  })

  it('when the session has expired, says so and opens the login screen', async () => {
    server.use(
      http.get('/api/local-pokemon', () =>
        HttpResponse.json(errorBody(401, 'Your session has expired or is invalid, please log in again'), {
          status: 401,
        }),
      ),
    )
    logInAs('ash')

    renderApp('/local')

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Your session expired, please log in again')
  })

  it('is reachable from the menu once logged in', async () => {
    const user = userEvent.setup()
    localList()
    server.use(
      http.get('/api/pokemon', () =>
        HttpResponse.json({ items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 }),
      ),
    )
    logInAs('ash')
    renderApp('/')

    await user.click(screen.getByRole('link', { name: 'My Pokémon' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'My local Pokémon' })).toBeInTheDocument()
  })

  it('lists the local Pokémon with their local data, sending the token', async () => {
    server.use(
      http.get('/api/local-pokemon', () =>
        HttpResponse.json({
          items: [
            summary(7, {
              pokeapiId: 133,
              name: 'eevee',
              category: 'Evolution Pokémon',
              types: ['normal'],
              localName: 'Évoli',
              localRegion: 'Kanto',
              tags: ['starter'],
            }),
          ],
          page: 0,
          size: 20,
          totalItems: 1,
          totalPages: 1,
        }),
      ),
    )
    logInAs('ash')

    renderApp('/local')

    const card = within(await screen.findByRole('article', { name: 'Eevee' }))
    expect(card.getByText('#0133')).toBeInTheDocument()
    expect(card.getByText('Évoli · Kanto')).toBeInTheDocument()
    expect(card.getByText('Evolution Pokémon')).toBeInTheDocument()
    expect(within(card.getByRole('list', { name: 'Types' })).getByText('Normal')).toBeInTheDocument()
    expect(within(card.getByRole('list', { name: 'Tags' })).getByText('starter')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Eevee/ })).toHaveAttribute('href', '/local/7')
  })

  it('sends the login token', async () => {
    const api = localList()
    logInAs('ash')

    renderApp('/local')

    await screen.findByRole('heading', { name: 'Pokemon 1' })
    expect(api.tokens).toEqual([`Bearer ${TEST_TOKEN}`])
  })

  it('says so when nothing is imported yet', async () => {
    localList({ totalItems: 0 })
    logInAs('ash')

    renderApp('/local')

    expect(await screen.findByText(/No Pokémon imported yet/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Browse the catalog' })).toHaveAttribute('href', '/')
  })

  it('moves between pages', async () => {
    const user = userEvent.setup()
    localList({ totalItems: 6 })
    logInAs('ash')
    renderApp('/local')

    await screen.findByRole('heading', { name: 'Pokemon 1' })
    await user.click(screen.getByRole('button', { name: 'Next' }))

    expect(await screen.findByRole('heading', { name: 'Pokemon 4' })).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  })

  it('after rapid clicks on "Next", a late answer for an earlier page never replaces the last page asked for', async () => {
    const user = userEvent.setup()
    // Page 2 (index 1) answers after page 3 (index 2).
    localList({ totalItems: 9, delays: { 1: 300 } })
    logInAs('ash')
    renderApp('/local')
    await screen.findByRole('heading', { name: 'Pokemon 1' })

    await user.click(screen.getByRole('button', { name: 'Next' }))
    await user.click(screen.getByRole('button', { name: 'Next' }))

    expect(await screen.findByRole('heading', { name: 'Pokemon 7' })).toBeInTheDocument()
    await act(() => delay(400))
    expect(screen.getByRole('heading', { name: 'Pokemon 7' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Pokemon 4' })).not.toBeInTheDocument()
    expect(screen.getByText('Page 3 of 3')).toBeInTheDocument()
  })

  it('reloads the data when the user returns to the browser tab', async () => {
    const api = localList({ totalItems: 3 })
    logInAs('ash')
    renderApp('/local')
    await screen.findByRole('heading', { name: 'Pokemon 1' })

    api.renamed = true
    act(() => {
      focusManager.setFocused(false)
      focusManager.setFocused(true)
    })

    expect(await screen.findByText('Renamed 1')).toBeInTheDocument()
  })

  it('loads fresh data when going back to the list', async () => {
    const user = userEvent.setup()
    const api = localList({ totalItems: 3 })
    server.use(http.get('/api/local-pokemon/:id', () => HttpResponse.json(eevee)))
    logInAs('ash')
    const { router } = renderApp('/local')
    await screen.findByRole('heading', { name: 'Pokemon 1' })

    await user.click(screen.getByRole('link', { name: /Pokemon 1/ }))
    await screen.findByRole('heading', { name: 'Local data' })
    api.renamed = true
    await router.navigate(-1)

    expect(await screen.findByText('Renamed 1')).toBeInTheDocument()
  })
})

describe('Local Pokémon (detail)', () => {
  it('shows the local data and the PokeAPI data', async () => {
    server.use(http.get('/api/local-pokemon/7', () => HttpResponse.json(eevee)))
    logInAs('ash')

    renderApp('/local/7')

    const local = within(await screen.findByRole('region', { name: 'Local data' }))
    expect(local.getByText('Évoli')).toBeInTheDocument()
    expect(local.getByText('Kanto')).toBeInTheDocument()
    expect(within(local.getByRole('list', { name: 'Tags' })).getAllByRole('listitem').map((t) => t.textContent)).toEqual(
      ['fan-favorite', 'starter'],
    )
    expect(screen.getByRole('heading', { level: 1, name: 'Eevee' })).toBeInTheDocument()
    expect(screen.getByText('#0133')).toBeInTheDocument()
    expect(screen.getByText('Its genetic code is irregular.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View in catalog' })).toHaveAttribute('href', '/pokemon/eevee')
  })

  it('says when the proprietary fields are not set yet', async () => {
    server.use(
      http.get('/api/local-pokemon/7', () =>
        HttpResponse.json({ ...eevee, localName: null, localRegion: null, tags: [] }),
      ),
    )
    logInAs('ash')

    renderApp('/local/7')

    const local = within(await screen.findByRole('region', { name: 'Local data' }))
    expect(local.getAllByText('Not set')).toHaveLength(2)
    expect(local.getByText('No tags')).toBeInTheDocument()
  })

  it('a local Pokémon deleted by another user (404) says so and links back to the list', async () => {
    server.use(
      http.get('/api/local-pokemon/7', () =>
        HttpResponse.json(errorBody(404, 'This Pokémon no longer exists in the local database'), { status: 404 }),
      ),
    )
    logInAs('ash')

    renderApp('/local/7')

    expect(await screen.findByRole('heading', { name: 'This Pokémon was deleted by another user' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to my Pokémon' })).toHaveAttribute('href', '/local')
  })

  it('an id that is not a number is "not found" without calling the API', async () => {
    logInAs('ash')

    renderApp('/local/abc')

    expect(await screen.findByRole('heading', { name: 'Pokémon not found' })).toBeInTheDocument()
  })

  it('goes back to the list page the user came from', async () => {
    const user = userEvent.setup()
    localList({ totalItems: 6 })
    server.use(http.get('/api/local-pokemon/:id', () => HttpResponse.json(eevee)))
    logInAs('ash')
    renderApp('/local?page=2')

    await user.click(await screen.findByRole('link', { name: /Pokemon 4/ }))
    await user.click(await screen.findByRole('button', { name: 'Back to my Pokémon' }))

    expect(await screen.findByRole('heading', { name: 'Pokemon 4' })).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
  })
})
