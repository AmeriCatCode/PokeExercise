import { describe, expect, it } from 'vitest'
import { localDataSchema, parseTags, toUpdate } from './schemas'

const valid = { localName: 'Évoli', localRegion: 'Kanto', tags: 'starter' }

function messages(form: typeof valid) {
  const result = localDataSchema.safeParse(form)
  return result.success ? [] : result.error.issues.map((issue) => issue.message)
}

describe('local data form rules', () => {
  it('splits tags on commas, trimmed, lower case, ignoring empty ones', () => {
    expect(parseTags(' Starter, ,fan-favorite ,')).toEqual(['starter', 'fan-favorite'])
    expect(parseTags('   ')).toEqual([])
  })

  it('accepts valid values and empty ones', () => {
    expect(messages(valid)).toEqual([])
    expect(messages({ localName: '', localRegion: '', tags: '' })).toEqual([])
  })

  it('limits the local name and region to 100 characters', () => {
    expect(messages({ ...valid, localName: 'x'.repeat(100) })).toEqual([])
    expect(messages({ ...valid, localName: 'x'.repeat(101) })).toEqual(['Use at most 100 characters'])
    expect(messages({ ...valid, localRegion: 'x'.repeat(101) })).toEqual(['Use at most 100 characters'])
  })

  it('rejects duplicated tags whatever the case, long tags and too many tags', () => {
    expect(messages({ ...valid, tags: 'starter, STARTER' })).toEqual(['Each tag only once ("starter")'])
    expect(messages({ ...valid, tags: 'x'.repeat(31) })).toEqual(['Each tag can have at most 30 characters'])
    const tags = Array.from({ length: 21 }, (_, i) => `tag${i}`).join(',')
    expect(messages({ ...valid, tags })).toEqual(['Use at most 20 tags'])
  })

  it('sends empty texts as null, with the version the form was opened with', () => {
    expect(toUpdate({ localName: '  ', localRegion: ' Kanto ', tags: 'a, b' }, 4)).toEqual({
      localName: null,
      localRegion: 'Kanto',
      tags: ['a', 'b'],
      version: 4,
    })
  })
})
