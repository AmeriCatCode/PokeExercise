import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { RouterProvider, type DataRouter } from 'react-router'
import { createQueryClient } from '../api/queryClient'

type AppProps = {
  router: DataRouter
  /** Only tests pass their own; the app creates one with the production settings. */
  queryClient?: QueryClient
}

export function App({ router, queryClient: givenQueryClient }: AppProps) {
  const [queryClient] = useState(() => givenQueryClient ?? createQueryClient())

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
}
