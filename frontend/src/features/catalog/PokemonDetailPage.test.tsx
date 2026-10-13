import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { renderApp } from '../../test/renderApp'
import { server } from '../../test/server'
import type { PokemonDetail } from './types'

const bulbasaur: PokemonDetail = {
  id: 1,
  name: 'bulbasaur',
  imageUrl: 'https://img.example/artwork/1.png',
  spriteUrl: 'https://img.example/1.png',
  category: 'Seed Pokémon',
  types: ['grass', 'poison'],
  heightM: 0.7,
  weightKg: 6.9,
  abilities: ['overgrow', 'chlorophyll'],
  stats: [
    { name: 'hp', baseValue: 45 },
    { name: 'attack', baseValue: 49 },
    { name: 'defense', baseValue: 49 },
    { name: 'special-attack', baseValue: 65 },
    { name: 'special-defense', baseValue: 65 },
    { name: 'speed', baseValue: 45 },
  ],
  description: 'A strange seed was planted on its back at birth.',
  evolution: [
    { stage: 1, speciesId: 1, name: 'bulbasaur', evolvesFrom: null },
    { stage: 2, speciesId: 2, name: 'ivysaur', evolvesFrom: 'bulbasaur' },
    { stage: 3, speciesId: 3, name: 'venusaur', evolvesFrom: 'ivysaur' },
  ],
}

const ivysaur: PokemonDetail = {
  ...bulbasaur,
  id: 2,
  name: 'ivysaur',
  imageUrl: 'https://img.example/artwork/2.png',
  description: 'When the bulb on its back grows large, it appears to lose the ability to stand on its hind legs.',
}

function detailApi(...pokemon: PokemonDetail[]) {
  return http.get('/api/pokemon/:idOrName', ({ params }) => {
    const found = pokemon.find((p) => p.name === params.idOrName || String(p.id) === params.idOrName)
    return found
      ? HttpResponse.json(found)
      : HttpResponse.json(
          { status: 404, message: 'Pokémon not found', fieldErrors: [], timestamp: '2026-10-12T10:00:00Z' },
          { status: 404 },
        )
  })
}

describe('PokemonDetailPage', () => {
  it('shows the image, category, types, measurements and abilities', async () => {
    server.use(detailApi(bulbasaur))

    renderApp('/pokemon/bulbasaur')

    expect(await screen.findByRole('heading', { level: 1, name: 'Bulbasaur' })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Bulbasaur' })).toHaveAttribute('src', 'https://img.example/artwork/1.png')
    expect(screen.getByText('#0001')).toBeInTheDocument()
    expect(screen.getByText('Seed Pokémon')).toBeInTheDocument()
    const types = within(screen.getByRole('list', { name: 'Types' }))
    expect(types.getByText('Grass')).toBeInTheDocument()
    expect(types.getByText('Poison')).toBeInTheDocument()
    expect(screen.getByText('0.7 m')).toBeInTheDocument()
    expect(screen.getByText('6.9 kg')).toBeInTheDocument()
    expect(screen.getByText('Overgrow, Chlorophyll')).toBeInTheDocument()
  })

  it('shows the core statistics', async () => {
    server.use(detailApi(bulbasaur))

    renderApp('/pokemon/bulbasaur')

    const stats = within(await screen.findByRole('region', { name: 'Base stats' }))
    for (const [label, value] of [
      ['HP', '45'],
      ['Attack', '49'],
      ['Defense', '49'],
      ['Sp. Atk', '65'],
      ['Sp. Def', '65'],
      ['Speed', '45'],
    ]) {
      expect(stats.getByRole('meter', { name: label })).toHaveAttribute('aria-valuenow', value)
    }
  })

  it('shows the description', async () => {
    server.use(detailApi(bulbasaur))

    renderApp('/pokemon/bulbasaur')

    expect(await screen.findByText('A strange seed was planted on its back at birth.')).toBeInTheDocument()
  })

  it('shows the evolution stages in order and marks the current Pokémon', async () => {
    server.use(detailApi(bulbasaur))

    renderApp('/pokemon/bulbasaur')

    const evolution = within(await screen.findByRole('region', { name: 'Evolution' }))
    const links = evolution.getAllByRole('link')
    expect(links.map((link) => link.textContent)).toEqual(['Bulbasaur', 'Ivysaur', 'Venusaur'])
    expect(evolution.getByRole('link', { name: 'Bulbasaur' })).toHaveAttribute('aria-current', 'page')
    expect(evolution.getByRole('link', { name: 'Ivysaur' })).not.toHaveAttribute('aria-current')
  })

  it('groups branching evolutions in the same stage and says what each one evolves from', async () => {
    server.use(
      detailApi({
        ...bulbasaur,
        id: 265,
        name: 'wurmple',
        evolution: [
          { stage: 1, speciesId: 265, name: 'wurmple', evolvesFrom: null },
          { stage: 2, speciesId: 266, name: 'silcoon', evolvesFrom: 'wurmple' },
          { stage: 2, speciesId: 268, name: 'cascoon', evolvesFrom: 'wurmple' },
          { stage: 3, speciesId: 267, name: 'beautifly', evolvesFrom: 'silcoon' },
          { stage: 3, speciesId: 269, name: 'dustox', evolvesFrom: 'cascoon' },
        ],
      }),
    )

    renderApp('/pokemon/wurmple')

    const evolution = within(await screen.findByRole('region', { name: 'Evolution' }))
    const stage3 = within(evolution.getByRole('list', { name: 'Stage 3' }))
    expect(stage3.getByRole('link', { name: 'Beautifly' })).toBeInTheDocument()
    expect(stage3.getByText('from Silcoon')).toBeInTheDocument()
    expect(stage3.getByRole('link', { name: 'Dustox' })).toBeInTheDocument()
    expect(stage3.getByText('from Cascoon')).toBeInTheDocument()
  })

  it('opens the detail of an evolution when it is clicked', async () => {
    const user = userEvent.setup()
    server.use(detailApi(bulbasaur, ivysaur))
    renderApp('/pokemon/bulbasaur')

    const evolution = within(await screen.findByRole('region', { name: 'Evolution' }))
    await user.click(evolution.getByRole('link', { name: 'Ivysaur' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Ivysaur' })).toBeInTheDocument()
    expect(screen.getByText(ivysaur.description)).toBeInTheDocument()
  })

  it('shows a "not found" message for an unknown Pokémon', async () => {
    server.use(detailApi(bulbasaur))

    renderApp('/pokemon/missingno')

    expect(await screen.findByRole('heading', { name: 'Pokémon not found' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to the catalog' })).toHaveAttribute('href', '/')
  })

  it('shows the error message and lets the user try again when the API fails', async () => {
    const user = userEvent.setup()
    server.use(
      http.get(
        '/api/pokemon/:idOrName',
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
      detailApi(bulbasaur),
    )
    renderApp('/pokemon/bulbasaur')

    expect(await screen.findByRole('alert')).toHaveTextContent('PokeAPI is unavailable, please try again later')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Bulbasaur' })).toBeInTheDocument()
  })
})
