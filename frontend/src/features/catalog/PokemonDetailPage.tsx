import { useId } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../../api/http'
import { TypeBadge } from '../../components/ui/TypeBadge'
import { formatName, formatPokedexNumber } from '../../utils/format'
import { useCatalogReturnUrl } from './catalogNavigation'
import { EvolutionChain } from './EvolutionChain'
import { StatList } from './StatList'
import type { PokemonDetail } from './types'
import { usePokemonDetail } from './usePokemonDetail'

export function PokemonDetailPage() {
  const { idOrName = '' } = useParams()
  const navigate = useNavigate()
  const catalogUrl = useCatalogReturnUrl()
  const { data, isPending, isError, error, refetch } = usePokemonDetail(idOrName)

  if (isError && error instanceof ApiError && error.status === 404) {
    return (
      <section className="py-16 text-center">
        <h1 className="text-2xl font-semibold">Pokémon not found</h1>
        <p className="mt-2 text-slate-600">There is no Pokémon called “{idOrName}”.</p>
        <Link to="/" className="mt-4 inline-block text-red-700 underline">
          Back to the catalog
        </Link>
      </section>
    )
  }

  return (
    <div>
      <button
        type="button"
        onClick={() => void navigate(catalogUrl)}
        className="mb-4 text-sm font-medium text-red-700 hover:underline"
      >
        Back to the catalog
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

      {data && <PokemonDetailView pokemon={data} />}
    </div>
  )
}

function PokemonDetailView({ pokemon }: { pokemon: PokemonDetail }) {
  const statsHeadingId = useId()
  const evolutionHeadingId = useId()
  const displayName = formatName(pokemon.name)
  const image = pokemon.imageUrl ?? pokemon.spriteUrl

  return (
    <article className="grid gap-6 rounded-xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6 md:grid-cols-[minmax(0,1fr)_minmax(0,1.4fr)]">
      <div className="flex flex-col items-center text-center">
        {image ? (
          <img src={image} alt={displayName} width={280} height={280} className="h-auto w-full max-w-72" />
        ) : (
          <div className="flex h-60 w-60 items-center justify-center text-slate-400">No image</div>
        )}
        <span className="text-sm text-slate-500">{formatPokedexNumber(pokemon.id)}</span>
        <h1 className="text-3xl font-bold">{displayName}</h1>
        {pokemon.category && <p className="text-slate-600">{pokemon.category}</p>}
        <ul aria-label="Types" className="mt-2 flex flex-wrap justify-center gap-1">
          {pokemon.types.map((type) => (
            <li key={type}>
              <TypeBadge type={type} />
            </li>
          ))}
        </ul>
        <dl className="mt-4 grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-left text-sm">
          <dt className="font-medium text-slate-500">Height</dt>
          <dd>{pokemon.heightM} m</dd>
          <dt className="font-medium text-slate-500">Weight</dt>
          <dd>{pokemon.weightKg} kg</dd>
          <dt className="font-medium text-slate-500">Abilities</dt>
          <dd>{pokemon.abilities.map(formatName).join(', ')}</dd>
        </dl>
      </div>

      <div className="flex flex-col gap-6">
        {pokemon.description && <p className="text-lg leading-relaxed text-slate-700">{pokemon.description}</p>}

        <section aria-labelledby={statsHeadingId}>
          <h2 id={statsHeadingId} className="mb-3 text-lg font-semibold">
            Base stats
          </h2>
          <StatList stats={pokemon.stats} />
        </section>

        <section aria-labelledby={evolutionHeadingId}>
          <h2 id={evolutionHeadingId} className="mb-3 text-lg font-semibold">
            Evolution
          </h2>
          <EvolutionChain stages={pokemon.evolution} currentId={pokemon.id} />
        </section>
      </div>
    </article>
  )
}
