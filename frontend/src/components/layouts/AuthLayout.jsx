import React from 'react';
import { Outlet } from 'react-router-dom';

export default function AuthLayout() {
  return (
    <div className="min-h-full flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8 bg-slate-100">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="inline-flex h-12 w-12 rounded-xl bg-indigo-600 text-white font-bold text-lg items-center justify-center shadow-lg">
          ID
        </div>
        <h2 className="mt-4 text-2xl font-bold text-slate-900 tracking-tight">
          School ID Card Platform
        </h2>
        <p className="mt-1 text-xs text-slate-500">
          Ordering, Admin Brokerage & Printing Agency Fulfillment
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-sm border border-slate-200/80 rounded-xl sm:px-10">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
