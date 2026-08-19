import type { ButtonHTMLAttributes } from 'react'
import { buttonClasses, type ButtonVariant } from '../../lib/buttonClasses'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
}

export function Button({ variant = 'primary', className = '', ...props }: ButtonProps) {
  return <button className={buttonClasses(variant, className)} type="button" {...props} />
}
