import { useId, type InputHTMLAttributes } from 'react'

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string
  /** Validation message shown under the field. */
  error?: string
}

/** A labelled input with its validation message, linked for screen readers. */
export function TextField({ label, error, ...inputProps }: TextFieldProps) {
  const id = useId()
  const errorId = `${id}-error`

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-slate-700">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={`rounded-md border px-3 py-2 focus:outline-none focus-visible:ring-2 focus-visible:ring-red-600 ${
          error ? 'border-red-500' : 'border-slate-300'
        }`}
        {...inputProps}
      />
      {error && (
        <p id={errorId} role="alert" className="text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  )
}
