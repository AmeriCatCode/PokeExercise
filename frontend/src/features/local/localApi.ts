import { deleteRequest, getJson, postJson, putJson } from '../../api/http'
import type { PageResponse } from '../../api/types'
import type { LocalPokemon, LocalPokemonSummary } from './types'

// Every local route is protected: each request sends the login token.

export const LOCAL_PAGE_SIZE = 20

/** @param idOrName the Pokémon's number or name in PokeAPI */
export function importPokemon(idOrName: string) {
  return postJson<LocalPokemon>('/api/local-pokemon', { idOrName }, { auth: true })
}

/** @param page zero-based page number */
export function fetchLocalPage(page: number, size: number, signal?: AbortSignal) {
  return getJson<PageResponse<LocalPokemonSummary>>('/api/local-pokemon', {
    params: { page, size },
    signal,
    auth: true,
  })
}

export function fetchLocalPokemon(id: number, signal?: AbortSignal) {
  return getJson<LocalPokemon>(`/api/local-pokemon/${id}`, { signal, auth: true })
}

/** The proprietary fields to save, with the version the user loaded. */
export type LocalPokemonUpdate = {
  localName: string | null
  localRegion: string | null
  tags: string[]
  version: number
}

export function updateLocalPokemon(id: number, update: LocalPokemonUpdate) {
  return putJson<LocalPokemon>(`/api/local-pokemon/${id}`, update, { auth: true })
}

/** @param version the version the user loaded: the delete is rejected (409) if someone changed it since */
export function deleteLocalPokemon(id: number, version: number) {
  return deleteRequest(`/api/local-pokemon/${id}`, { params: { version }, auth: true })
}
