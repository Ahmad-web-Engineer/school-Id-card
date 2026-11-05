import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import { extractArray, extractPagination } from '../../services/responseUtils';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import CreateDispatchModal from '../../features/delivery/CreateDispatchModal';
import { DISPATCH_STATUSES, DISPATCH_TYPES } from '../../features/delivery/deliveryConstants';
import {
  Truck,
  Package,
  Calendar,
  Clock,
  CheckCircle,
  Filter,
  RefreshCw,
  Plus,
  ArrowRight,
  ExternalLink,
} from 'lucide-react';

export default function AgencyDispatchPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [isDispatchModalOpen, setIsDispatchModalOpen] = useState(false);
  const [selectedAssignment, setSelectedAssignment] = useState(null);

  // 1. Fetch agency dispatches
  const {
    data: dispatchesRes,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['agency-dispatches', page, statusFilter],
    queryFn: () =>
      api.get('/agency/dispatches', {
        params: {
          page,
          size: 10,
          status: statusFilter || undefined,
        },
      }),
  });

  const dispatches = extractArray(dispatchesRes);
  const pagination = extractPagination(dispatchesRes, dispatches.length);

  // 2. Fetch assigned jobs to populate Dispatch button selector
  const { data: jobsRes } = useQuery({
    queryKey: ['agency-jobs-for-dispatch'],
    queryFn: async () => {
      const res = await api.get('/agency/jobs?size=50');
      return extractArray(res);
    },
  });

  const activeJobs = (jobsRes || []).filter(
    (j) => ['ASSIGNED', 'AGENCY_ACCEPTED', 'IN_PRODUCTION', 'PRINTING_COMPLETED', 'COMPLETED'].includes(j.assignmentStatus)
  );

  // 3. Create Dispatch Mutation
  const dispatchMutation = useMutation({
    mutationFn: async ({ assignmentId, payload }) => {
      const res = await api.post(`/agency/jobs/${assignmentId}/dispatch`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['agency-dispatches']);
      queryClient.invalidateQueries(['agency-jobs-for-dispatch']);
      setIsDispatchModalOpen(false);
      setSelectedAssignment(null);
    },
  });

  const inTransitCount = dispatches.filter((d) => d.dispatchStatus === 'IN_TRANSIT').length;
  const deliveredCount = dispatches.filter(
    (d) => d.dispatchStatus === 'DELIVERED_TO_SCHOOL' || d.dispatchStatus === 'DELIVERED_TO_HUB'
  ).length;
  const totalDispatchedCards = dispatches.reduce((acc, d) => acc + (d.dispatchQuantity || 0), 0);

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Truck className="h-6 w-6 text-indigo-600" />
            Consignment Dispatch & Courier Tracking
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Dispatch completed card batches, register courier tracking numbers, and view transit logs.
          </p>
        </div>

        {activeJobs.length > 0 && (
          <div className="flex items-center gap-2">
            <select
              aria-label="Select Job to Dispatch"
              className="text-xs border border-slate-300 rounded-md py-1.5 px-2 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500 max-w-[200px]"
              onChange={(e) => {
                const job = activeJobs.find((j) => j.id === e.target.value);
                if (job) {
                  setSelectedAssignment(job);
                  setIsDispatchModalOpen(true);
                }
              }}
              value=""
            >
              <option value="" disabled>
                Select Finished Job to Dispatch...
              </option>
              {activeJobs.map((j) => (
                <option key={j.id} value={j.id}>
                  {j.jobNumber || j.assignmentNumber} ({j.totalQuantity || j.assignedQuantity || 0} cards) [{j.assignmentStatus}]
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Metrics Strip */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Total Consignments</p>
            <p className="text-2xl font-bold text-slate-900 mt-1">{pagination.totalElements}</p>
          </div>
          <div className="p-3 bg-indigo-50 text-indigo-600 rounded-lg">
            <Package className="h-5 w-5" />
          </div>
        </Card>

        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">In Transit</p>
            <p className="text-2xl font-bold text-blue-600 mt-1">{inTransitCount}</p>
          </div>
          <div className="p-3 bg-blue-50 text-blue-600 rounded-lg">
            <Truck className="h-5 w-5" />
          </div>
        </Card>

        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Total Dispatched (Page)</p>
            <p className="text-2xl font-bold text-emerald-600 mt-1">{totalDispatchedCards.toLocaleString()} cards</p>
          </div>
          <div className="p-3 bg-emerald-50 text-emerald-600 rounded-lg">
            <CheckCircle className="h-5 w-5" />
          </div>
        </Card>
      </div>

      {/* Filter and Control Bar */}
      <Card className="p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Filter className="h-4 w-4 text-slate-400" />
            <select
              aria-label="Filter dispatch status"
              className="text-xs border border-slate-300 rounded-md py-1.5 px-2 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
            >
              <option value="">All Dispatch Statuses</option>
              {Object.keys(DISPATCH_STATUSES).map((k) => (
                <option key={k} value={k}>
                  {DISPATCH_STATUSES[k].label}
                </option>
              ))}
            </select>
          </div>

          <Button variant="ghost" size="sm" onClick={() => refetch()}>
            <RefreshCw className="h-4 w-4 text-slate-600" />
          </Button>
        </div>
      </Card>

      {/* Dispatches Table View */}
      {isLoading ? (
        <div className="py-20 flex justify-center">
          <LoadingSpinner size="lg" className="text-indigo-600" />
        </div>
      ) : isError ? (
        <ErrorState
          title="Failed to Load Dispatches"
          message={error?.message || 'Could not retrieve dispatch history.'}
          onRetry={refetch}
        />
      ) : dispatches.length === 0 ? (
        <EmptyState
          icon={Truck}
          title="No Dispatches Found"
          message="No consignments match your selected status or filter."
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-semibold">
                <tr>
                  <th className="py-3 px-4">Dispatch Number</th>
                  <th className="py-3 px-4">Job Number</th>
                  <th className="py-3 px-4">Courier & Tracking</th>
                  <th className="py-3 px-4">Quantity</th>
                  <th className="py-3 px-4">Model / Route</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Dispatch Date</th>
                  <th className="py-3 px-4">Notes</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {dispatches.map((disp) => {
                  const statusCfg = DISPATCH_STATUSES[disp.dispatchStatus] || {
                    label: disp.dispatchStatus,
                    variant: 'neutral',
                  };
                  const typeCfg = DISPATCH_TYPES[disp.dispatchType] || {
                    label: disp.dispatchType,
                    variant: 'neutral',
                  };

                  return (
                    <tr key={disp.id} className="hover:bg-slate-50 transition-colors">
                      <td className="py-3 px-4 font-mono font-bold text-slate-900">
                        {disp.dispatchNumber}
                      </td>
                      <td className="py-3 px-4 font-mono text-slate-600">
                        {disp.jobNumber}
                      </td>
                      <td className="py-3 px-4">
                        <div className="font-semibold text-slate-900">{disp.courierName}</div>
                        <div className="text-[11px] font-mono text-indigo-600">{disp.trackingNumber}</div>
                      </td>
                      <td className="py-3 px-4 font-semibold text-slate-900">
                        {disp.dispatchQuantity} cards
                      </td>
                      <td className="py-3 px-4">
                        <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-700">
                          {typeCfg.label}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <Badge variant={statusCfg.variant} size="xs">
                          {statusCfg.label}
                        </Badge>
                      </td>
                      <td className="py-3 px-4 text-slate-500 text-[11px]">
                        {new Date(disp.dispatchDate).toLocaleDateString('en-IN', {
                          day: '2-digit',
                          month: 'short',
                          year: 'numeric',
                        })}
                      </td>
                      <td className="py-3 px-4 text-slate-600 max-w-xs truncate">
                        {disp.dispatchNotes || <span className="text-slate-400 italic">None</span>}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {pagination.totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200 bg-slate-50 text-xs">
              <p className="text-slate-500">
                Page <span className="font-semibold text-slate-700">{page + 1}</span> of{' '}
                <span className="font-semibold text-slate-700">{pagination.totalPages}</span>
              </p>
              <div className="flex space-x-2">
                <Button
                  variant="outline"
                  size="xs"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="xs"
                  disabled={page >= pagination.totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </Card>
      )}

      {/* Dispatch Modal */}
      {selectedAssignment && (
        <CreateDispatchModal
          isOpen={isDispatchModalOpen}
          onClose={() => {
            setIsDispatchModalOpen(false);
            setSelectedAssignment(null);
          }}
          assignment={selectedAssignment}
          onSubmit={(payload) => dispatchMutation.mutate({ assignmentId: selectedAssignment.id, payload })}
          isSubmitting={dispatchMutation.isPending}
        />
      )}
    </div>
  );
}
