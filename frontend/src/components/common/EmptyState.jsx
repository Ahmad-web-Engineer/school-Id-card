import React from 'react';
import { Inbox } from 'lucide-react';

export default function EmptyState({
  title = 'No items found',
  description = 'There are no records to display at this time.',
  icon: Icon = Inbox,
  action,
  className = '',
}) {
  return (
    <div className={`text-center py-12 px-4 rounded-lg border-2 border-dashed border-slate-200 bg-white ${className}`}>
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-500">
        <Icon className="h-6 w-6" aria-hidden="true" />
      </div>
      <h3 className="mt-3 text-sm font-semibold text-slate-900">{title}</h3>
      <p className="mt-1 text-xs text-slate-500 max-w-sm mx-auto">{description}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
