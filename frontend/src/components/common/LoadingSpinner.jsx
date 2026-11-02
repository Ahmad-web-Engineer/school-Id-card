import React from 'react';

const sizeMap = {
  sm: 'h-4 w-4 border-2',
  md: 'h-6 w-6 border-2',
  lg: 'h-8 w-8 border-3',
  xl: 'h-12 w-12 border-4',
};

export default function LoadingSpinner({ size = 'md', className = '' }) {
  const sizeClass = sizeMap[size] || sizeMap.md;

  return (
    <div
      role="status"
      aria-label="Loading"
      className={`inline-block animate-spin rounded-full border-solid border-current border-r-transparent align-[-0.125em] motion-reduce:animate-[spin_1.5s_linear_infinite] ${sizeClass} ${className}`}
    >
      <span className="sr-only">Loading...</span>
    </div>
  );
}
