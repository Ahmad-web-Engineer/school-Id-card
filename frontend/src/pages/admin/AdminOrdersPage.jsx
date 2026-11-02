import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { ORDER_STATUSES } from '../../features/orders/ordersConstants';
import {
  ShoppingBag,
  Search,
  Filter,
  Clock,
  CheckCircle,
  FileText,
  AlertCircle,
  Eye,
  School,
  ArrowRight
} from 'lucide-react';

export default function AdminOrdersPage() {
  const navigate = useNavigate();

  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [statusFilter, setStatusFilter] = useState('');
  const [schoolFilter, setSchoolFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // 1. Fetch schools for filter dropdown
  const { data: schoolsRes } = useQuery({
    queryKey: ['admin-schools-list'],
    queryFn: () => api.get('/admin/schools', { params: { page: 0, size: 200 } }),
  });

  // 2. Fetch admin orders list
  const {
    data: ordersRes,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['admin-orders', page, size, statusFilter, schoolFilter],
    queryFn: () =>
      api.get('/admin/orders', {
        params: {
          page,
          size,
          status: statusFilter || undefined,
          schoolId: schoolFilter || undefined,
        },
      }),
  });

  const schools = schoolsRes?.data?.content || [];
  const orders = ordersRes?.data?.content || [];
  const totalPages = ordersRes?.data?.totalPages || 0;
  const totalElements = ordersRes?.data?.totalElements || 0;

  // Filter local search query
  const displayOrders = orders.filter((o) =>
    searchQuery
      ? o.orderNumber?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        o.schoolName?.toLowerCase().includes(searchQuery.toLowerCase())
      : true
  );

  // Compute stats
  const submittedCount = orders.filter((o) => ['SUBMITTED', 'UNDER_REVIEW'].includes(o.orderStatus)).length;
  const verifiedCount = orders.filter((o) => o.orderStatus === 'VERIFIED').length;
  const approvedCount = orders.filter((o) => o.orderStatus === 'APPROVED').length;

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
          <ShoppingBag className="h-6 w-6 text-indigo-600" />
          Order Management & Verification
        </h1>
        <p className="text-xs text-slate-500 mt-1">
          Review school order batches, verify student details, generate quotations, and queue production.
        </p>
      </div>

      {/* Metrics Banner */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Total Orders</p>
              <h3 className="text-2xl font-bold text-slate-900 mt-1">{totalElements}</h3>
            </div>
            <div className="h-10 w-10 rounded-full bg-slate-100 flex items-center justify-center text-slate-600">
              <ShoppingBag className="h-5 w-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Awaiting Verification</p>
              <h3 className="text-2xl font-bold text-sky-600 mt-1">{submittedCount}</h3>
            </div>
            <div className="h-10 w-10 rounded-full bg-sky-50 flex items-center justify-center text-sky-600">
              <Clock className="h-5 w-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Verified (Ready for Quote)</p>
              <h3 className="text-2xl font-bold text-indigo-600 mt-1">{verifiedCount}</h3>
            </div>
            <div className="h-10 w-10 rounded-full bg-indigo-50 flex items-center justify-center text-indigo-600">
              <FileText className="h-5 w-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Approved Orders</p>
              <h3 className="text-2xl font-bold text-emerald-600 mt-1">{approvedCount}</h3>
            </div>
            <div className="h-10 w-10 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-600">
              <CheckCircle className="h-5 w-5" />
            </div>
          </div>
        </Card>
      </div>

      {/* Filter and Search Bar */}
      <Card className="p-4">
        <div className="flex flex-col lg:flex-row items-center justify-between gap-3">
          <div className="relative flex-1 w-full">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search by order number or school name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="flex flex-col sm:flex-row items-center gap-2 w-full lg:w-auto">
            {/* School Filter */}
            <select
              value={schoolFilter}
              onChange={(e) => {
                setSchoolFilter(e.target.value);
                setPage(0);
              }}
              className="text-xs px-3 py-1.5 rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 w-full sm:w-48"
            >
              <option value="">All Schools</option>
              {schools.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>

            {/* Status Filter */}
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="text-xs px-3 py-1.5 rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 w-full sm:w-44"
            >
              <option value="">All Statuses</option>
              <option value="DRAFT">Draft</option>
              <option value="SUBMITTED">Submitted</option>
              <option value="UNDER_REVIEW">Under Review</option>
              <option value="CORRECTION_REQUIRED">Correction Required</option>
              <option value="VERIFIED">Verified</option>
              <option value="QUOTATION_GENERATED">Quotation Ready</option>
              <option value="APPROVED">Approved</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Admin Orders Table */}
      {isLoading ? (
        <Card className="py-12 flex justify-center">
          <LoadingSpinner size="lg" className="text-indigo-600" />
        </Card>
      ) : isError ? (
        <ErrorState
          title="Could not load orders"
          message={error?.message || 'Failed to fetch platform orders.'}
          onRetry={refetch}
        />
      ) : displayOrders.length === 0 ? (
        <Card className="p-12 text-center">
          <EmptyState
            title="No orders found"
            description="No orders match the selected filters or search terms."
          />
        </Card>
      ) : (
        <div className="space-y-3">
          <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
            <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
              <thead className="bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Order Number</th>
                  <th className="px-4 py-3">School Name</th>
                  <th className="px-4 py-3">Template</th>
                  <th className="px-4 py-3">Students</th>
                  <th className="px-4 py-3">Total Qty</th>
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-800">
                {displayOrders.map((order) => {
                  const statusConfig = ORDER_STATUSES[order.orderStatus] || {
                    label: order.orderStatus,
                    variant: 'neutral',
                  };
                  return (
                    <tr
                      key={order.id}
                      className="hover:bg-slate-50 transition-colors cursor-pointer"
                      onClick={() => navigate(`/admin/orders/${order.id}`)}
                    >
                      <td className="px-4 py-3 font-semibold text-indigo-600 font-mono">
                        {order.orderNumber}
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-900">
                        {order.schoolName}
                      </td>
                      <td className="px-4 py-3 text-slate-600">{order.templateName || 'Standard'}</td>
                      <td className="px-4 py-3">{order.totalStudents} cards</td>
                      <td className="px-4 py-3 font-semibold">{order.totalQuantity} items</td>
                      <td className="px-4 py-3 text-slate-500">
                        {new Date(order.createdAt).toLocaleDateString('en-IN', {
                          day: '2-digit',
                          month: 'short',
                          year: 'numeric',
                        })}
                      </td>
                      <td className="px-4 py-3">
                        <Badge variant={statusConfig.variant} size="sm">
                          {statusConfig.label}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-right" onClick={(e) => e.stopPropagation()}>
                        <Button
                          size="xs"
                          variant="outline"
                          onClick={() => navigate(`/admin/orders/${order.id}`)}
                        >
                          <Eye className="h-3 w-3 mr-1" />
                          Review
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between text-xs text-slate-500 px-2">
              <span>
                Showing {page * size + 1} to {Math.min((page + 1) * size, totalElements)} of {totalElements} orders
              </span>
              <div className="flex items-center space-x-2">
                <Button
                  size="xs"
                  variant="outline"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Previous
                </Button>
                <span>Page {page + 1} of {totalPages}</span>
                <Button
                  size="xs"
                  variant="outline"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
