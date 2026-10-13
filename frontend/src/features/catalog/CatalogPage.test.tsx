import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../../test/renderApp'
import { server } from '../../test/server'
import type { PokemonSummary } from './types'

function pokemon(id: number, overrides: Partial<PokemonSummary> = {}): PokemonSummary {
  return {
    id,
    name: `pokemon-${id}`,
    spriteUrl: `https://img.example/${id}.png`,
    category: 'Seed Pokémon',
    types: ['grass', 'poison'],
    weightKg: 6.9,
    abilities: ['overgrow', 'chlorophyll'],
    ...overrides,
  }
}

/** A fake catalog of `totalItems` Pokémon, served 3 per page. */
function catalog({ totalItems = 9, delays = {} as Record<number, number> } = {}) {
  return http.get('/api/pokemon', async ({ request }) => {
    const page = Number(new URL(request.url).searchParams.get('page') ?? 0)
    if (delays[page]) await delay(delays[page])
    const size = 3
    const ids = Array.from({ length: size }, (_, i) => page * size + i + 1).filter((id) => id <= totalItems)
    return HttpResponse.json({
      items: ids.map((id) => pokemon(id)),
      page,
      size,
      totalItems,
      totalPages: Math.ceil(totalItems / size),
    })
  })
}

const cards = () => within(screen.getByRole('list', { name: 'Pokémon' })).getAllByRole('article')

describe('CatalogPage', () => {
  it('renders one card per Pokémon', async () => {
    server.use(catalog())

    renderApp('/')

    expect(await screen.findByRole('heading', { name: 'Pokemon 1' })).toBeInTheDocument()
    expect(cards()).toHaveLength(3)
  })

  it('shows the sprite, category, type badges, weight and abilities on each card', async () => {
    server.use(
      http.get('/api/pokemon', () =>
        HttpResponse.json({
          items: [
            pokemon(1, {
              name: 'bulbasaur',
              category: 'Seed Pokémon',
              types: ['grass', 'poison'],
              weightKg: 6.9,
              abilities: ['overgrow', 'chlorophyll'],
            }),
          ],
          page: 0,
          size: 20,
          totalItems: 1,
          totalPages: 1,
        }),
      ),
    )

    renderApp('/')

    const card = within(await screen.findByRole('article', { name: 'Bulbasaur' }))
    expect(card.getByRole('img', { name: 'Bulbasaur' })).toHaveAttribute('src', 'https://img.example/1.png')
    expect(card.getByText('Seed Pokémon')).toBeInTheDocument()
    const types = card.getByRole('list', { name: 'Types' })
    expect(within(types).getByText('Grass')).toBeInTheDocument()
    expect(within(types).getByText('Poison')).toBeInTheDocument()
    expect(card.getByText('6.9 kg')).toBeInTheDocument()
    expect(card.getByText('Overgrow, Chlorophyll')).toBeInTheDocument()
  })

  it('shows a placeholder when a Pokémon has no sprite', async () => {
    server.use(
      http.get('/api/pokemon', () =>
        HttpResponse.json({
          items: [pokemon(10, { name: 'no-sprite', spriteUrl: null })],
          page: 0,
          size: 20,
          totalItems: 1,
          totalPages: 1,
        }),
      ),
    )

    renderApp('/')

    const card = within(await screen.findByRole('article', { name: 'No Sprite' }))
    expect(card.queryByRole('img')).not.toBeInTheDocument()
    expect(card.getByText('No image')).toBeInTheDocument()
  })

  it('moves between pages and disables "Next" on the last page', async () => {
    const user = userEvent.setup()
    server.use(catalog({ totalItems: 6 }))
    renderApp('/')

    await screen.findByRole('heading', { name: 'Pokemon 1' })
    expect(screen.getByText('Page 1 of 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Next' }))

    expect(await screen.findByRole('heading', { name: 'Pokemon 4' })).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled()
  })

  it('opens the page given in the URL', async () => {
    server.use(catalog())

    renderApp('/?page=3')

    expect(await screen.findByRole('heading', { name: 'Pokemon 7' })).toBeInTheDocument()
    expect(screen.getByText('Page 3 of 3')).toBeInTheDocument()
  })

  it('shows a loading state while the first page loads', async () => {
    server.use(catalog({ delays: { 0: 100 } }))

    renderApp('/')

    expect(screen.getByText('Loading Pokémon…')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Pokemon 1' })).toBeInTheDocument()
    expect(screen.queryByText('Loading Pokémon…')).not.toBeInTheDocument()
  })

  it('shows the error message and lets the user try again', async () => {
    const user = userEvent.setup()
    server.use(
      http.get(
        '/api/pokemon',
        () =>
          HttpResponse.json(
            {
              status: 502,
              message: 'PokeAPI is unavailable, please try again later',
              fieldErrors: [],
              timestamp: '2026-10-12T10:00:00Z',
            },
            { status: 502 },
          ),
        { once: true },
      ),
      catalog(),
    )
    renderApp('/')

    expect(await screen.findByRole('alert')).toHaveTextContent('PokeAPI is unavailable, please try again later')

    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('heading', { name: 'Pokemon 1' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows the page the user ended on when "Next" is clicked quickly, even if answers arrive out of order', async () => {
    const user = userEvent.setup()
    // Page 2 answers after page 3.
    server.use(catalog({ delays: { 1: 300 } }))
    renderApp('/')
    await screen.findByRole('heading', { name: 'Pokemon 1' })

    await user.click(screen.getByRole('button', { name: 'Next' }))
    await user.click(screen.getByRole('button', { name: 'Next' }))

    expect(await screen.findByRole('heading', { name: 'Pokemon 7' })).toBeInTheDocument()
    expect(screen.getByText('Page 3 of 3')).toBeInTheDocument()

    // Wait for the late answer of page 2: the screen must stay on page 3.
    await new Promise((resolve) => setTimeout(resolve, 400))
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Pokemon 7' })).toBeInTheDocument())
    expect(screen.queryByRole('heading', { name: 'Pokemon 4' })).not.toBeInTheDocument()
    expect(screen.getByText('Page 3 of 3')).toBeInTheDocument()
  })
})
