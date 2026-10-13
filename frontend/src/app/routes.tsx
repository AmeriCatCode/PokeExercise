import type { RouteObject } from 'react-router'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { RequireSession } from '../features/auth/RequireSession'
import { CatalogPage } from '../features/catalog/CatalogPage'
import { PokemonDetailPage } from '../features/catalog/PokemonDetailPage'
import { LocalListPage } from '../features/local/LocalListPage'
import { LocalPokemonPage } from '../features/local/LocalPokemonPage'
import { Layout } from './Layout'
import { NotFoundPage } from './NotFoundPage'

export const routes: RouteObject[] = [
  {
    element: <Layout />,
    children: [
      { path: '/', element: <CatalogPage /> },
      { path: '/pokemon/:idOrName', element: <PokemonDetailPage /> },
      {
        path: '/local',
        element: (
          <RequireSession>
            <LocalListPage />
          </RequireSession>
        ),
      },
      {
        path: '/local/:id',
        element: (
          <RequireSession>
            <LocalPokemonPage />
          </RequireSession>
        ),
      },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
