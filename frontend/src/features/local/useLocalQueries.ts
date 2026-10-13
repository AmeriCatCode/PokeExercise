import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../api/http'
import { detailKeys, localKeys } from '../../api/queryKeys'
import {
  deleteLocalPokemon,
  fetchLocalPage,
  fetchLocalPokemon,
  importPokemon,
  LOCAL_PAGE_SIZE,
  updateLocalPokemon,
  type LocalPokemonUpdate,
} from './localApi'

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

export function isNotFound(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404
}

/**
 * Saves the proprietary fields. Afterwards the saved Pokémon is shown at once, and the local list (which
 * shows the local name, region and tags) is reloaded.
 */
export function useUpdateLocalPokemon(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (update: LocalPokemonUpdate) => updateLocalPokemon(id, update),
    onSuccess: async (updated) => {
      queryClient.setQueryData(localKeys.byId(id), updated)
      await queryClient.invalidateQueries({ queryKey: localKeys.lists() })
    },
    onError: async (error) => {
      // Deleted by another user: the list is out of date too.
      if (isNotFound(error)) await queryClient.invalidateQueries({ queryKey: localKeys.lists() })
    },
  })
}

/**
 * Deletes the Pokémon. When it is gone (deleted now, or already by someone else: 404), `onGone` runs (the
 * screen leaves), and then the copies that contain it are dropped: the local list, the local Pokémon and
 * the "imported" flag of the catalog details, so the catalog offers Import again.
 */
export function useDeleteLocalPokemon(id: number, onGone: (alreadyDeleted: boolean) => void) {
  const queryClient = useQueryClient()
  const forget = async () => {
    queryClient.removeQueries({ queryKey: localKeys.byId(id) })
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: localKeys.lists() }),
      queryClient.invalidateQueries({ queryKey: detailKeys.all }),
    ])
  }
  return useMutation({
    mutationFn: (version: number) => deleteLocalPokemon(id, version),
    onSuccess: () => {
      onGone(false)
      return forget()
    },
    onError: (error) => {
      if (!isNotFound(error)) return
      onGone(true)
      return forget()
    },
  })
}
