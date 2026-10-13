import { useSearchParams } from 'react-router'
import { Pagination } from '../../components/ui/Pagination'
import { PokemonCard } from './PokemonCard'
import { useCatalogPage } from './useCatalogPage'

/** The URL holds the page (1-based, e.g. `?page=3`), so reloading or going back keeps it. */
function usePageParam(): [number, (page: number) => void] {
  const [searchParams, setSearchParams] = useSearchParams()
  const fromUrl = Number(searchParams.get('page'))
  const page = Number.isInteger(fromUrl) && fromUrl >= 1 ? fromUrl - 1 : 0
  const setPage = (next: number) => setSearchParams(next === 0 ? {} : { page: String(next + 1) })
  return [page, setPage]
}

export function CatalogPage() {
  const [page, setPage] = usePageParam()
  const { data, isPending, isError, error, refetch, isFetching, isPlaceholderData } = useCatalogPage(page)

  return (
    <section aria-busy={isFetching}>
      <h1 className="mb-4 text-2xl font-semibold">Catalog</h1>

      {isPending && <p className="py-12 text-center text-slate-600">Loading Pokémon…</p>}

      {isError && (
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-red-800">
          <p>{error.message}</p>
          <button
            type="button"
            onClick={() => void refetch()}
            className="mt-2 rounded-md bg-red-700 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-800"
          >
            Try again
          </button>
        </div>
      )}

      {data && !isError && (
        <>
          <ul
            aria-label="Pokémon"
            className={`grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 ${isPlaceholderData ? 'opacity-60' : ''}`}
          >
            {data.items.map((pokemon) => (
              <li key={pokemon.id}>
                <PokemonCard pokemon={pokemon} />
              </li>
            ))}
          </ul>
          <div className="mt-6">
            <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
          </div>
        </>
      )}
    </section>
  )
}
