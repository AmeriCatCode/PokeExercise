import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { TextField } from '../../components/ui/TextField'
import { localDataSchema, toFormValues, toUpdate, type LocalDataForm as FormValues } from './schemas'
import type { LocalPokemon } from './types'
import { isConflict, isNotFound, useUpdateLocalPokemon } from './useLocalQueries'

type LocalDataFormProps = {
  /** The Pokémon as it is when the form opens. */
  pokemon: LocalPokemon
  /** Loads the latest version of the Pokémon. */
  onReload: () => Promise<LocalPokemon | undefined>
  onCancel: () => void
  onSaved: () => void
}

/**
 * Edits the proprietary fields. The form is a snapshot: its values and version are taken once, when it
 * opens, and a background reload never changes them, so the user's typing is never lost and the save
 * carries the version the user actually saw. If someone else saved in between, the API answers 409 and
 * the user reloads explicitly.
 */
export function LocalDataForm({ pokemon, onReload, onCancel, onSaved }: LocalDataFormProps) {
  const [version, setVersion] = useState(pokemon.version)
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(localDataSchema), defaultValues: toFormValues(pokemon) })
  const update = useUpdateLocalPokemon(pokemon.id)
  const conflict = isConflict(update.error)
  const gone = isNotFound(update.error)

  const reload = async () => {
    const latest = await onReload()
    if (latest) {
      reset(toFormValues(latest))
      setVersion(latest.version)
    }
    update.reset()
  }

  return (
    <form
      noValidate
      onSubmit={handleSubmit((form) => update.mutate(toUpdate(form, version), { onSuccess: onSaved }))}
      className="flex flex-col gap-4"
    >
      <TextField label="Local name" error={errors.localName?.message} {...register('localName')} />
      <TextField label="Local region" error={errors.localRegion?.message} {...register('localRegion')} />
      <TextField
        label="Tags"
        hint="Separate tags with commas, e.g. starter, favorite"
        error={errors.tags?.message}
        {...register('tags')}
      />

      {conflict && (
        <div role="alert" className="rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
          <p>This Pokémon was changed by someone else. Reload it to see the changes, then apply yours again.</p>
          <button
            type="button"
            onClick={() => void reload()}
            className="mt-2 rounded-md bg-amber-600 px-3 py-1.5 font-medium text-white hover:bg-amber-700"
          >
            Reload
          </button>
        </div>
      )}
      {gone && (
        <div role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800">
          <p>This Pokémon no longer exists: another user deleted it.</p>
          <Link to="/local" className="mt-1 inline-block font-medium underline">
            Back to my Pokémon
          </Link>
        </div>
      )}
      {update.isError && !conflict && !gone && (
        <p role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800">
          {update.error.message}
        </p>
      )}

      <div className="flex flex-wrap gap-2">
        <button
          type="submit"
          disabled={update.isPending || gone}
          className="rounded-md bg-red-600 px-4 py-2 font-medium text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {update.isPending ? 'Saving…' : 'Save'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          disabled={update.isPending}
          className="rounded-md border border-slate-300 bg-white px-4 py-2 font-medium hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
        >
          Cancel
        </button>
      </div>
    </form>
  )
}
