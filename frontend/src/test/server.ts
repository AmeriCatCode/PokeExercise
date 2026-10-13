import { setupServer } from 'msw/node'

/**
 * Fake API used by the tests. Each test registers the handlers it needs with `server.use(...)`.
 */
export const server = setupServer()
