export type ButtonVariant = 'primary' | 'secondary' | 'ghost'

const variantClasses: Record<ButtonVariant, string> = {
  primary: 'bg-primary text-white hover:opacity-90',
  secondary: 'bg-white text-primary border border-primary hover:bg-primary-soft',
  ghost: 'bg-white text-muted-2 hover:bg-primary-soft hover:text-ink',
}

/** react-router Link 등 <button>이 아닌 요소에 버튼 스타일을 입힐 때도 재사용. */
export function buttonClasses(variant: ButtonVariant = 'primary', className = '') {
  return `font-suit inline-flex items-center justify-center rounded-[6px] px-4 py-2 text-sm font-semibold tracking-[0.3px] transition disabled:cursor-not-allowed disabled:opacity-40 ${variantClasses[variant]} ${className}`
}
