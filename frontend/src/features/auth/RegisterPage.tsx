import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { ApiError } from '../../api/http'
import { TextField } from '../../components/ui/TextField'
import { login, register as registerAccount } from './authApi'
import { AuthCard } from './AuthCard'
import { registerSchema, type RegisterForm } from './schemas'
import { setSession } from './sessionStore'

const FORM_FIELDS = ['username', 'email', 'password'] as const

export function RegisterPage() {
  const navigate = useNavigate()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RegisterForm>({ resolver: zodResolver(registerSchema) })

  /** Create the account, then log in with the same credentials. */
  const registerMutation = useMutation({
    mutationFn: async ({ username, email, password }: RegisterForm) => {
      await registerAccount({ username, email, password })
      return login({ username, password })
    },
    onSuccess: (response) => {
      setSession({ token: response.token, expiresAt: response.expiresAt, user: response.user })
      void navigate('/', { replace: true })
    },
    onError: (error) => {
      // Show the server's field errors (e.g. "username already taken") on the fields themselves.
      if (error instanceof ApiError) {
        for (const fieldError of error.fieldErrors) {
          const field = FORM_FIELDS.find((name) => name === fieldError.field)
          if (field) setError(field, { message: fieldError.message })
        }
      }
    },
  })

  const hasFieldErrors = registerMutation.error instanceof ApiError && registerMutation.error.fieldErrors.length > 0

  return (
    <AuthCard title="Create an account">
      {registerMutation.isError && !hasFieldErrors && (
        <p role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800">
          {registerMutation.error.message}
        </p>
      )}
      <form
        noValidate
        onSubmit={handleSubmit((form) => registerMutation.mutate(form))}
        className="flex flex-col gap-4"
      >
        <TextField label="Username" autoComplete="username" error={errors.username?.message} {...register('username')} />
        <TextField label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          error={errors.password?.message}
          {...register('password')}
        />
        <TextField
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />
        <button
          type="submit"
          disabled={registerMutation.isPending}
          className="rounded-md bg-red-600 px-4 py-2 font-medium text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {registerMutation.isPending ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="text-center text-sm text-slate-600">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-red-700 underline">
          Log in
        </Link>
      </p>
    </AuthCard>
  )
}
