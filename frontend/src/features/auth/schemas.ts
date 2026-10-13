import { z } from 'zod'

/** Same rules as the backend, so most mistakes are caught before sending the form. */
export const loginSchema = z.object({
  username: z.string().trim().min(1, 'Enter your username'),
  password: z.string().min(1, 'Enter your password'),
})

export type LoginForm = z.infer<typeof loginSchema>

export const registerSchema = z
  .object({
    username: z
      .string()
      .trim()
      .regex(/^[A-Za-z0-9_.-]{3,30}$/, 'Use 3 to 30 characters: letters, digits, ".", "_" or "-"'),
    email: z.string().trim().pipe(z.email('Enter a valid email address')),
    password: z
      .string()
      .min(8, 'Use at least 8 characters')
      .max(72, 'Use at most 72 characters')
      .regex(/^(?=.*\p{L})(?=.*\d)/u, 'Use at least one letter and one digit'),
    confirmPassword: z.string(),
  })
  .refine((form) => form.password === form.confirmPassword, {
    message: 'The passwords do not match',
    path: ['confirmPassword'],
    // Also check when other fields are invalid, so every problem is shown at once.
    when: () => true,
  })

export type RegisterForm = z.infer<typeof registerSchema>
