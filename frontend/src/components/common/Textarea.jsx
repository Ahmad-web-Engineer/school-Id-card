import React, { forwardRef } from 'react';

const Textarea = forwardRef(function Textarea(
  {
    label,
    id,
    name,
    rows = 3,
    error,
    helperText,
    required = false,
    disabled = false,
    className = '',
    ...props
  },
  ref
) {
  const textareaId = id || name;
  const errorId = error ? `${textareaId}-error` : undefined;
  const helperId = helperText ? `${textareaId}-helper` : undefined;

  return (
    <div className="w-full">
      {label && (
        <label htmlFor={textareaId} className="block text-xs font-semibold text-slate-700 mb-1">
          {label}
          {required && <span className="text-rose-500 ml-0.5">*</span>}
        </label>
      )}
      <textarea
        ref={ref}
        id={textareaId}
        name={name}
        rows={rows}
        disabled={disabled}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={errorId || helperId}
        className={`w-full rounded-md border px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 transition-colors focus:outline-none focus:ring-2 focus:ring-offset-1 ${
          error
            ? 'border-rose-400 focus:border-rose-500 focus:ring-rose-400 bg-rose-50/30'
            : 'border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 bg-white'
        } ${disabled ? 'bg-slate-100 text-slate-500 cursor-not-allowed' : ''} ${className}`}
        {...props}
      />
      {error ? (
        <p id={errorId} className="mt-1 text-xs text-rose-600 font-medium">
          {error}
        </p>
      ) : helperText ? (
        <p id={helperId} className="mt-1 text-xs text-slate-500">
          {helperText}
        </p>
      ) : null}
    </div>
  );
});

export default Textarea;
