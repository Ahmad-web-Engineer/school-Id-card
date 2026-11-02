import React from 'react';

const badgeVariants = {
  success: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  warning: 'bg-amber-50 text-amber-700 border-amber-200',
  danger: 'bg-rose-50 text-rose-700 border-rose-200',
  info: 'bg-sky-50 text-sky-700 border-sky-200',
  neutral: 'bg-slate-100 text-slate-700 border-slate-200',
  brand: 'bg-indigo-50 text-indigo-700 border-indigo-200',
};

const badgeSizes = {
  sm: 'px-2 py-0.5 text-[11px]',
  md: 'px-2.5 py-1 text-xs',
};

export default function Badge({
  children,
  variant = 'neutral',
  size = 'md',
  className = '',
}) {
  return (
    <span
      className={`inline-flex items-center font-medium rounded-full border ${
        badgeVariants[variant] || badgeVariants.neutral
      } ${badgeSizes[size] || badgeSizes.md} ${className}`}
    >
      {children}
    </span>
  );
}
