import type { RouteObject } from 'react-router'
import { CatalogPage } from '../features/catalog/CatalogPage'
import { Layout } from './Layout'
import { NotFoundPage } from './NotFoundPage'

export const routes: RouteObject[] = [
  {
    element: <Layout />,
    children: [
      { path: '/', element: <CatalogPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
