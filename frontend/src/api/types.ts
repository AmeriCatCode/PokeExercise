/** The common shape of every paginated response. `page` is zero-based. */
export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}
