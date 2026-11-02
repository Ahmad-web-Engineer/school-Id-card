import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import {
  Users,
  Palette,
  ShoppingBag,
  Truck,
  ArrowRight,
  PlusCircle,
  FileSpreadsheet,
  AlertCircle,
  RefreshCw,
  FileCheck,
  ShieldCheck,
} from 'lucide-react';

export default function SchoolDashboardPage() {
  const navigate = useNavigate();

  const { data: statsData, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['school-dashboard-stats'],
    queryFn: async () => {
      const res = await api.get('/school/dashboard/stats');
      return res.data;
    },
    refetchInterval: 30000,
  });

  const stats = statsData || {
    enrolledStudents: 0,
    approvedTemplates: 0,
    totalOrders: 0,
    activeOrders: 0,
    deliveredCards: 0,
    pendingQuotations: 0,
    recentOrders: [],
  };

  const kpis = [
    {
      label: 'Enrolled Students',
      value: stats.enrolledStudents,
      subtext: 'Roster ready for card issuance',
      icon: Users,
      color: 'text-indigo-600 bg-indigo-50 border-indigo-100',
      action: () => navigate('/school/students'),
    },
    {
      label: 'Approved Templates',
      value: stats.approvedTemplates,
      subtext: 'Available card formats',
      icon: Palette,
      color: 'text-purple-600 bg-purple-50 border-purple-100',
      action: () => navigate('/school/templates'),
    },
    {
      label: 'Active Orders',
      value: stats.activeOrders,
      subtext: 'In verification / production',
      icon: ShoppingBag,
      color: 'text-sky-600 bg-sky-50 border-sky-100',
      action: () => navigate('/school/orders'),
    },
    {
      label: 'Delivered Cards',
      value: stats.deliveredCards,
      subtext: 'Completed card distribution',
      icon: Truck,
      color: 'text-emerald-600 bg-emerald-50 border-emerald-100',
      action: () => navigate('/school/orders'),
    },
  ];

  const getStatusBadgeVariant = (status) => {
    switch (status) {
      case 'SUBMITTED':
      case 'UNDER_REVIEW':
        return 'warning';
      case 'VERIFIED':
      case 'QUOTATION_GENERATED':
      case 'APPROVED':
        return 'info';
      case 'IN_PRODUCTION':
      case 'PRINTED':
      case 'QC_PASSED':
        return 'brand';
      case 'DISPATCHED':
      case 'DELIVERED':
      case 'COMPLETED':
        return 'success';
      case 'CANCELLED':
      case 'CORRECTION_REQUIRED':
        return 'danger';
      default:
        return 'neutral';
    }
  };

  const recentOrderColumns = [
    {
      key: 'orderNumber',
      label: 'Order #',
      cellClassName: 'font-mono text-xs font-bold text-indigo-700',
    },
    {
      key: 'totalStudents',
      label: 'Students Included',
      render: (val) => `${val || 0} students`,
    },
    {
      key: 'totalQuantity',
      label: 'Card Quantity',
      render: (val) => `${val || 0} pcs`,
    },
    {
      key: 'orderStatus',
      label: 'Status',
      render: (val) => (
        <Badge variant={getStatusBadgeVariant(val)} size="sm">
          {val}
        </Badge>
      ),
    },
    {
      key: 'createdAt',
      label: 'Order Date',
      render: (val) => (val ? new Date(val).toLocaleDateString() : '—'),
    },
    {
      key: 'actions',
      label: 'Action',
      render: (_, row) => (
        <Button
          size="sm"
          variant="outline"
          onClick={() => navigate('/school/orders')}
          title="View Order Details"
        >
          View Order
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
          <span>Failed to load school dashboard metrics</span>
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
          <h1 className="text-xl font-bold text-slate-900">School Portal Operations</h1>
          <p className="text-xs text-slate-500 mt-1">
            Student roster data, card template selection, print orders, and fulfillment tracking.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="info">SCHOOL PORTAL</Badge>
          <Button size="sm" variant="ghost" onClick={() => refetch()} title="Refresh live telemetry">
            <RefreshCw className="h-3.5 w-3.5" />
          </Button>
        </div>
      </div>

      {/* Pending Quotations Banner */}
      {stats.pendingQuotations > 0 && (
        <div className="p-4 bg-amber-50 border border-amber-200 rounded-lg flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-amber-100 text-amber-800 rounded-md">
              <FileCheck className="h-5 w-5" />
            </div>
            <div>
              <p className="text-sm font-bold text-amber-900">
                {stats.pendingQuotations} Quotation{stats.pendingQuotations > 1 ? 's' : ''} Ready for Approval
              </p>
              <p className="text-xs text-amber-700 mt-0.5">
                Admin has reviewed your order specifications and issued a quotation for approval.
              </p>
            </div>
          </div>
          <Button size="sm" variant="primary" onClick={() => navigate('/school/orders')}>
            Review & Approve
          </Button>
        </div>
      )}

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

      {/* Quick Action Navigation */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <button
          type="button"
          onClick={() => navigate('/school/students')}
          className="p-3 bg-white border border-slate-200 rounded-lg text-left hover:border-indigo-400 hover:bg-indigo-50/30 transition-colors flex items-center gap-3"
        >
          <div className="p-2 bg-indigo-50 text-indigo-700 rounded-md">
            <Users className="h-4 w-4" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">Student Roster</div>
            <div className="text-[11px] text-slate-500">View & Search</div>
          </div>
        </button>

        <button
          type="button"
          onClick={() => navigate('/school/students/import')}
          className="p-3 bg-white border border-slate-200 rounded-lg text-left hover:border-purple-400 hover:bg-purple-50/30 transition-colors flex items-center gap-3"
        >
          <div className="p-2 bg-purple-50 text-purple-700 rounded-md">
            <FileSpreadsheet className="h-4 w-4" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">Import Data</div>
            <div className="text-[11px] text-slate-500">Excel & ZIP Photos</div>
          </div>
        </button>

        <button
          type="button"
          onClick={() => navigate('/school/templates')}
          className="p-3 bg-white border border-slate-200 rounded-lg text-left hover:border-sky-400 hover:bg-sky-50/30 transition-colors flex items-center gap-3"
        >
          <div className="p-2 bg-sky-50 text-sky-700 rounded-md">
            <Palette className="h-4 w-4" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">Templates</div>
            <div className="text-[11px] text-slate-500">Preview with Students</div>
          </div>
        </button>

        <button
          type="button"
          onClick={() => navigate('/school/orders')}
          className="p-3 bg-white border border-slate-200 rounded-lg text-left hover:border-emerald-400 hover:bg-emerald-50/30 transition-colors flex items-center gap-3"
        >
          <div className="p-2 bg-emerald-50 text-emerald-700 rounded-md">
            <PlusCircle className="h-4 w-4" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">Order Cards</div>
            <div className="text-[11px] text-slate-500">Submit New Batch</div>
          </div>
        </button>
      </div>

      {/* Recent Orders Stream */}
      <Card
        title="Recent Print Orders"
        actions={
          <Button size="sm" variant="outline" onClick={() => navigate('/school/orders')}>
            View All Orders
          </Button>
        }
      >
        <ResponsiveTable
          columns={recentOrderColumns}
          data={stats.recentOrders || []}
          keyField="orderId"
          emptyMessage="No print orders have been placed yet. Go to 'Order Cards' to start your first ID batch."
        />
      </Card>

      {/* Role Isolation Confirmation */}
      <div className="p-4 bg-slate-50 border border-slate-200 rounded-lg flex items-start gap-3">
        <ShieldCheck className="h-5 w-5 text-indigo-600 shrink-0 mt-0.5" />
        <div className="text-xs text-slate-600 leading-relaxed">
          <span className="font-semibold text-slate-800">Privacy & Security Isolation:</span> Your institution operates directly
          with the platform Admin. Printing agency identities, internal job allocation, and manufacturing rates are strictly confidential
          and separated from school data.
        </div>
      </div>
    </div>
  );
}
