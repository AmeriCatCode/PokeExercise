import { useQuery } from '@tanstack/react-query'
import { detailKeys } from '../../api/queryKeys'
import { fetchPokemonDetail } from './catalogApi'

export function usePokemonDetail(idOrName: string) {
  return useQuery({
    queryKey: detailKeys.byIdOrName(idOrName),
    queryFn: ({ signal }) => fetchPokemonDetail(idOrName, signal),
  })
}
