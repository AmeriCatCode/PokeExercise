/**
 * Every query key of the app, in one place, so invalidation after a change is consistent.
 */
export const catalogKeys = {
  all: ['catalog'] as const,
  page: (page: number, size: number) => [...catalogKeys.all, 'page', { page, size }] as const,
}

export const detailKeys = {
  all: ['pokemon-detail'] as const,
  /**
   * Names are case-insensitive, so "Pikachu" and "pikachu" share one entry. Logged-in users get the
   * import status too, so their answer is kept apart from a visitor's.
   */
  byIdOrName: (idOrName: string, loggedIn: boolean) =>
    [...detailKeys.all, idOrName.toLowerCase(), { loggedIn }] as const,
}

/** The local database: the list pages and each local Pokémon. */
export const localKeys = {
  all: ['local-pokemon'] as const,
  lists: () => [...localKeys.all, 'list'] as const,
  page: (page: number, size: number) => [...localKeys.lists(), { page, size }] as const,
  byId: (id: number) => [...localKeys.all, 'detail', id] as const,
}
