import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useLocation } from 'react-router-dom';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Input from '../../components/common/Input';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import NewOrderWizardModal from '../../features/orders/NewOrderWizardModal';
import { ORDER_STATUSES } from '../../features/orders/ordersConstants';
import {
  ShoppingBag,
  PlusCircle,
  Search,
  Filter,
  Clock,
  CheckCircle,
  FileText,
  AlertCircle,
  Eye,
  Send,
  ArrowRight
} from 'lucide-react';

export default function OrdersPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [statusFilter, setStatusFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [isWizardOpen, setIsWizardOpen] = useState(location.pathname === '/school/new-order');
  const [actionError, setActionError] = useState(null);

  useEffect(() => {
    if (location.pathname === '/school/new-order') {
      setIsWizardOpen(true);
    }
  }, [location.pathname]);

  const handleCloseWizard = () => {
    setIsWizardOpen(false);
    if (location.pathname === '/school/new-order') {
      navigate('/school/orders', { replace: true });
    }
  };

  // Fetch orders
  const {
    data: ordersRes,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['school-orders', page, size, statusFilter],
    queryFn: () =>
      api.get('/school/orders', {
        params: {
          page,
          size,
          status: statusFilter || undefined,
        },
      }),
  });

  // Submit draft order mutation
  const submitOrderMutation = useMutation({
    mutationFn: (orderId) => api.post(`/school/orders/${orderId}/submit`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['school-orders'] });
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to submit order.');
    },
  });

  const orders = ordersRes?.data?.content || [];
  const totalPages = ordersRes?.data?.totalPages || 0;
  const totalElements = ordersRes?.data?.totalElements || 0;

  // Filtered by local search query if needed
  const displayOrders = orders.filter((o) =>
    searchQuery ? o.orderNumber?.toLowerCase().includes(searchQuery.toLowerCase()) : true
  );

  // Compute stats
  const pendingCount = orders.filter((o) => ['SUBMITTED', 'UNDER_REVIEW'].includes(o.orderStatus)).length;
  const quotationReadyCount = orders.filter((o) => o.orderStatus === 'QUOTATION_GENERATED').length;
  const approvedCount = orders.filter((o) => o.orderStatus === 'APPROVED').length;

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <ShoppingBag className="h-6 w-6 text-indigo-600" />
            ID Card Printing Orders
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Manage batches, track order verification, and approve quotations.
          </p>
        </div>
        <Button size="sm" onClick={() => setIsWizardOpen(true)}>
          <PlusCircle className="h-4 w-4 mr-1.5" />
          New Order
        </Button>
      </div>

      {actionError && (
        <div className="rounded-lg bg-rose-50 border border-rose-200 p-3 flex items-center justify-between text-xs text-rose-700">
          <span>{actionError}</span>
          <button onClick={() => setActionError(null)} className="font-semibold underline ml-2">Dismiss</button>
        </div>
      )}

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
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Under Review</p>
              <h3 className="text-2xl font-bold text-slate-900 mt-1">{pendingCount}</h3>
            </div>
            <div className="h-10 w-10 rounded-full bg-sky-50 flex items-center justify-center text-sky-600">
              <Clock className="h-5 w-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Quotations Ready</p>
              <h3 className="text-2xl font-bold text-amber-600 mt-1">{quotationReadyCount}</h3>
            </div>
            <div className="h-10 w-10 rounded-full bg-amber-50 flex items-center justify-center text-amber-600">
              <FileText className="h-5 w-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Approved</p>
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
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="relative flex-1 w-full">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search by order number (e.g. ORD-2026-)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div className="flex items-center space-x-2 w-full sm:w-auto">
            <Filter className="h-4 w-4 text-slate-400 hidden sm:inline" />
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="text-xs px-3 py-1.5 rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 w-full sm:w-48"
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

      {/* Orders List Table */}
      {isLoading ? (
        <Card className="py-12 flex justify-center">
          <LoadingSpinner size="lg" className="text-indigo-600" />
        </Card>
      ) : isError ? (
        <ErrorState
          title="Could not load orders"
          message={error?.message || 'Failed to fetch school orders.'}
          onRetry={refetch}
        />
      ) : displayOrders.length === 0 ? (
        <Card className="p-12 text-center">
          <EmptyState
            title="No orders found"
            description={
              statusFilter || searchQuery
                ? 'No orders match your filter criteria.'
                : 'You have not created any ID card printing orders yet.'
            }
          />
          {!statusFilter && !searchQuery && (
            <div className="mt-4 flex justify-center">
              <Button size="sm" onClick={() => setIsWizardOpen(true)}>
                <PlusCircle className="h-4 w-4 mr-1.5" />
                Create First Order
              </Button>
            </div>
          )}
        </Card>
      ) : (
        <div className="space-y-3">
          <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
            <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
              <thead className="bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Order Number</th>
                  <th className="px-4 py-3">Template</th>
                  <th className="px-4 py-3">Students</th>
                  <th className="px-4 py-3">Total Qty</th>
                  <th className="px-4 py-3">Created Date</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
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
                      onClick={() => navigate(`/school/orders/${order.id}`)}
                    >
                      <td className="px-4 py-3 font-semibold text-indigo-600 font-mono">
                        {order.orderNumber}
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-900">
                        {order.templateName || 'Standard Template'}
                      </td>
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
                        <div className="flex items-center justify-end space-x-2">
                          {order.orderStatus === 'DRAFT' && (
                            <Button
                              size="xs"
                              variant="outline"
                              onClick={() => submitOrderMutation.mutate(order.id)}
                              disabled={submitOrderMutation.isPending}
                            >
                              <Send className="h-3 w-3 mr-1" />
                              Submit
                            </Button>
                          )}
                          {order.orderStatus === 'QUOTATION_GENERATED' && (
                            <Button
                              size="xs"
                              onClick={() => navigate(`/school/orders/${order.id}`)}
                              className="bg-amber-600 hover:bg-amber-700 text-white"
                            >
                              Review Quotation
                            </Button>
                          )}
                          <Button
                            size="xs"
                            variant="ghost"
                            onClick={() => navigate(`/school/orders/${order.id}`)}
                          >
                            <Eye className="h-3.5 w-3.5 mr-1" />
                            Details
                          </Button>
                        </div>
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

      {/* New Order Wizard Modal */}
      <NewOrderWizardModal
        isOpen={isWizardOpen}
        onClose={handleCloseWizard}
        onSuccess={(created) => {
          navigate(`/school/orders/${created.id}`);
        }}
      />
    </div>
  );
}
