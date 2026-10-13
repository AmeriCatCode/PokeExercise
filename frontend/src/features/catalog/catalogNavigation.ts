import { useLocation } from 'react-router'

/**
 * Navigation state carried from the catalog to the detail screens, so "Back to the catalog" returns to
 * the catalog page the user came from, even after moving between evolutions.
 */
export type CatalogNavigationState = { catalogSearch?: string }

/** The catalog URL to go back to (e.g. `/?page=3`), or the first page. */
export function useCatalogReturnUrl(): string {
  const state = useLocation().state as CatalogNavigationState | null
  return `/${state?.catalogSearch ?? ''}`
}

/** State to pass when leaving a detail for another detail, so the way back to the catalog is kept. */
export function useKeepCatalogState(): CatalogNavigationState | null {
  return useLocation().state as CatalogNavigationState | null
}
