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

export type Stat = {
  /** PokeAPI name: hp, attack, defense, special-attack, special-defense, speed */
  name: string
  baseValue: number
}

export type EvolutionStage = {
  /** 1 for the base species, 2 for its evolutions, and so on */
  stage: number
  speciesId: number
  name: string
  /** `null` for the base species */
  evolvesFrom: string | null
}

/** The detail of one Pokémon, as returned by `GET /api/pokemon/{idOrName}`. */
export type PokemonDetail = {
  id: number
  name: string
  /** Full-size official artwork, `null` when there is none. */
  imageUrl: string | null
  spriteUrl: string | null
  category: string
  types: string[]
  heightM: number
  weightKg: number
  abilities: string[]
  stats: Stat[]
  description: string
  evolution: EvolutionStage[]
  /** Logged-in users only: whether the Pokémon is in the local database. Absent for visitors. */
  imported?: boolean
  /** Logged-in users only, when imported: its id in the local database. */
  localId?: number
}
