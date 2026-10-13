import type { EvolutionStage, PokemonDetail, Stat } from '../catalog/types'

/** The proprietary fields: the only ones users can change. */
type ProprietaryFields = {
  /** Localized nomenclature; `null` when not set. */
  localName: string | null
  /** Geographical metadata; `null` when not set. */
  localRegion: string | null
  /** Internal classification tags. */
  tags: string[]
}

/** An entry of `GET /api/local-pokemon`. */
export type LocalPokemonSummary = ProprietaryFields & {
  /** The local id. */
  id: number
  /** The Pokémon's number in PokeAPI. */
  pokeapiId: number
  name: string
  spriteUrl: string | null
  category: string
  types: string[]
}

/** A local Pokémon, as returned by `GET /api/local-pokemon/{id}` and by the import. */
export type LocalPokemon = ProprietaryFields & {
  /** The local id. */
  id: number
  /** The Pokémon's number in PokeAPI. */
  pokeapiId: number
  // Copied from PokeAPI at import (read-only)
  name: string
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
  /** Increases on every change; sent back when changing the Pokémon, to detect concurrent edits. */
  version: number
  /** ISO dates */
  createdAt: string
  updatedAt: string
}

/** The PokeAPI part of a local Pokémon, in the shape the catalog detail view shows. */
export function toPokemonDetail(local: LocalPokemon): PokemonDetail {
  return { ...local, id: local.pokeapiId }
}
