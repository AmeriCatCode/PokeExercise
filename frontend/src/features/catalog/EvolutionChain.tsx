import { Link } from 'react-router'
import { formatName } from '../../utils/format'
import { useKeepCatalogState } from './catalogNavigation'
import type { EvolutionStage } from './types'

type EvolutionChainProps = {
  stages: EvolutionStage[]
  /** The Pokémon being viewed, marked as the current page. */
  currentId: number
}

/**
 * The evolution lineage, stage by stage. When a stage has species coming from different branches
 * (e.g. Wurmple → Silcoon → Beautifly / Wurmple → Cascoon → Dustox), each one says what it evolves from.
 */
export function EvolutionChain({ stages, currentId }: EvolutionChainProps) {
  const state = useKeepCatalogState()
  const byStage = groupByStage(stages)

  return (
    <ol className="flex flex-wrap items-start gap-3">
      {byStage.map(([stage, species], index) => {
        const showOrigin = new Set(species.map((s) => s.evolvesFrom)).size > 1
        return (
          <li key={stage} className="flex items-start gap-3">
            {index > 0 && (
              <span aria-hidden="true" className="pt-2 text-slate-400">
                →
              </span>
            )}
            <ul aria-label={`Stage ${stage}`} className="flex flex-col gap-2">
              {species.map((s) => {
                const isCurrent = s.speciesId === currentId
                return (
                  <li key={s.speciesId} className="flex flex-col">
                    <Link
                      to={`/pokemon/${s.speciesId}`}
                      state={state}
                      aria-current={isCurrent ? 'page' : undefined}
                      className={`rounded-md border px-3 py-1.5 text-sm font-medium ${
                        isCurrent
                          ? 'border-red-600 bg-red-600 text-white'
                          : 'border-slate-300 bg-white hover:bg-slate-100'
                      }`}
                    >
                      {formatName(s.name)}
                    </Link>
                    {showOrigin && s.evolvesFrom && (
                      <span className="mt-0.5 text-xs text-slate-500">from {formatName(s.evolvesFrom)}</span>
                    )}
                  </li>
                )
              })}
            </ul>
          </li>
        )
      })}
    </ol>
  )
}

function groupByStage(stages: EvolutionStage[]): [number, EvolutionStage[]][] {
  const groups = new Map<number, EvolutionStage[]>()
  for (const stage of stages) {
    groups.set(stage.stage, [...(groups.get(stage.stage) ?? []), stage])
  }
  return [...groups.entries()].sort(([a], [b]) => a - b)
}
