import { act, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { focusManager } from '@tanstack/react-query'
import { delay, http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'
import { renderApp } from '../../test/renderApp'
import { server } from '../../test/server'
import { errorBody, logInAs } from '../../test/session'
import type { LocalPokemon, LocalPokemonSummary } from './types'

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
  abilities: ['run-away'],
  stats: [{ name: 'hp', baseValue: 55 }],
  description: 'Its genetic code is irregular.',
  evolution: [{ stage: 1, speciesId: 133, name: 'eevee', evolvesFrom: null }],
  localName: 'Évoli',
  localRegion: 'Kanto',
  tags: ['starter'],
  version: 3,
  createdAt: '2026-10-13T10:00:00Z',
  updatedAt: '2026-10-13T11:00:00Z',
}

type UpdateBody = { localName: string | null; localRegion: string | null; tags: string[]; version: number }

/**
 * A fake backend holding Eevee in a shared local database, with the real version rules: a save or delete
 * with another version gets 409, and a missing Pokémon 404.
 */
function backend({ saveDelay = 0, deleteDelay = 0 } = {}) {
  const db = {
    pokemon: { ...eevee } as LocalPokemon | null,
    updates: [] as UpdateBody[],
    deletes: [] as number[],
    /** Another user saves a change. */
    changeByAnotherUser(change: Partial<LocalPokemon>) {
      if (db.pokemon) db.pokemon = { ...db.pokemon, ...change, version: db.pokemon.version + 1 }
    },
  }
  const summary = (p: LocalPokemon): LocalPokemonSummary => ({
    id: p.id,
    pokeapiId: p.pokeapiId,
    name: p.name,
    spriteUrl: p.spriteUrl,
    category: p.category,
    types: p.types,
    localName: p.localName,
    localRegion: p.localRegion,
    tags: p.tags,
  })
  const notFound = () =>
    HttpResponse.json(errorBody(404, 'This Pokémon no longer exists in the local database'), { status: 404 })
  const conflict = () =>
    HttpResponse.json(errorBody(409, 'This Pokémon was changed by someone else, please reload it'), { status: 409 })

  server.use(
    http.get('/api/local-pokemon', () => {
      const items = db.pokemon ? [summary(db.pokemon)] : []
      return HttpResponse.json({ items, page: 0, size: 20, totalItems: items.length, totalPages: 1 })
    }),
    http.get('/api/local-pokemon/7', () => (db.pokemon ? HttpResponse.json(db.pokemon) : notFound())),
    http.put('/api/local-pokemon/7', async ({ request }) => {
      const body = (await request.json()) as UpdateBody
      db.updates.push(body)
      if (saveDelay) await delay(saveDelay)
      if (!db.pokemon) return notFound()
      if (body.version !== db.pokemon.version) return conflict()
      db.pokemon = { ...db.pokemon, ...body, version: db.pokemon.version + 1 }
      return HttpResponse.json(db.pokemon)
    }),
    http.delete('/api/local-pokemon/7', async ({ request }) => {
      const version = Number(new URL(request.url).searchParams.get('version'))
      db.deletes.push(version)
      if (deleteDelay) await delay(deleteDelay)
      if (!db.pokemon) return notFound()
      if (version !== db.pokemon.version) return conflict()
      db.pokemon = null
      return new HttpResponse(null, { status: 204 })
    }),
    http.get('/api/pokemon/:idOrName', () =>
      HttpResponse.json({ ...eevee, id: 133, imported: db.pokemon !== null, localId: db.pokemon ? 7 : undefined }),
    ),
  )
  return db
}

async function openEditForm() {
  const user = userEvent.setup()
  await user.click(await screen.findByRole('button', { name: 'Edit' }))
  return user
}

const field = (label: string) => screen.getByRole('textbox', { name: label })

afterEach(() => {
  focusManager.setFocused(undefined)
})

describe('Editing a local Pokémon', () => {
  it('opens the form with the current values', async () => {
    backend()
    logInAs('ash')
    renderApp('/local/7')

    await openEditForm()

    expect(field('Local name')).toHaveValue('Évoli')
    expect(field('Local region')).toHaveValue('Kanto')
    expect(field('Tags')).toHaveValue('starter')
  })

  it('validates the form before sending it', async () => {
    const db = backend()
    logInAs('ash')
    renderApp('/local/7')
    const user = await openEditForm()

    await user.clear(field('Local name'))
    await user.type(field('Local name'), 'x'.repeat(101))
    await user.clear(field('Tags'))
    await user.type(field('Tags'), 'starter, Starter')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Use at most 100 characters')).toBeInTheDocument()
    expect(screen.getByText('Each tag only once ("starter")')).toBeInTheDocument()
    expect(db.updates).toEqual([])
  })

  it('saves the new values with the version that was loaded, and shows them', async () => {
    const db = backend()
    logInAs('ash')
    renderApp('/local/7')
    const user = await openEditForm()

    await user.clear(field('Local name'))
    await user.type(field('Local name'), 'Évoli de Sacha')
    await user.clear(field('Local region'))
    await user.clear(field('Tags'))
    await user.type(field('Tags'), 'Starter,  fan-favorite , ')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Changes saved')
    expect(db.updates).toEqual([
      { localName: 'Évoli de Sacha', localRegion: null, tags: ['starter', 'fan-favorite'], version: 3 },
    ])
    const local = within(screen.getByRole('region', { name: 'Local data' }))
    expect(local.getByText('Évoli de Sacha')).toBeInTheDocument()
    expect(local.getByText('Not set')).toBeInTheDocument()
    expect(within(local.getByRole('list', { name: 'Tags' })).getByText('fan-favorite')).toBeInTheDocument()
  })

  it('disables Save while saving, so a double click saves once', async () => {
    const db = backend({ saveDelay: 100 })
    logInAs('ash')
    renderApp('/local/7')
    const user = await openEditForm()

    await user.click(screen.getByRole('button', { name: 'Save' }))
    const saving = screen.getByRole('button', { name: 'Saving…' })
    expect(saving).toBeDisabled()
    await user.click(saving)

    expect(await screen.findByRole('status')).toHaveTextContent('Changes saved')
    expect(db.updates).toHaveLength(1)
  })

  it('when someone else saved first (409), says so; Reload shows their change and the next save works', async () => {
    const db = backend()
    logInAs('ash')
    renderApp('/local/7')
    const user = await openEditForm()
    await user.clear(field('Local name'))
    await user.type(field('Local name'), "Ben's name")

    // Ana saves a change while Ben's form is open.
    db.changeByAnotherUser({ localName: "Ana's name" })
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'This Pokémon was changed by someone else. Reload it to see the changes, then apply yours again.',
    )
    await user.click(screen.getByRole('button', { name: 'Reload' }))

    expect(await screen.findByDisplayValue("Ana's name")).toBeInTheDocument()
    await user.type(field('Local region'), ' Plains')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Changes saved')
    expect(db.updates.at(-1)).toMatchObject({ localName: "Ana's name", localRegion: 'Kanto Plains', version: 4 })
  })

  it('when the Pokémon was deleted meanwhile (404), says it no longer exists', async () => {
    const db = backend()
    logInAs('ash')
    renderApp('/local/7')
    const user = await openEditForm()

    db.pokemon = null
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'This Pokémon no longer exists: another user deleted it.',
    )
    expect(screen.getByRole('link', { name: 'Back to my Pokémon' })).toHaveAttribute('href', '/local')
  })

  it("a reload while the form is open doesn't change the form's values or version", async () => {
    const db = backend()
    logInAs('ash')
    renderApp('/local/7')
    const user = await openEditForm()
    await user.clear(field('Local name'))
    await user.type(field('Local name'), 'Typing in progress')

    // Ana's change arrives with a background reload (the user came back to the tab).
    db.changeByAnotherUser({ localName: "Ana's name", localRegion: 'Johto' })
    act(() => {
      focusManager.setFocused(false)
      focusManager.setFocused(true)
    })
    await act(() => delay(50))

    expect(field('Local name')).toHaveValue('Typing in progress')
    expect(field('Local region')).toHaveValue('Kanto')
    await user.click(screen.getByRole('button', { name: 'Save' }))
    // Still the version the form was opened with: the server detects the conflict.
    expect(await screen.findByRole('alert')).toHaveTextContent('changed by someone else')
    expect(db.updates[0].version).toBe(3)
  })

  it('Cancel closes the form without saving', async () => {
    const db = backend()
    logInAs('ash')
    renderApp('/local/7')
    const user = await openEditForm()

    await user.type(field('Local name'), ' changed')
    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('textbox', { name: 'Local name' })).not.toBeInTheDocument()
    expect(within(screen.getByRole('region', { name: 'Local data' })).getByText('Évoli')).toBeInTheDocument()
    expect(db.updates).toEqual([])
  })

  it('after a save, going back to the local list shows the new values', async () => {
    backend()
    logInAs('ash')
    renderApp('/local/7')
    const user = await openEditForm()
    await user.clear(field('Local name'))
    await user.type(field('Local name'), 'Évoli de Sacha')
    await user.click(screen.getByRole('button', { name: 'Save' }))
    await screen.findByRole('status')

    await user.click(screen.getByRole('button', { name: 'Back to my Pokémon' }))

    expect(await screen.findByText('Évoli de Sacha · Kanto')).toBeInTheDocument()
  })
})

