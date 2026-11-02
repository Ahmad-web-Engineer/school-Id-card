import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import {
  Briefcase,
  Printer,
  CheckCircle,
  Truck,
  IndianRupee,
  Layers,
  ArrowRight,
  AlertCircle,
  RefreshCw,
  ShieldCheck,
  CheckCircle2,
} from 'lucide-react';

export default function AgencyDashboardPage() {
  const navigate = useNavigate();

  const { data: statsData, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['agency-dashboard-stats'],
    queryFn: async () => {
      const res = await api.get('/agency/dashboard/stats');
      return res.data;
    },
    refetchInterval: 30000,
  });

  const stats = statsData || {
    totalJobs: 0,
    activeJobs: 0,
    completedJobs: 0,
    productionCapacity: 0,
    totalEarnings: '0.00',
    recentJobs: [],
  };

  const formattedEarnings = Number(stats.totalEarnings || 0).toLocaleString('en-IN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
  });

  const kpis = [
    {
      label: 'Assigned Print Jobs',
      value: stats.totalJobs,
      subtext: `${stats.activeJobs} active in production queue`,
      icon: Briefcase,
      color: 'text-amber-600 bg-amber-50 border-amber-100',
      action: () => navigate('/agency/jobs'),
    },
    {
      label: 'Cards In Production',
      value: stats.activeJobs,
      subtext: 'Batches pending print & QC',
      icon: Printer,
      color: 'text-indigo-600 bg-indigo-50 border-indigo-100',
      action: () => navigate('/agency/jobs'),
    },
    {
      label: 'Completed Batches',
      value: stats.completedJobs,
      subtext: 'QC passed and dispatched',
      icon: CheckCircle,
      color: 'text-emerald-600 bg-emerald-50 border-emerald-100',
      action: () => navigate('/agency/jobs'),
    },
    {
      label: 'Fulfillment Earnings',
      value: `₹${formattedEarnings}`,
      subtext: 'Settled production payouts',
      icon: IndianRupee,
      color: 'text-sky-600 bg-sky-50 border-sky-100',
      action: () => navigate('/agency/finance'),
    },
  ];

  const getStatusBadgeVariant = (status) => {
    switch (status) {
      case 'ASSIGNED':
        return 'warning';
      case 'AGENCY_ACCEPTED':
      case 'IN_PRODUCTION':
        return 'brand';
      case 'COMPLETED':
      case 'DISPATCHED':
      case 'DELIVERED':
        return 'success';
      case 'REJECTED':
      case 'CANCELLED':
        return 'danger';
      default:
        return 'neutral';
    }
  };

  const recentJobColumns = [
    {
      key: 'jobId',
      label: 'Job Reference',
      render: (val) => (
        <span className="font-mono text-xs font-bold text-amber-700">
          JOB-{String(val).substring(0, 8).toUpperCase()}
        </span>
      ),
    },
    {
      key: 'totalQuantity',
      label: 'Assigned Quantity',
      render: (val) => `${val || 0} cards`,
    },
    {
      key: 'status',
      label: 'Status',
      render: (val) => (
        <Badge variant={getStatusBadgeVariant(val)} size="sm">
          {val}
        </Badge>
      ),
    },
    {
      key: 'assignedAt',
      label: 'Assigned Date',
      render: (val) => (val ? new Date(val).toLocaleDateString() : '—'),
    },
    {
      key: 'actions',
      label: 'Action',
      render: (_, row) => (
        <Button
          size="sm"
          variant="outline"
          onClick={() => navigate('/agency/jobs')}
          title="Open Job Details"
        >
          Open Job
        </Button>
      ),
    },
  ];

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="h-8 w-64 bg-slate-200 animate-pulse rounded" />
          <div className="h-6 w-32 bg-slate-200 animate-pulse rounded" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-28 bg-white border border-slate-200 rounded-lg p-5 animate-pulse" />
          ))}
        </div>
        <div className="h-64 bg-white border border-slate-200 rounded-lg p-6 animate-pulse" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="p-6 bg-rose-50 border border-rose-200 rounded-lg text-rose-800 space-y-3">
        <div className="flex items-center gap-2 font-semibold">
          <AlertCircle className="h-5 w-5 text-rose-600" />
          <span>Failed to load agency operations dashboard</span>
        </div>
        <p className="text-xs text-rose-700">{error?.message || 'Network error encountered.'}</p>
        <Button size="sm" variant="outline" onClick={() => refetch()} className="flex items-center gap-1.5">
          <RefreshCw className="h-3.5 w-3.5" />
          <span>Retry Loading</span>
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Printing Agency Operations</h1>
          <p className="text-xs text-slate-500 mt-1">
            Assigned print job batches, quality control inspection, package dispatch, and settlement invoices.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="warning">PRINTING AGENCY PORTAL</Badge>
          <Button size="sm" variant="ghost" onClick={() => refetch()} title="Refresh live telemetry">
            <RefreshCw className="h-3.5 w-3.5" />
          </Button>
        </div>
      </div>

      {/* Production Capacity Indicator */}
      <div className="p-4 bg-white border border-slate-200/90 rounded-lg flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-indigo-50 text-indigo-700 rounded-lg border border-indigo-100">
            <Layers className="h-5 w-5" />
          </div>
          <div>
            <div className="text-sm font-bold text-slate-800">
              Daily Rated Capacity: {stats.productionCapacity ? stats.productionCapacity.toLocaleString('en-IN') : '5,000'} Cards / Day
            </div>
            <div className="text-xs text-slate-500">
              Allocated capacity automatically guides Admin automated job scheduling and dispatch load.
            </div>
          </div>
        </div>
        <Badge variant="brand">VERIFIED PRINT PARTNER</Badge>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {kpis.map((kpi) => {
          const Icon = kpi.icon;
          return (
            <div
              key={kpi.label}
              onClick={kpi.action}
              className="bg-white p-5 rounded-lg border border-slate-200/90 shadow-sm hover:shadow-md transition-shadow cursor-pointer flex flex-col justify-between"
            >
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-xs font-medium text-slate-500">{kpi.label}</p>
                  <p className="text-2xl font-bold text-slate-900 mt-1">{kpi.value}</p>
                </div>
                <div className={`p-2.5 rounded-lg border ${kpi.color}`}>
                  <Icon className="h-5 w-5" />
                </div>
              </div>
              <p className="text-[11px] text-slate-400 mt-3 flex items-center justify-between">
                <span>{kpi.subtext}</span>
                <ArrowRight className="h-3 w-3 text-slate-400" />
              </p>
            </div>
          );
        })}
      </div>

      {/* Quick Agency Workflow Links */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
        <button
          type="button"
          onClick={() => navigate('/agency/jobs')}
          className="p-3 bg-white border border-slate-200 rounded-lg text-left hover:border-amber-400 hover:bg-amber-50/30 transition-colors flex items-center gap-3"
        >
          <div className="p-2 bg-amber-50 text-amber-700 rounded-md">
            <Briefcase className="h-4 w-4" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">Job Batches</div>
            <div className="text-[11px] text-slate-500">Accept & Download PDFs</div>
          </div>
        </button>

        <button
          type="button"
          onClick={() => navigate('/agency/jobs')}
          className="p-3 bg-white border border-slate-200 rounded-lg text-left hover:border-emerald-400 hover:bg-emerald-50/30 transition-colors flex items-center gap-3"
        >
          <div className="p-2 bg-emerald-50 text-emerald-700 rounded-md">
            <CheckCircle2 className="h-4 w-4" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">Quality Check (QC)</div>
            <div className="text-[11px] text-slate-500">Log Passed / Failed Cards</div>
          </div>
        </button>

        <button
          type="button"
          onClick={() => navigate('/agency/delivery')}
          className="p-3 bg-white border border-slate-200 rounded-lg text-left hover:border-sky-400 hover:bg-sky-50/30 transition-colors flex items-center gap-3"
        >
          <div className="p-2 bg-sky-50 text-sky-700 rounded-md">
            <Truck className="h-4 w-4" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">Dispatch Packages</div>
            <div className="text-[11px] text-slate-500">Courier Tracking & Proof</div>
          </div>
        </button>
      </div>

      {/* Recent Assigned Jobs Stream */}
      <Card
        title="Recent Assigned Job Batches"
        actions={
          <Button size="sm" variant="outline" onClick={() => navigate('/agency/jobs')}>
            View All Jobs
          </Button>
        }
      >
        <ResponsiveTable
          columns={recentJobColumns}
          data={stats.recentJobs || []}
          keyField="jobId"
          emptyMessage="No print jobs have been assigned to your agency yet."
        />
      </Card>

      {/* Strict Role Isolation Notice */}
      <div className="p-4 bg-slate-50 border border-slate-200 rounded-lg flex items-start gap-3">
        <ShieldCheck className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
        <div className="text-xs text-slate-600 leading-relaxed">
          <span className="font-semibold text-slate-800">Agency Data Isolation:</span> Your operations strictly interface with
          Admin-assigned manufacturing packages. School retail pricing, billing terms, and other printing vendor operations are
          strictly partitioned and inaccessible.
        </div>
      </div>
    </div>
  );
}
