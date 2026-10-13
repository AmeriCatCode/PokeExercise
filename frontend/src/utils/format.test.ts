import { describe, expect, it } from 'vitest'
import { formatName, formatPokedexNumber } from './format'

describe('formatName', () => {
  it('capitalizes each word of a PokeAPI name', () => {
    expect(formatName('bulbasaur')).toBe('Bulbasaur')
    expect(formatName('venusaur-mega')).toBe('Venusaur Mega')
    expect(formatName('solar-power')).toBe('Solar Power')
  })
})

describe('formatPokedexNumber', () => {
  it('pads the number to at least 4 digits', () => {
    expect(formatPokedexNumber(1)).toBe('#0001')
    expect(formatPokedexNumber(1025)).toBe('#1025')
    expect(formatPokedexNumber(10033)).toBe('#10033')
  })
})
