/**
 * Autore: Luca Franzon 20054744
 */
import React from 'react';
import { cn } from './Button';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

/**
 * Componente di input generico che include un'etichetta opzionale e messaggi di errore.
 */
const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, ...props }, ref) => {
    return (
      <div className="w-full flex flex-col gap-1.5">
        {label && (
          <label className="text-sm font-medium text-slate-300">
            {label}
          </label>
        )}
        <input
          ref={ref}
          className={cn(
            "flex h-11 w-full rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-2 text-sm text-white transition-colors file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:border-transparent disabled:cursor-not-allowed disabled:opacity-50",
            error && "border-red-500 focus-visible:ring-red-500",
            className
          )}
          {...props}
        />
        {error && (
          <span className="text-sm text-red-500">
            {error}
          </span>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

export default Input;
