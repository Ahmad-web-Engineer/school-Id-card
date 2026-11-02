import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import Button from './Button';

export default function ErrorState({
  title = 'Something went wrong',
  message = 'An unexpected error occurred while loading this section.',
  errorCode,
  onRetry,
  className = '',
}) {
  return (
    <div className={`p-6 rounded-lg border border-rose-200 bg-rose-50/50 text-center ${className}`}>
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-rose-100 text-rose-600">
        <AlertTriangle className="h-6 w-6" aria-hidden="true" />
      </div>
      <h3 className="mt-3 text-sm font-semibold text-rose-900">{title}</h3>
      <p className="mt-1 text-xs text-rose-700 max-w-md mx-auto">{message}</p>
      {errorCode && (
        <p className="mt-1 text-[11px] font-mono text-rose-500">
          Code: {errorCode}
        </p>
      )}
      {onRetry && (
        <div className="mt-4">
          <Button variant="danger" size="sm" onClick={onRetry}>
            <RefreshCw className="mr-1.5 h-3.5 w-3.5" />
            Try again
          </Button>
        </div>
      )}
    </div>
  );
}
