import { useState } from 'react'
import { useNavigate } from 'react-router'
import { formatName } from '../../utils/format'
import type { LocalListLocationState } from './LocalListPage'
import type { LocalPokemon } from './types'
import { isConflict, useDeleteLocalPokemon } from './useLocalQueries'

type DeleteLocalPokemonProps = {
  pokemon: LocalPokemon
  /** Loads the latest version of the Pokémon. */
  onReload: () => Promise<unknown>
}

/**
 * The Delete button, with a confirmation step. It sends the version the user saw when clicking Delete,
 * even if a background reload shows a newer one while the confirmation is open: a change made by someone
 * else since is never deleted unseen (409). If someone already deleted it (404), the job is done.
 */
export function DeleteLocalPokemon({ pokemon, onReload }: DeleteLocalPokemonProps) {
  const navigate = useNavigate()
  // The version the user decided to delete; null while no confirmation is open.
  const [versionToDelete, setVersionToDelete] = useState<number | null>(null)
  const displayName = formatName(pokemon.name)
  const deletion = useDeleteLocalPokemon(pokemon.id, (alreadyDeleted) => {
    const state: LocalListLocationState = {
      notice: alreadyDeleted
        ? `${displayName} had already been deleted by another user.`
        : `${displayName} was deleted.`,
    }
    // Replace: going back must not reopen a Pokémon that no longer exists.
    void navigate('/local', { state, replace: true })
  })
  const conflict = isConflict(deletion.error)

  const close = () => {
    deletion.reset()
    setVersionToDelete(null)
  }

  if (versionToDelete === null) {
    return (
      <button
        type="button"
        onClick={() => setVersionToDelete(pokemon.version)}
        className="rounded-md border border-red-300 bg-white px-4 py-2 font-medium text-red-700 hover:bg-red-50"
      >
        Delete
      </button>
    )
  }

  return (
    <div className="flex w-full flex-col gap-3 rounded-md border border-red-200 bg-red-50 p-3 text-sm">
      <p className="font-medium text-red-900">Delete {displayName} from the local database?</p>
      {conflict && (
        <div role="alert" className="rounded-md border border-amber-200 bg-amber-50 p-3 text-amber-900">
          <p>This Pokémon was changed by someone else. Reload it to see the changes before deleting it.</p>
          <button
            type="button"
            onClick={() => void onReload().then(close)}
            className="mt-2 rounded-md bg-amber-600 px-3 py-1.5 font-medium text-white hover:bg-amber-700"
          >
            Reload
          </button>
        </div>
      )}
      {deletion.isError && !conflict && (
        <p role="alert" className="text-red-800">
          {deletion.error.message}
        </p>
      )}
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          disabled={deletion.isPending || conflict}
          onClick={() => deletion.mutate(versionToDelete)}
          className="rounded-md bg-red-600 px-4 py-2 font-medium text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {deletion.isPending ? 'Deleting…' : 'Yes, delete'}
        </button>
        <button
          type="button"
          disabled={deletion.isPending}
          onClick={close}
          className="rounded-md border border-slate-300 bg-white px-4 py-2 font-medium hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
        >
          Cancel
        </button>
      </div>
    </div>
  )
}
