import { useId } from 'react'
import { Link, useLocation } from 'react-router'
import { TypeBadge } from '../../components/ui/TypeBadge'
import { formatName, formatPokedexNumber } from '../../utils/format'
import type { LocalNavigationState } from './localNavigation'
import { TagList } from './TagList'
import type { LocalPokemonSummary } from './types'

export function LocalPokemonCard({ pokemon }: { pokemon: LocalPokemonSummary }) {
  const titleId = useId()
  const { search } = useLocation()
  const displayName = formatName(pokemon.name)
  const localInfo = [pokemon.localName, pokemon.localRegion].filter(Boolean).join(' · ')
  const state: LocalNavigationState = { listSearch: search }

  return (
    <Link
      to={`/local/${pokemon.id}`}
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
        <span className="text-xs text-slate-500">{formatPokedexNumber(pokemon.pokeapiId)}</span>
        <h2 id={titleId} className="text-lg font-semibold">
          {displayName}
        </h2>
        {localInfo && <p className="text-sm font-medium text-red-800">{localInfo}</p>}
        {pokemon.category && <p className="text-sm text-slate-600">{pokemon.category}</p>}
        <ul aria-label="Types" className="mt-2 flex flex-wrap justify-center gap-1">
          {pokemon.types.map((type) => (
            <li key={type}>
              <TypeBadge type={type} />
            </li>
          ))}
        </ul>
        {pokemon.tags.length > 0 && (
          <div className="mt-3">
            <TagList tags={pokemon.tags} />
          </div>
        )}
      </article>
    </Link>
  )
}
