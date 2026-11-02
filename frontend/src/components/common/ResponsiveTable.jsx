import React from 'react';
import LoadingSpinner from './LoadingSpinner';
import EmptyState from './EmptyState';
import ErrorState from './ErrorState';

export default function ResponsiveTable({
  columns = [],
  data = [],
  keyField = 'id',
  loading = false,
  error = null,
  onRetry,
  emptyTitle = 'No data available',
  emptyMessage = 'No records match the current filter or criteria.',
  className = '',
}) {
  if (loading) {
    return (
      <div className="py-12 flex flex-col items-center justify-center bg-white rounded-lg border border-slate-200">
        <LoadingSpinner size="lg" className="text-indigo-600 mb-3" />
        <p className="text-xs text-slate-500 font-medium">Loading records...</p>
      </div>
    );
  }

  if (error) {
    return (
      <ErrorState
        title="Failed to load records"
        message={error.message || 'An error occurred while fetching table data.'}
        errorCode={error.errorCode}
        onRetry={onRetry}
      />
    );
  }

  if (!data || data.length === 0) {
    return <EmptyState title={emptyTitle} description={emptyMessage} />;
  }

  return (
    <div className={`overflow-hidden rounded-lg border border-slate-200 bg-white ${className}`}>
      {/* Desktop / Tablet Table View (hidden on mobile < 768px) */}
      <div className="hidden md:block overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
          <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wider text-slate-600">
            <tr>
              {columns.map((col) => (
                <th
                  key={col.key}
                  scope="col"
                  className={`px-5 py-3.5 ${col.headerClassName || ''}`}
                >
                  {col.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white text-slate-800">
            {data.map((row, idx) => (
              <tr
                key={row[keyField] || idx}
                className="hover:bg-slate-50/70 transition-colors"
              >
                {columns.map((col) => (
                  <td
                    key={col.key}
                    className={`px-5 py-4 whitespace-nowrap text-sm ${col.cellClassName || ''}`}
                  >
                    {col.render ? col.render(row[col.key], row) : row[col.key]}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Mobile Stacked Card View (visible only on screens < 768px, like 360px) */}
      <div className="block md:hidden divide-y divide-slate-200">
        {data.map((row, idx) => (
          <div key={row[keyField] || idx} className="p-4 space-y-2">
            {columns.map((col) => (
              <div
                key={col.key}
                className="flex items-center justify-between text-xs py-1 border-b border-slate-50 last:border-none"
              >
                <span className="font-semibold text-slate-500">{col.label}</span>
                <span className="font-medium text-slate-900 text-right">
                  {col.render ? col.render(row[col.key], row) : row[col.key]}
                </span>
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
