/** "venusaur-mega" → "Venusaur Mega" */
export function formatName(name: string): string {
  return name
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

/** 25 → "#0025" */
export function formatPokedexNumber(id: number): string {
  return `#${String(id).padStart(4, '0')}`
}
