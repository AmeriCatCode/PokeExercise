import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import { TextField } from '../../components/ui/TextField'
import { login } from './authApi'
import { AuthCard } from './AuthCard'
import { loginSchema, type LoginForm } from './schemas'
import { setSession } from './sessionStore'

/** Where the login screen was opened from, and why. */
export type LoginLocationState = { from?: string; reason?: 'expired' } | null

export function LoginPage() {
  const navigate = useNavigate()
  const state = useLocation().state as LoginLocationState
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) })

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (response) => {
      setSession({ token: response.token, expiresAt: response.expiresAt, user: response.user })
      void navigate(state?.from ?? '/', { replace: true })
    },
  })

  return (
    <AuthCard title="Log in">
      {state?.reason === 'expired' && (
        <p role="status" className="rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
          Your session expired, please log in again
        </p>
      )}
      {loginMutation.isError && (
        <p role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800">
          {loginMutation.error.message}
        </p>
      )}
      <form noValidate onSubmit={handleSubmit((form) => loginMutation.mutate(form))} className="flex flex-col gap-4">
        <TextField label="Username" autoComplete="username" error={errors.username?.message} {...register('username')} />
        <TextField
          label="Password"
          type="password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />
        <button
          type="submit"
          disabled={loginMutation.isPending}
          className="rounded-md bg-red-600 px-4 py-2 font-medium text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loginMutation.isPending ? 'Logging in…' : 'Log in'}
        </button>
      </form>
      <p className="text-center text-sm text-slate-600">
        No account yet?{' '}
        <Link to="/register" className="font-medium text-red-700 underline">
          Create an account
        </Link>
      </p>
    </AuthCard>
  )
}
