type PaginationProps = {
  /** zero-based */
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

const buttonClass =
  'rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50'

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  return (
    <nav aria-label="Pagination" className="flex items-center justify-center gap-4">
      <button type="button" className={buttonClass} disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
        Previous
      </button>
      <span className="text-sm text-slate-600">
        Page {page + 1} of {Math.max(totalPages, 1)}
      </span>
      <button
        type="button"
        className={buttonClass}
        disabled={page + 1 >= totalPages}
        onClick={() => onPageChange(page + 1)}
      >
        Next
      </button>
    </nav>
  )
}
