import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <section className="py-16 text-center">
      <h1 className="text-2xl font-semibold">Page not found</h1>
      <Link to="/" className="mt-4 inline-block text-red-700 underline">
        Back to the catalog
      </Link>
    </section>
  )
}
