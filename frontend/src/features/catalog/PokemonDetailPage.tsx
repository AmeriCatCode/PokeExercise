import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../../api/http'
import { useSession } from '../auth/sessionStore'
import { ImportPanel } from '../local/ImportPanel'
import { useCatalogReturnUrl } from './catalogNavigation'
import { PokemonDetailView } from './PokemonDetailView'
import { usePokemonDetail } from './usePokemonDetail'

export function PokemonDetailPage() {
  const { idOrName = '' } = useParams()
  const navigate = useNavigate()
  const catalogUrl = useCatalogReturnUrl()
  const loggedIn = useSession() !== null
  const { data, isPending, isError, error, refetch } = usePokemonDetail(idOrName)

  if (isError && error instanceof ApiError && error.status === 404) {
    return (
      <section className="py-16 text-center">
        <h1 className="text-2xl font-semibold">Pokémon not found</h1>
        <p className="mt-2 text-slate-600">There is no Pokémon called “{idOrName}”.</p>
        <Link to="/" className="mt-4 inline-block text-red-700 underline">
          Back to the catalog
        </Link>
      </section>
    )
  }

  return (
    <div>
      <button
        type="button"
        onClick={() => void navigate(catalogUrl)}
        className="mb-4 text-sm font-medium text-red-700 hover:underline"
      >
        Back to the catalog
      </button>

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

      {data && (
        <PokemonDetailView
          pokemon={data}
          // Keyed by Pokémon, so moving to an evolution starts with a fresh panel.
          panel={loggedIn && <ImportPanel key={data.id} pokemon={data} />}
        />
      )}
    </div>
  )
}

