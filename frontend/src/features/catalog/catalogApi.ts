import { getJson } from '../../api/http'
import type { PageResponse } from '../../api/types'
import type { PokemonDetail, PokemonSummary } from './types'

export const CATALOG_PAGE_SIZE = 20

/** @param page zero-based page number */
export function fetchCatalogPage(page: number, size: number, signal?: AbortSignal) {
  return getJson<PageResponse<PokemonSummary>>('/api/pokemon', { params: { page, size }, signal })
}

/**
 * @param options.auth send the login token: the answer then also says whether the Pokémon is imported.
 *   If the token is rejected, the session has expired (see `http.ts`).
 */
export function fetchPokemonDetail(idOrName: string, options: { signal?: AbortSignal; auth?: boolean } = {}) {
  return getJson<PokemonDetail>(`/api/pokemon/${encodeURIComponent(idOrName)}`, options)
}
