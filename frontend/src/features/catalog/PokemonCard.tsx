import { useId } from 'react'
import { Link, useLocation } from 'react-router'
import { TypeBadge } from '../../components/ui/TypeBadge'
import { formatName, formatPokedexNumber } from '../../utils/format'
import type { CatalogNavigationState } from './catalogNavigation'
import type { PokemonSummary } from './types'

export function PokemonCard({ pokemon }: { pokemon: PokemonSummary }) {
  const titleId = useId()
  const { search } = useLocation()
  const displayName = formatName(pokemon.name)
  const state: CatalogNavigationState = { catalogSearch: search }

  return (
    <Link
      to={`/pokemon/${pokemon.name}`}
      state={state}
      className="block h-full rounded-xl focus:outline-none focus-visible:ring-2 focus-visible:ring-red-600"
    >
      <article
        aria-labelledby={titleId}
        className="flex h-full flex-col items-center rounded-xl border border-slate-200 bg-white p-4 text-center shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
      >
        {pokemon.spriteUrl ? (
          <img src={pokemon.spriteUrl} alt={displayName} width={96} height={96} loading="lazy" className="h-24 w-24" />
        ) : (
          <div className="flex h-24 w-24 items-center justify-center text-xs text-slate-400">No image</div>
        )}
        <span className="text-xs text-slate-500">{formatPokedexNumber(pokemon.id)}</span>
        <h2 id={titleId} className="text-lg font-semibold">
          {displayName}
        </h2>
        {pokemon.category && <p className="text-sm text-slate-600">{pokemon.category}</p>}
        <ul aria-label="Types" className="mt-2 flex flex-wrap justify-center gap-1">
          {pokemon.types.map((type) => (
            <li key={type}>
              <TypeBadge type={type} />
            </li>
          ))}
        </ul>
        <dl className="mt-3 grid w-full grid-cols-[auto_1fr] gap-x-2 gap-y-1 text-left text-sm">
          <dt className="font-medium text-slate-500">Weight</dt>
          <dd>{pokemon.weightKg} kg</dd>
          <dt className="font-medium text-slate-500">Abilities</dt>
          <dd>{pokemon.abilities.map(formatName).join(', ')}</dd>
        </dl>
      </article>
    </Link>
  )
}
