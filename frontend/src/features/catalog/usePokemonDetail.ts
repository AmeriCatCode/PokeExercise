import { useQuery } from '@tanstack/react-query'
import { detailKeys } from '../../api/queryKeys'
import { useSession } from '../auth/sessionStore'
import { fetchPokemonDetail } from './catalogApi'

/** The detail of a catalog Pokémon; for a logged-in user, with whether it is already imported. */
export function usePokemonDetail(idOrName: string) {
  const loggedIn = useSession() !== null
  return useQuery({
    queryKey: detailKeys.byIdOrName(idOrName, loggedIn),
    queryFn: ({ signal }) => fetchPokemonDetail(idOrName, { signal, auth: loggedIn }),
  })
}
