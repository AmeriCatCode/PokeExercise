import { QueryClient } from '@tanstack/react-query'

/**
 * Single place for the server-state freshness rules:
 * - every screen loads fresh data when it opens (including going back to it or changing page);
 * - data is reloaded when the user returns to the browser tab.
 */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 0,
        refetchOnMount: true,
        refetchOnWindowFocus: true,
      },
    },
  })
}
