/** A catalog entry, as returned by `GET /api/pokemon`. */
export type PokemonSummary = {
  id: number
  name: string
  /** `null` when the Pokémon has no sprite. */
  spriteUrl: string | null
  /** Official Pokédex category, e.g. "Seed Pokémon". Empty when PokeAPI has none in English. */
  category: string
  types: string[]
  weightKg: number
  abilities: string[]
}
