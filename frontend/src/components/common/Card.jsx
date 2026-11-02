import React from 'react';

export default function Card({
  title,
  subtitle,
  actions,
  children,
  footer,
  className = '',
  bodyClassName = '',
}) {
  return (
    <div className={`bg-white rounded-lg border border-slate-200/80 shadow-sm overflow-hidden ${className}`}>
      {(title || subtitle || actions) && (
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between flex-wrap gap-2">
          <div>
            {title && <h3 className="text-base font-semibold text-slate-900">{title}</h3>}
            {subtitle && <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>}
          </div>
          {actions && <div className="flex items-center gap-2">{actions}</div>}
        </div>
      )}
      <div className={`p-5 ${bodyClassName}`}>{children}</div>
      {footer && <div className="px-5 py-3 bg-slate-50 border-t border-slate-100">{footer}</div>}
    </div>
  );
}
