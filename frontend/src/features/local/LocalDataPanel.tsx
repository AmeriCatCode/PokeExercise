import { useId, useState } from 'react'
import { Link } from 'react-router'
import { DeleteLocalPokemon } from './DeleteLocalPokemon'
import { LocalDataForm } from './LocalDataForm'
import { TagList } from './TagList'
import type { LocalPokemon } from './types'

type LocalDataPanelProps = {
  pokemon: LocalPokemon
  /** Loads the latest version of the Pokémon (after a 409). */
  onReload: () => Promise<LocalPokemon | undefined>
}

/** The proprietary fields of a local Pokémon, with Edit and Delete. */
export function LocalDataPanel({ pokemon, onReload }: LocalDataPanelProps) {
  const headingId = useId()
  const [editing, setEditing] = useState(false)
  const [saved, setSaved] = useState(false)
  const notSet = <span className="text-slate-400">Not set</span>

  return (
    <section
      aria-labelledby={headingId}
      className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 text-left"
    >
      <h2 id={headingId} className="text-lg font-semibold">
        Local data
      </h2>

      {editing ? (
        <LocalDataForm
          pokemon={pokemon}
          onReload={onReload}
          onCancel={() => setEditing(false)}
          onSaved={() => {
            setEditing(false)
            setSaved(true)
          }}
        />
      ) : (
        <>
          <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-sm">
            <dt className="font-medium text-slate-500">Local name</dt>
            <dd className="break-words">{pokemon.localName ?? notSet}</dd>
            <dt className="font-medium text-slate-500">Local region</dt>
            <dd className="break-words">{pokemon.localRegion ?? notSet}</dd>
            <dt className="font-medium text-slate-500">Tags</dt>
            <dd>
              {pokemon.tags.length > 0 ? (
                <TagList tags={pokemon.tags} align="start" />
              ) : (
                <span className="text-slate-400">No tags</span>
              )}
            </dd>
            <dt className="font-medium text-slate-500">Imported</dt>
            <dd>{new Date(pokemon.createdAt).toLocaleDateString()}</dd>
          </dl>
          {saved && (
            <p role="status" className="text-sm text-green-800">
              Changes saved.
            </p>
          )}
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => {
                setSaved(false)
                setEditing(true)
              }}
              className="rounded-md bg-red-600 px-4 py-2 font-medium text-white hover:bg-red-700"
            >
              Edit
            </button>
            <DeleteLocalPokemon pokemon={pokemon} onReload={onReload} />
          </div>
          <Link to={`/pokemon/${pokemon.name}`} className="text-sm font-medium text-red-700 hover:underline">
            View in catalog
          </Link>
        </>
      )}
    </section>
  )
}
