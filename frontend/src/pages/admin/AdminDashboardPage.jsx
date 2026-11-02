import React from 'react';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import { School, Building2, ShoppingBag, DollarSign } from 'lucide-react';

export default function AdminDashboardPage() {
  const stats = [
    { label: 'Registered Schools', value: '42', icon: School, color: 'text-indigo-600 bg-indigo-50' },
    { label: 'Printing Agencies', value: '8', icon: Building2, color: 'text-amber-600 bg-amber-50' },
    { label: 'Active Parent Orders', value: '19', icon: ShoppingBag, color: 'text-sky-600 bg-sky-50' },
    { label: 'Gross Revenue (YTD)', value: '₹14,80,200', icon: DollarSign, color: 'text-emerald-600 bg-emerald-50' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Admin Platform Dashboard</h1>
          <p className="text-xs text-slate-500 mt-1">
            Super Admin operations, order brokerage, pricing control, and agency assignments.
          </p>
        </div>
        <Badge variant="brand">SUPER_ADMIN PORTAL</Badge>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <div key={stat.label} className="bg-white p-5 rounded-lg border border-slate-200/80 shadow-sm flex items-center gap-4">
              <div className={`p-3 rounded-lg ${stat.color}`}>
                <Icon className="h-6 w-6" />
              </div>
              <div>
                <p className="text-xs font-medium text-slate-500">{stat.label}</p>
                <p className="text-lg font-bold text-slate-900 mt-0.5">{stat.value}</p>
              </div>
            </div>
          );
        })}
      </div>

      <Card title="Step 0 Foundation Check">
        <p className="text-sm text-slate-600 leading-relaxed">
          The Super Admin portal skeleton is initialized with role-based routing, responsive layout containers,
          strict separation of agency pricing from school visibility, and standard exception contracts.
        </p>
      </Card>
    </div>
  );
}
