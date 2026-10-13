import { QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import { RouterProvider, type DataRouter } from 'react-router'
import { createQueryClient } from '../api/queryClient'

type AppProps = {
  router: DataRouter
}

export function App({ router }: AppProps) {
  const [queryClient] = useState(createQueryClient)

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
}
