import { render } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { createQueryClient } from '../api/queryClient'
import { App } from '../app/App'
import { routes } from '../app/routes'

/**
 * Renders the whole app at the given URL, with the production query settings except retries
 * (a failing request should fail the screen straight away in tests).
 */
export function renderApp(path = '/') {
  const queryClient = createQueryClient()
  queryClient.setDefaultOptions({
    queries: { ...queryClient.getDefaultOptions().queries, retry: false },
  })
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  return { ...render(<App router={router} queryClient={queryClient} />), router, queryClient }
}