describe('Deleting a local Pokémon', () => {
  it('asks for confirmation; Cancel keeps it', async () => {
    const user = userEvent.setup()
    const db = backend()
    logInAs('ash')
    renderApp('/local/7')

    await user.click(await screen.findByRole('button', { name: 'Delete' }))
    expect(screen.getByText('Delete Eevee from the local database?')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByText('Delete Eevee from the local database?')).not.toBeInTheDocument()
    expect(db.deletes).toEqual([])
  })

  it('deletes it with the loaded version, then the local list no longer shows it', async () => {
    const user = userEvent.setup()
    const db = backend()
    logInAs('ash')
    renderApp('/local/7')

    await user.click(await screen.findByRole('button', { name: 'Delete' }))
    await user.click(screen.getByRole('button', { name: 'Yes, delete' }))

    expect(await screen.findByRole('heading', { name: 'My local Pokémon' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Eevee was deleted.')
    expect(await screen.findByText('No Pokémon imported yet.')).toBeInTheDocument()
    expect(db.deletes).toEqual([3])
  })

  it('disables Delete while deleting, so a double click deletes once', async () => {
    const user = userEvent.setup()
    const db = backend({ deleteDelay: 100 })
    logInAs('ash')
    renderApp('/local/7')

    await user.click(await screen.findByRole('button', { name: 'Delete' }))
    await user.click(screen.getByRole('button', { name: 'Yes, delete' }))
    const deleting = screen.getByRole('button', { name: 'Deleting…' })
    expect(deleting).toBeDisabled()
    await user.click(deleting)

    expect(await screen.findByRole('heading', { name: 'My local Pokémon' })).toBeInTheDocument()
    expect(db.deletes).toHaveLength(1)
  })

  it('when someone else changed it (409), says so and Reload shows the change', async () => {
    const user = userEvent.setup()
    const db = backend()
    logInAs('ash')
    renderApp('/local/7')
    await user.click(await screen.findByRole('button', { name: 'Delete' }))

    db.changeByAnotherUser({ localName: "Ana's name" })
    await user.click(screen.getByRole('button', { name: 'Yes, delete' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'This Pokémon was changed by someone else. Reload it to see the changes before deleting it.',
    )
    await user.click(screen.getByRole('button', { name: 'Reload' }))

    const local = within(screen.getByRole('region', { name: 'Local data' }))
    expect(await local.findByText("Ana's name")).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(db.pokemon).not.toBeNull()
  })

  it('deletes the version the user saw when asking to delete, even if a reload brings a newer one meanwhile', async () => {
    const user = userEvent.setup()
    const db = backend()
    logInAs('ash')
    renderApp('/local/7')
    await user.click(await screen.findByRole('button', { name: 'Delete' }))

    // Ana saves a change; it arrives with a background reload while the confirmation is open.
    db.changeByAnotherUser({ localName: "Ana's name" })
    act(() => {
      focusManager.setFocused(false)
      focusManager.setFocused(true)
    })
    await within(screen.getByRole('region', { name: 'Local data' })).findByText("Ana's name")
    await user.click(screen.getByRole('button', { name: 'Yes, delete' }))

    // The user decided on version 3, never confirmed deleting Ana's change: 409, nothing deleted.
    expect(await screen.findByRole('alert')).toHaveTextContent('changed by someone else')
    expect(db.deletes).toEqual([3])
    expect(db.pokemon).not.toBeNull()
  })

  it('when another user already deleted it (404), treats it as deleted and goes to the list', async () => {
    const user = userEvent.setup()
    const db = backend()
    logInAs('ash')
    renderApp('/local/7')
    await user.click(await screen.findByRole('button', { name: 'Delete' }))

    db.pokemon = null
    await user.click(screen.getByRole('button', { name: 'Yes, delete' }))

    expect(await screen.findByRole('heading', { name: 'My local Pokémon' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Eevee had already been deleted by another user.')
  })

  it('after a delete, the catalog detail shows the Import button again', async () => {
    const user = userEvent.setup()
    backend()
    logInAs('ash')
    const { router } = renderApp('/local/7')

    await user.click(await screen.findByRole('button', { name: 'Delete' }))
    await user.click(screen.getByRole('button', { name: 'Yes, delete' }))
    await screen.findByRole('heading', { name: 'My local Pokémon' })
    await act(() => router.navigate('/pokemon/eevee'))

    expect(await screen.findByRole('button', { name: 'Import' })).toBeInTheDocument()
  })
})
