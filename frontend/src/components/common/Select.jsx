import React, { forwardRef } from 'react';

const Select = forwardRef(function Select(
  {
    label,
    id,
    name,
    options = [],
    error,
    helperText,
    required = false,
    disabled = false,
    placeholder = 'Select an option',
    className = '',
    children,
    ...props
  },
  ref
) {
  const selectId = id || name;
  const errorId = error ? `${selectId}-error` : undefined;
  const helperId = helperText ? `${selectId}-helper` : undefined;

  return (
    <div className="w-full">
      {label && (
        <label htmlFor={selectId} className="block text-xs font-semibold text-slate-700 mb-1">
          {label}
          {required && <span className="text-rose-500 ml-0.5">*</span>}
        </label>
      )}
      <select
        ref={ref}
        id={selectId}
        name={name}
        disabled={disabled}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={errorId || helperId}
        className={`w-full rounded-md border px-3 py-2 text-sm text-slate-900 transition-colors focus:outline-none focus:ring-2 focus:ring-offset-1 ${
          error
            ? 'border-rose-400 focus:border-rose-500 focus:ring-rose-400 bg-rose-50/30'
            : 'border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 bg-white'
        } ${disabled ? 'bg-slate-100 text-slate-500 cursor-not-allowed' : ''} ${className}`}
        {...props}
      >
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.length > 0
          ? options.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))
          : children}
      </select>
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

export default Select;
