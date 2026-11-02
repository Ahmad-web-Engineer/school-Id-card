import React from 'react';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import { Users, Palette, ShoppingBag, Truck } from 'lucide-react';

export default function SchoolDashboardPage() {
  const stats = [
    { label: 'Enrolled Students', value: '1,280', icon: Users, color: 'text-indigo-600 bg-indigo-50' },
    { label: 'Approved Templates', value: '3', icon: Palette, color: 'text-purple-600 bg-purple-50' },
    { label: 'Active Orders', value: '2', icon: ShoppingBag, color: 'text-sky-600 bg-sky-50' },
    { label: 'Cards Delivered', value: '1,150', icon: Truck, color: 'text-emerald-600 bg-emerald-50' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">School Portal Dashboard</h1>
          <p className="text-xs text-slate-500 mt-1">
            Student roster management, template customization, orders, and delivery tracking.
          </p>
        </div>
        <Badge variant="info">SCHOOL PORTAL</Badge>
      </div>

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

      <Card title="Role Isolation Notice">
        <p className="text-sm text-slate-600 leading-relaxed">
          The School portal communicates solely with the platform Admin. Printing agency identities, internal job assignments,
          and production cost settlements are strictly isolated and never rendered in school DTOs or screens.
        </p>
      </Card>
    </div>
  );
}
