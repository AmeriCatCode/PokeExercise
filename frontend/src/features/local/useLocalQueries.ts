import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../api/http'
import { detailKeys, localKeys } from '../../api/queryKeys'
import { fetchLocalPage, fetchLocalPokemon, importPokemon, LOCAL_PAGE_SIZE } from './localApi'

/**
 * One page of the local list. Same out-of-order safety as the catalog: the page number is part of the
 * query key, so a late answer for an earlier page never replaces the page asked for last.
 *
 * @param page zero-based page number
 */
export function useLocalPage(page: number) {
  return useQuery({
    queryKey: localKeys.page(page, LOCAL_PAGE_SIZE),
    queryFn: ({ signal }) => fetchLocalPage(page, LOCAL_PAGE_SIZE, signal),
    placeholderData: keepPreviousData,
  })
}

export function useLocalPokemon(id: number, { enabled = true } = {}) {
  return useQuery({
    queryKey: localKeys.byId(id),
    queryFn: ({ signal }) => fetchLocalPokemon(id, signal),
    enabled,
  })
}

export function isConflict(error: unknown): boolean {
  return error instanceof ApiError && error.status === 409
}

/**
 * Imports a Pokémon. Afterwards (also when someone else imported it first: 409) the local list and the
 * "imported" flag of the catalog details are out of date, so they are reloaded. The mutation stays
 * pending until they are, so the button never flashes back to "Import".
 */
export function useImportPokemon() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: importPokemon,
    onSettled: async (_data, error) => {
      if (error && !isConflict(error)) return
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: localKeys.all }),
        queryClient.invalidateQueries({ queryKey: detailKeys.all }),
      ])
    },
  })
}
