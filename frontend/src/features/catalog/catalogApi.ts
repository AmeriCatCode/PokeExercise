import { getJson } from '../../api/http'
import type { PageResponse } from '../../api/types'
import type { PokemonDetail, PokemonSummary } from './types'

export const CATALOG_PAGE_SIZE = 20

/** @param page zero-based page number */
export function fetchCatalogPage(page: number, size: number, signal?: AbortSignal) {
  return getJson<PageResponse<PokemonSummary>>('/api/pokemon', { params: { page, size }, signal })
}

export function fetchPokemonDetail(idOrName: string, signal?: AbortSignal) {
  return getJson<PokemonDetail>(`/api/pokemon/${encodeURIComponent(idOrName)}`, { signal })
}
