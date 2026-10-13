/**
 * Every query key of the app, in one place, so invalidation after a change is consistent.
 */
export const catalogKeys = {
  all: ['catalog'] as const,
  page: (page: number, size: number) => [...catalogKeys.all, 'page', { page, size }] as const,
}
