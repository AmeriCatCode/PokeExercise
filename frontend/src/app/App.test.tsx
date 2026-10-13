import { render, screen } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { App } from './App'
import { routes } from './routes'

function renderAt(path: string) {
  render(<App router={createMemoryRouter(routes, { initialEntries: [path] })} />)
}

describe('App', () => {
  it('shows the app header with navigation to the catalog', () => {
    renderAt('/')

    expect(screen.getByRole('banner')).toHaveTextContent('Pokédex')
    expect(screen.getByRole('link', { name: 'Catalog' })).toHaveAttribute('href', '/')
  })

  it('shows a not-found page for unknown routes', () => {
    renderAt('/does-not-exist')

    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to the catalog' })).toHaveAttribute('href', '/')
  })
})
