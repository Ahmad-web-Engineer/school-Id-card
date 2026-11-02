import React from 'react';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import { Briefcase, Printer, CheckCircle, Truck } from 'lucide-react';

export default function AgencyDashboardPage() {
  const stats = [
    { label: 'Assigned Print Jobs', value: '4', icon: Briefcase, color: 'text-amber-600 bg-amber-50' },
    { label: 'Cards In Production', value: '850', icon: Printer, color: 'text-indigo-600 bg-indigo-50' },
    { label: 'QC Passed', value: '620', icon: CheckCircle, color: 'text-emerald-600 bg-emerald-50' },
    { label: 'Dispatched Batches', value: '3', icon: Truck, color: 'text-sky-600 bg-sky-50' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Printing Agency Dashboard</h1>
          <p className="text-xs text-slate-500 mt-1">
            Fulfillment operations, job batches, quality checks, and dispatch records.
          </p>
        </div>
        <Badge variant="warning">PRINTING AGENCY PORTAL</Badge>
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

      <Card title="Agency Isolation Notice">
        <p className="text-sm text-slate-600 leading-relaxed">
          The Agency portal only displays jobs explicitly assigned to your agency by the Admin. School selling prices,
          client contact details, and other agency assignments are strictly partitioned.
        </p>
      </Card>
    </div>
  );
}
