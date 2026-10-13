import { Link } from 'react-router'
import { formatName } from '../../utils/format'
import type { PokemonDetail } from '../catalog/types'
import { isConflict, useImportPokemon } from './useLocalQueries'

/**
 * For logged-in users on the catalog detail: the Import button, or "Already imported" with a link to the
 * local version.
 */
export function ImportPanel({ pokemon }: { pokemon: PokemonDetail }) {
  const importMutation = useImportPokemon()
  const conflict = isConflict(importMutation.error)

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-slate-50 p-4 text-left">
      {pokemon.imported ? (
        <div className="flex flex-wrap items-center justify-between gap-2">
          <span className="rounded-full bg-green-100 px-3 py-1 text-sm font-medium text-green-800">
            Already imported
          </span>
          {pokemon.localId !== undefined && (
            <Link to={`/local/${pokemon.localId}`} className="text-sm font-medium text-red-700 hover:underline">
              View local version
            </Link>
          )}
        </div>
      ) : (
        <button
          type="button"
          disabled={importMutation.isPending}
          onClick={() => importMutation.mutate(pokemon.name)}
          className="rounded-md bg-red-600 px-4 py-2 font-medium text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {importMutation.isPending ? 'Importing…' : 'Import'}
        </button>
      )}

      {importMutation.isSuccess && (
        <p role="status" className="text-sm text-green-800">
          {formatName(pokemon.name)} was imported into the local database.
        </p>
      )}
      {conflict && (
        <p role="status" className="text-sm text-amber-900">
          Another user imported this Pokémon first.
        </p>
      )}
      {importMutation.isError && !conflict && (
        <p role="alert" className="text-sm text-red-800">
          {importMutation.error.message}
        </p>
      )}
    </div>
  )
}
