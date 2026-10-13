import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { catalogKeys } from '../../api/queryKeys'
import { CATALOG_PAGE_SIZE, fetchCatalogPage } from './catalogApi'

/**
 * One page of the catalog. The page number is part of the query key, so the screen always shows the
 * page the user asked for last: a late answer for an earlier page only fills that page's cache entry.
 * While the new page loads, the previous one stays on screen instead of a blank list.
 *
 * @param page zero-based page number
 */
export function useCatalogPage(page: number) {
  return useQuery({
    queryKey: catalogKeys.page(page, CATALOG_PAGE_SIZE),
    queryFn: ({ signal }) => fetchCatalogPage(page, CATALOG_PAGE_SIZE, signal),
    placeholderData: keepPreviousData,
  })
}
