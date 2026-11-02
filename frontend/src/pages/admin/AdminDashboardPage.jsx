import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import { 
  School, 
  Building2, 
  Users, 
  ShoppingBag, 
  IndianRupee, 
  ArrowRight, 
  Palette, 
  AlertCircle, 
  RefreshCw,
  Clock,
  CheckCircle,
  Truck
} from 'lucide-react';

export default function AdminDashboardPage() {
  const navigate = useNavigate();

  const { data: statsData, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['admin-dashboard-stats'],
    queryFn: async () => {
      const res = await api.get('/admin/dashboard/stats');
      return res.data;
    },
    refetchInterval: 30000, // live refresh every 30 seconds
  });

  const stats = statsData || {
    totalSchools: 0,
    totalAgencies: 0,
    totalStudents: 0,
    totalOrders: 0,
    activeOrders: 0,
    completedOrders: 0,
    totalRevenue: '0.00',
    recentOrders: [],
    statusDistribution: {},
  };

  const formattedRevenue = Number(stats.totalRevenue || 0).toLocaleString('en-IN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
  });

  const kpis = [
    {
      label: 'Registered Schools',
      value: stats.totalSchools,
      subtext: 'Active educational institutions',
      icon: School,
      color: 'text-indigo-600 bg-indigo-50 border-indigo-100',
      action: () => navigate('/admin/schools'),
    },
    {
      label: 'Printing Agencies',
      value: stats.totalAgencies,
      subtext: 'Verified production partners',
      icon: Building2,
      color: 'text-amber-600 bg-amber-50 border-amber-100',
      action: () => navigate('/admin/agencies'),
    },
    {
      label: 'Total Students',
      value: stats.totalStudents,
      subtext: 'Enrolled student records',
      icon: Users,
      color: 'text-sky-600 bg-sky-50 border-sky-100',
      action: () => navigate('/admin/schools'),
    },
    {
      label: 'Gross Platform Revenue',
      value: `₹${formattedRevenue}`,
      subtext: `${stats.completedOrders} completed orders fulfilled`,
      icon: IndianRupee,
      color: 'text-emerald-600 bg-emerald-50 border-emerald-100',
      action: () => navigate('/admin/orders'),
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
      key: 'schoolName',
      label: 'School',
      cellClassName: 'font-semibold text-slate-800',
    },
    {
      key: 'totalStudents',
      label: 'Students',
      render: (val) => `${val || 0} enrolled`,
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
      label: 'Created Date',
      render: (val) => (val ? new Date(val).toLocaleDateString() : '—'),
    },
    {
      key: 'actions',
      label: 'Action',
      render: (_, row) => (
        <Button
          size="sm"
          variant="outline"
          onClick={() => navigate(`/admin/orders`)}
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
          <span>Failed to load platform dashboard metrics</span>
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
          <h1 className="text-xl font-bold text-slate-900">Platform Admin Operations</h1>
          <p className="text-xs text-slate-500 mt-1">
            Real-time multi-tenant telemetry, school management, fulfillment tracking, and agency operations.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="brand">SUPER_ADMIN PORTAL</Badge>
          <Button size="sm" variant="ghost" onClick={() => refetch()} title="Refresh live telemetry">
            <RefreshCw className="h-3.5 w-3.5" />
          </Button>
        </div>
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

      {/* Quick Admin Actions */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <button
          type="button"
          onClick={() => navigate('/admin/schools')}
          className="p-3 bg-white border border-slate-200 rounded-lg text-left hover:border-indigo-400 hover:bg-indigo-50/30 transition-colors flex items-center gap-3"
        >
          <div className="p-2 bg-indigo-50 text-indigo-700 rounded-md">
            <School className="h-4 w-4" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">Schools</div>
            <div className="text-[11px] text-slate-500">Manage & Provision</div>
          </div>
        </button>

        <button
          type="button"
          onClick={() => navigate('/admin/agencies')}
          className="p-3 bg-white border border-slate-200 rounded-lg text-left hover:border-amber-400 hover:bg-amber-50/30 transition-colors flex items-center gap-3"
        >
          <div className="p-2 bg-amber-50 text-amber-700 rounded-md">
            <Building2 className="h-4 w-4" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">Agencies</div>
            <div className="text-[11px] text-slate-500">Capacity & Partner</div>
          </div>
        </button>

        <button
          type="button"
          onClick={() => navigate('/admin/templates')}
          className="p-3 bg-white border border-slate-200 rounded-lg text-left hover:border-purple-400 hover:bg-purple-50/30 transition-colors flex items-center gap-3"
        >
          <div className="p-2 bg-purple-50 text-purple-700 rounded-md">
            <Palette className="h-4 w-4" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">Card Designer</div>
            <div className="text-[11px] text-slate-500">Templates & Specs</div>
          </div>
        </button>

        <button
          type="button"
          onClick={() => navigate('/admin/orders')}
          className="p-3 bg-white border border-slate-200 rounded-lg text-left hover:border-emerald-400 hover:bg-emerald-50/30 transition-colors flex items-center gap-3"
        >
          <div className="p-2 bg-emerald-50 text-emerald-700 rounded-md">
            <ShoppingBag className="h-4 w-4" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">Orders Pipeline</div>
            <div className="text-[11px] text-slate-500">Brokerage & Fulfillment</div>
          </div>
        </button>
      </div>

      {/* Pipeline Status Breakdown */}
      {stats.statusDistribution && Object.keys(stats.statusDistribution).length > 0 && (
        <Card title="Orders Fulfillment Status Distribution">
          <div className="flex flex-wrap gap-2 pt-1">
            {Object.entries(stats.statusDistribution).map(([statusKey, count]) => (
              <div
                key={statusKey}
                className="px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-lg flex items-center gap-2 text-xs"
              >
                <Badge variant={getStatusBadgeVariant(statusKey)} size="sm">
                  {statusKey}
                </Badge>
                <span className="font-bold text-slate-900">{count}</span>
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* Recent Orders Stream */}
      <Card
        title="Recent Orders Stream"
        actions={
          <Button size="sm" variant="outline" onClick={() => navigate('/admin/orders')}>
            View All Orders
          </Button>
        }
      >
        <ResponsiveTable
          columns={recentOrderColumns}
          data={stats.recentOrders || []}
          keyField="orderId"
          emptyMessage="No orders have been submitted yet on the platform."
        />
      </Card>
    </div>
  );
}
