import { useLocation } from 'react-router'

/**
 * Navigation state carried from the local list to a local Pokémon, so "Back to my Pokémon" returns to
 * the list page the user came from.
 */
export type LocalNavigationState = { listSearch?: string }

/** The local list URL to go back to (e.g. `/local?page=2`), or its first page. */
export function useLocalListReturnUrl(): string {
  const state = useLocation().state as LocalNavigationState | null
  return `/local${state?.listSearch ?? ''}`
}
