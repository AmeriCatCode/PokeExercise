import { useId, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../../api/http'
import { PokemonDetailView } from '../catalog/PokemonDetailView'
import { useLocalListReturnUrl } from './localNavigation'
import { TagList } from './TagList'
import { toPokemonDetail, type LocalPokemon } from './types'
import { useLocalPokemon } from './useLocalQueries'

/** Local ids are positive whole numbers; anything else can't exist, so it isn't even requested. */
function parseLocalId(value: string | undefined): number | null {
  return value && /^\d{1,15}$/.test(value) && Number(value) > 0 ? Number(value) : null
}

/** One local Pokémon: its proprietary fields and the PokeAPI data copied at import. */
export function LocalPokemonPage() {
  const id = parseLocalId(useParams().id)
  const navigate = useNavigate()
  const listUrl = useLocalListReturnUrl()
  const { data, isPending, isError, error, refetch } = useLocalPokemon(id ?? 0, { enabled: id !== null })

  if (id === null) {
    return (
      <Message title="Pokémon not found">
        <p className="mt-2 text-slate-600">This address doesn't match any local Pokémon.</p>
      </Message>
    )
  }

  if (isError && error instanceof ApiError && error.status === 404) {
    return (
      <Message title="This Pokémon was deleted by another user">
        <p className="mt-2 text-slate-600">It is no longer in the local database.</p>
      </Message>
    )
  }

  return (
    <div>
      <button
        type="button"
        onClick={() => void navigate(listUrl)}
        className="mb-4 text-sm font-medium text-red-700 hover:underline"
      >
        Back to my Pokémon
      </button>

      {isPending && <p className="py-12 text-center text-slate-600">Loading Pokémon…</p>}

      {isError && (
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-red-800">
          <p>{error.message}</p>
          <button
            type="button"
            onClick={() => void refetch()}
            className="mt-2 rounded-md bg-red-700 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-800"
          >
            Try again
          </button>
        </div>
      )}

      {data && <PokemonDetailView pokemon={toPokemonDetail(data)} panel={<LocalDataPanel pokemon={data} />} />}
    </div>
  )
}

function Message({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="py-16 text-center">
      <h1 className="text-2xl font-semibold">{title}</h1>
      {children}
      <Link to="/local" className="mt-4 inline-block text-red-700 underline">
        Back to my Pokémon
      </Link>
    </section>
  )
}

function LocalDataPanel({ pokemon }: { pokemon: LocalPokemon }) {
  const headingId = useId()
  const notSet = <span className="text-slate-400">Not set</span>

  return (
    <section
      aria-labelledby={headingId}
      className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 text-left"
    >
      <h2 id={headingId} className="text-lg font-semibold">
        Local data
      </h2>
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
      <Link to={`/pokemon/${pokemon.name}`} className="text-sm font-medium text-red-700 hover:underline">
        View in catalog
      </Link>
    </section>
  )
}
