import { QueryClient } from '@tanstack/react-query'
import { ApiError } from './http'

const MAX_RETRIES = 1

/**
 * Single place for the server-state rules:
 * - every screen loads fresh data when it opens (including going back to it or changing page);
 * - data is reloaded when the user returns to the browser tab;
 * - a client error (4xx) is never retried; a server or network error is retried once.
 */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 0,
        refetchOnMount: true,
        refetchOnWindowFocus: true,
        retry: (failureCount, error) => !isClientError(error) && failureCount < MAX_RETRIES,
      },
    },
  })
}

function isClientError(error: unknown): boolean {
  return error instanceof ApiError && error.status >= 400 && error.status < 500
}
