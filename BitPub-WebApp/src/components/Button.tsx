/**
 * Autore: Luca Franzon 20054744
 */
import React from 'react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * Unisce le classi CSS in modo condizionale, risolvendo i conflitti tramite tailwind-merge.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
}

/**
 * Componente bottone personalizzato che supporta diverse varianti e dimensioni.
 */
const Button: React.FC<ButtonProps> = ({ 
  className, 
  variant = 'primary', 
  size = 'md', 
  children, 
  ...props 
}) => {
  const baseStyles = 'inline-flex items-center justify-center font-medium rounded-xl transition-all focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-brand-dark disabled:opacity-50 disabled:pointer-events-none active:scale-95';
  
  const variants = {
    primary: 'bg-brand hover:bg-brand-light text-white shadow-lg hover:shadow-brand/50 focus:ring-brand',
    secondary: 'bg-slate-800 hover:bg-slate-700 text-white border border-slate-700 hover:border-slate-600 focus:ring-slate-500',
    danger: 'bg-red-500 hover:bg-red-400 text-white shadow-lg hover:shadow-red-500/50 focus:ring-red-500',
    ghost: 'hover:bg-slate-800 text-slate-300 hover:text-white',
  };

  const sizes = {
    sm: 'h-9 px-3 text-sm',
    md: 'h-11 px-6 text-base',
    lg: 'h-14 px-8 text-lg',
  };

  return (
    <button 
      className={cn(baseStyles, variants[variant], sizes[size], className)}
      {...props}
    >
      {children}
    </button>
  );
};

export default Button;
