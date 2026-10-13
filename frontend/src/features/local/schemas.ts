import { z } from 'zod'
import type { LocalPokemonUpdate } from './localApi'
import type { LocalPokemon } from './types'

// Same rules as the backend, so most mistakes are caught before sending the form.
const MAX_TEXT_LENGTH = 100
const MAX_TAG_LENGTH = 30
const MAX_TAGS = 20

/** "Starter,  fan-favorite , " → ["starter", "fan-favorite"]: tags are typed separated by commas. */
export function parseTags(text: string): string[] {
  return text
    .split(',')
    .map((tag) => tag.trim().toLowerCase())
    .filter((tag) => tag.length > 0)
}

const optionalText = z.string().trim().max(MAX_TEXT_LENGTH, `Use at most ${MAX_TEXT_LENGTH} characters`)

export const localDataSchema = z.object({
  localName: optionalText,
  localRegion: optionalText,
  tags: z.string().superRefine((text, ctx) => {
    const tags = parseTags(text)
    if (tags.length > MAX_TAGS) {
      ctx.addIssue({ code: 'custom', message: `Use at most ${MAX_TAGS} tags` })
    }
    const tooLong = tags.find((tag) => tag.length > MAX_TAG_LENGTH)
    if (tooLong) {
      ctx.addIssue({ code: 'custom', message: `Each tag can have at most ${MAX_TAG_LENGTH} characters` })
    }
    const duplicate = tags.find((tag, index) => tags.indexOf(tag) !== index)
    if (duplicate) {
      ctx.addIssue({ code: 'custom', message: `Each tag only once ("${duplicate}")` })
    }
  }),
})

export type LocalDataForm = z.infer<typeof localDataSchema>

export function toFormValues(pokemon: LocalPokemon): LocalDataForm {
  return {
    localName: pokemon.localName ?? '',
    localRegion: pokemon.localRegion ?? '',
    tags: pokemon.tags.join(', '),
  }
}

/** An empty local name or region clears it. */
export function toUpdate(form: LocalDataForm, version: number): LocalPokemonUpdate {
  return {
    localName: form.localName.trim() || null,
    localRegion: form.localRegion.trim() || null,
    tags: parseTags(form.tags),
    version,
  }
}
