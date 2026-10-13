import { useId, type ReactNode } from 'react'
import { TypeBadge } from '../../components/ui/TypeBadge'
import { formatName, formatPokedexNumber } from '../../utils/format'
import { EvolutionChain } from './EvolutionChain'
import { StatList } from './StatList'
import type { PokemonDetail } from './types'

type PokemonDetailViewProps = {
  pokemon: PokemonDetail
  /** Shown under the Pokémon's name and measurements, e.g. the Import button or the local data. */
  panel?: ReactNode
}

/** The PokeAPI data of one Pokémon: used by the catalog detail and the local Pokémon screens. */
export function PokemonDetailView({ pokemon, panel }: PokemonDetailViewProps) {
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
        {panel && <div className="mt-6 w-full">{panel}</div>}
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
