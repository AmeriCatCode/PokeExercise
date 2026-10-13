import { Link, useLocation } from 'react-router'
import { Pagination } from '../../components/ui/Pagination'
import { usePageParam } from '../../components/ui/usePageParam'
import { LocalPokemonCard } from './LocalPokemonCard'
import { useLocalPage } from './useLocalQueries'

/** Location state of the list: a message about what just happened, e.g. after a delete. */
export type LocalListLocationState = { notice?: string } | null

/** "My local Pokémon": the Pokémon imported into the local database. */
export function LocalListPage() {
  const notice = (useLocation().state as LocalListLocationState)?.notice
  const [page, setPage] = usePageParam()
  const { data, isPending, isError, error, refetch, isFetching, isPlaceholderData } = useLocalPage(page)

  return (
    <section aria-busy={isFetching}>
      <h1 className="mb-4 text-2xl font-semibold">My local Pokémon</h1>

      {notice && (
        <p role="status" className="mb-4 rounded-md border border-green-200 bg-green-50 p-3 text-sm text-green-800">
          {notice}
        </p>
      )}

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

      {data && !isError && data.totalItems === 0 && (
        <div className="py-12 text-center text-slate-600">
          <p>No Pokémon imported yet.</p>
          <Link to="/" className="mt-2 inline-block font-medium text-red-700 underline">
            Browse the catalog
          </Link>
        </div>
      )}

      {data && !isError && data.totalItems > 0 && (
        <>
          <ul
            aria-label="Local Pokémon"
            className={`grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 ${isPlaceholderData ? 'opacity-60' : ''}`}
          >
            {data.items.map((pokemon) => (
              <li key={pokemon.id}>
                <LocalPokemonCard pokemon={pokemon} />
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
