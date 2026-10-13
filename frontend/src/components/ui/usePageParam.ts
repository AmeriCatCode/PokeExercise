import { useSearchParams } from 'react-router'

/**
 * The page of a paginated list, kept in the URL (1-based, e.g. `?page=3`), so reloading or going back
 * keeps it. Returns the zero-based page and a setter.
 */
export function usePageParam(): [number, (page: number) => void] {
  const [searchParams, setSearchParams] = useSearchParams()
  const fromUrl = Number(searchParams.get('page'))
  const page = Number.isInteger(fromUrl) && fromUrl >= 1 ? fromUrl - 1 : 0
  const setPage = (next: number) => setSearchParams(next === 0 ? {} : { page: String(next + 1) })
  return [page, setPage]
}
