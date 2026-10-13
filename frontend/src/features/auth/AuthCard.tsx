import type { ReactNode } from 'react'

/** The centered card around the login and registration forms. */
export function AuthCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mx-auto mt-6 flex max-w-sm flex-col gap-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="text-2xl font-semibold">{title}</h1>
      {children}
    </section>
  )
}
