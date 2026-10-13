/**
 * Every query key of the app, in one place, so invalidation after a change is consistent.
 */
export const catalogKeys = {
  all: ['catalog'] as const,
  page: (page: number, size: number) => [...catalogKeys.all, 'page', { page, size }] as const,
}

export const detailKeys = {
  all: ['pokemon-detail'] as const,
  /** Names are case-insensitive, so "Pikachu" and "pikachu" share one entry. */
  byIdOrName: (idOrName: string) => [...detailKeys.all, idOrName.toLowerCase()] as const,
}
