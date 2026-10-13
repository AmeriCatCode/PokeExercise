import { useId } from 'react'
import type { Stat } from './types'

const STAT_LABELS: Record<string, string> = {
  hp: 'HP',
  attack: 'Attack',
  defense: 'Defense',
  'special-attack': 'Sp. Atk',
  'special-defense': 'Sp. Def',
  speed: 'Speed',
}

/** Highest base stat a Pokémon can have; used as the scale of the bars. */
const MAX_BASE_STAT = 255

export function StatList({ stats }: { stats: Stat[] }) {
  return (
    <dl className="grid grid-cols-[auto_auto_minmax(4rem,1fr)] items-center gap-x-3 gap-y-2 text-sm">
      {stats.map((stat) => (
        <StatRow key={stat.name} stat={stat} />
      ))}
    </dl>
  )
}

function StatRow({ stat }: { stat: Stat }) {
  const labelId = useId()
  const percent = Math.min(100, (stat.baseValue / MAX_BASE_STAT) * 100)

  return (
    <>
      <dt id={labelId} className="font-medium text-slate-500">
        {STAT_LABELS[stat.name] ?? stat.name}
      </dt>
      <dd className="text-right font-semibold tabular-nums">{stat.baseValue}</dd>
      <dd>
        <div
          role="meter"
          aria-labelledby={labelId}
          aria-valuenow={stat.baseValue}
          aria-valuemin={0}
          aria-valuemax={MAX_BASE_STAT}
          className="h-2 overflow-hidden rounded-full bg-slate-200"
        >
          <div className="h-full rounded-full bg-red-500" style={{ width: `${percent}%` }} />
        </div>
      </dd>
    </>
  )
}
