import { formatName } from '../../utils/format'

// Full class names (not built dynamically) so Tailwind can find them.
const TYPE_COLORS: Record<string, string> = {
  normal: 'bg-stone-400 text-white',
  fire: 'bg-orange-500 text-white',
  water: 'bg-blue-500 text-white',
  electric: 'bg-yellow-400 text-slate-900',
  grass: 'bg-green-500 text-white',
  ice: 'bg-cyan-300 text-slate-900',
  fighting: 'bg-red-700 text-white',
  poison: 'bg-purple-500 text-white',
  ground: 'bg-amber-600 text-white',
  flying: 'bg-indigo-300 text-slate-900',
  psychic: 'bg-pink-500 text-white',
  bug: 'bg-lime-500 text-slate-900',
  rock: 'bg-yellow-700 text-white',
  ghost: 'bg-violet-700 text-white',
  dragon: 'bg-indigo-600 text-white',
  dark: 'bg-stone-700 text-white',
  steel: 'bg-slate-400 text-slate-900',
  fairy: 'bg-pink-300 text-slate-900',
}

export function TypeBadge({ type }: { type: string }) {
  const colors = TYPE_COLORS[type] ?? 'bg-slate-300 text-slate-900'
  return <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${colors}`}>{formatName(type)}</span>
}
