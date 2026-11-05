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
import ReceiveHubModal from '../../features/delivery/ReceiveHubModal';
import {
  DISPATCH_STATUSES,
  DELIVERY_STATUSES,
  DISPATCH_TYPES,
} from '../../features/delivery/deliveryConstants';
import {
  Truck,
  Package,
  Building2,
  Calendar,
  Clock,
  CheckCircle,
  Filter,
  RefreshCw,
  Archive,
  ArrowRight,
  ShieldCheck,
  Check,
} from 'lucide-react';

export default function AdminDeliveryPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState('DISPATCHES'); // 'DISPATCHES' or 'DELIVERIES'

  // Pagination & Filters for Dispatches
  const [dispatchPage, setDispatchPage] = useState(0);
  const [dispatchStatusFilter, setDispatchStatusFilter] = useState('');
  const [selectedForHubReceive, setSelectedForHubReceive] = useState(null);

  // Pagination & Filters for Deliveries
  const [deliveryPage, setDeliveryPage] = useState(0);
  const [deliveryStatusFilter, setDeliveryStatusFilter] = useState('');

  // 1. Fetch Global Dispatches
  const {
    data: dispatchesRes,
    isLoading: dispatchesLoading,
    isError: dispatchesError,
    error: dispatchesErr,
    refetch: refetchDispatches,
  } = useQuery({
    queryKey: ['admin-dispatches', dispatchPage, dispatchStatusFilter],
    queryFn: () =>
      api.get('/admin/delivery/dispatches', {
        params: {
          page: dispatchPage,
          size: 10,
          status: dispatchStatusFilter || undefined,
        },
      }),
  });

  const dispatches = extractArray(dispatchesRes);
  const dispatchPagination = extractPagination(dispatchesRes, dispatches.length);

  // 2. Fetch Global Deliveries
  const {
    data: deliveriesRes,
    isLoading: deliveriesLoading,
    isError: deliveriesError,
    error: deliveriesErr,
    refetch: refetchDeliveries,
  } = useQuery({
    queryKey: ['admin-deliveries', deliveryPage, deliveryStatusFilter],
    queryFn: () =>
      api.get('/admin/delivery/records', {
        params: {
          page: deliveryPage,
          size: 10,
          status: deliveryStatusFilter || undefined,
        },
      }),
  });

  const deliveries = extractArray(deliveriesRes);
  const deliveryPagination = extractPagination(deliveriesRes, deliveries.length);

  // 3. Receive at Hub Mutation (Model B)
  const receiveHubMutation = useMutation({
    mutationFn: async ({ dispatchId, payload }) => {
      const res = await api.post(`/admin/delivery/dispatches/${dispatchId}/receive-hub`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['admin-dispatches']);
      setSelectedForHubReceive(null);
    },
  });

  // 4. Close Delivery Mutation
  const closeDeliveryMutation = useMutation({
    mutationFn: async (deliveryId) => {
      const res = await api.post(`/admin/delivery/records/${deliveryId}/close`, {
        closureNotes: 'Admin verified and closed delivery cycle',
      });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['admin-deliveries']);
    },
  });

  const inTransitCount = dispatches.filter((d) => d.dispatchStatus === 'IN_TRANSIT').length;
  const hubReceivedCount = dispatches.filter((d) => d.dispatchStatus === 'DELIVERED_TO_HUB').length;
  const confirmedDeliveriesCount = deliveries.filter((d) => d.deliveryStatus === 'CONFIRMED').length;

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
          <Truck className="h-6 w-6 text-indigo-600" />
          Logistics, Dispatches & Delivery Oversight
        </h1>
        <p className="text-xs text-slate-500 mt-1">
          Supervise consignment shipping, track Model A direct deliveries and Model B sorting hub arrivals, and close verified delivery receipts.
        </p>
      </div>

      {/* Metrics Strip */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Total Dispatches</p>
            <p className="text-2xl font-bold text-slate-900 mt-1">{dispatchPagination.totalElements}</p>
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
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Hub Received (Model B)</p>
            <p className="text-2xl font-bold text-amber-600 mt-1">{hubReceivedCount}</p>
          </div>
          <div className="p-3 bg-amber-50 text-amber-600 rounded-lg">
            <Archive className="h-5 w-5" />
          </div>
        </Card>

        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Confirmed Receipts</p>
            <p className="text-2xl font-bold text-emerald-600 mt-1">{confirmedDeliveriesCount}</p>
          </div>
          <div className="p-3 bg-emerald-50 text-emerald-600 rounded-lg">
            <CheckCircle className="h-5 w-5" />
          </div>
        </Card>
      </div>

      {/* Tab Bar */}
      <div className="flex border-b border-slate-200">
        <button
          type="button"
          onClick={() => setActiveTab('DISPATCHES')}
          className={`py-2 px-4 text-xs font-semibold border-b-2 flex items-center gap-2 transition-colors ${
            activeTab === 'DISPATCHES'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          }`}
        >
          <Truck className="h-4 w-4" />
          Consignment Dispatches & Transit
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('DELIVERIES')}
          className={`py-2 px-4 text-xs font-semibold border-b-2 flex items-center gap-2 transition-colors ${
            activeTab === 'DELIVERIES'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          }`}
        >
          <CheckCircle className="h-4 w-4" />
          School Delivery Receipts
          {confirmedDeliveriesCount > 0 && (
            <span className="bg-emerald-100 text-emerald-800 text-[10px] font-bold px-1.5 py-0.5 rounded-full">
              {confirmedDeliveriesCount}
            </span>
          )}
        </button>
      </div>

      {/* TAB 1: DISPATCHES */}
      {activeTab === 'DISPATCHES' && (
        <div className="space-y-4">
          <Card className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Filter className="h-4 w-4 text-slate-400" />
                <select
                  aria-label="Filter dispatch status"
                  className="text-xs border border-slate-300 rounded-md py-1.5 px-2 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  value={dispatchStatusFilter}
                  onChange={(e) => {
                    setDispatchStatusFilter(e.target.value);
                    setDispatchPage(0);
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

              <Button variant="ghost" size="sm" onClick={() => refetchDispatches()}>
                <RefreshCw className="h-4 w-4 text-slate-600" />
              </Button>
            </div>
          </Card>

          {dispatchesLoading ? (
            <div className="py-20 flex justify-center">
              <LoadingSpinner size="lg" className="text-indigo-600" />
            </div>
          ) : dispatchesError ? (
            <ErrorState
              title="Failed to Load Dispatches"
              message={dispatchesErr?.message || 'Could not retrieve dispatches.'}
              onRetry={refetchDispatches}
            />
          ) : dispatches.length === 0 ? (
            <EmptyState
              icon={Truck}
              title="No Dispatches Found"
              message="No consignments match the selected status filter."
            />
          ) : (
            <Card className="overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-semibold">
                    <tr>
                      <th className="py-3 px-4">Dispatch #</th>
                      <th className="py-3 px-4">Order / School</th>
                      <th className="py-3 px-4">Agency</th>
                      <th className="py-3 px-4">Courier & AWB</th>
                      <th className="py-3 px-4">Quantity</th>
                      <th className="py-3 px-4">Route Type</th>
                      <th className="py-3 px-4">Status</th>
                      <th className="py-3 px-4 text-right">Action</th>
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
                          <td className="py-3 px-4">
                            <div className="font-medium text-slate-900">{disp.orderNumber}</div>
                            <div className="text-[11px] text-slate-500">{disp.schoolName}</div>
                          </td>
                          <td className="py-3 px-4 font-medium text-slate-800">
                            {disp.agencyName || <span className="text-slate-400 italic">Central Hub</span>}
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
                          <td className="py-3 px-4 text-right">
                            {disp.dispatchType === 'AGENCY_TO_HUB' && disp.dispatchStatus === 'IN_TRANSIT' ? (
                              <Button
                                size="xs"
                                className="bg-amber-600 hover:bg-amber-700 text-white"
                                onClick={() => setSelectedForHubReceive(disp)}
                              >
                                Receive at Hub
                              </Button>
                            ) : (
                              <span className="text-slate-400 text-xs">Logged</span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {dispatchPagination.totalPages > 1 && (
                <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200 bg-slate-50 text-xs">
                  <p className="text-slate-500">
                    Page <span className="font-semibold text-slate-700">{dispatchPage + 1}</span> of{' '}
                    <span className="font-semibold text-slate-700">{dispatchPagination.totalPages}</span>
                  </p>
                  <div className="flex space-x-2">
                    <Button
                      variant="outline"
                      size="xs"
                      disabled={dispatchPage === 0}
                      onClick={() => setDispatchPage((p) => Math.max(0, p - 1))}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="xs"
                      disabled={dispatchPage >= dispatchPagination.totalPages - 1}
                      onClick={() => setDispatchPage((p) => p + 1)}
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </Card>
          )}
        </div>
      )}

      {/* TAB 2: DELIVERIES */}
      {activeTab === 'DELIVERIES' && (
        <div className="space-y-4">
          <Card className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Filter className="h-4 w-4 text-slate-400" />
                <select
                  aria-label="Filter delivery status"
                  className="text-xs border border-slate-300 rounded-md py-1.5 px-2 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  value={deliveryStatusFilter}
                  onChange={(e) => {
                    setDeliveryStatusFilter(e.target.value);
                    setDeliveryPage(0);
                  }}
                >
                  <option value="">All Delivery Statuses</option>
                  {Object.keys(DELIVERY_STATUSES).map((k) => (
                    <option key={k} value={k}>
                      {DELIVERY_STATUSES[k].label}
                    </option>
                  ))}
                </select>
              </div>

              <Button variant="ghost" size="sm" onClick={() => refetchDeliveries()}>
                <RefreshCw className="h-4 w-4 text-slate-600" />
              </Button>
            </div>
          </Card>

          {deliveriesLoading ? (
            <div className="py-20 flex justify-center">
              <LoadingSpinner size="lg" className="text-indigo-600" />
            </div>
          ) : deliveriesError ? (
            <ErrorState
              title="Failed to Load Delivery Records"
              message={deliveriesErr?.message || 'Could not retrieve deliveries.'}
              onRetry={refetchDeliveries}
            />
          ) : deliveries.length === 0 ? (
            <EmptyState
              icon={CheckCircle}
              title="No Delivery Receipts"
              message="No school delivery confirmations found."
            />
          ) : (
            <Card className="overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-semibold">
                    <tr>
                      <th className="py-3 px-4">Receipt #</th>
                      <th className="py-3 px-4">Order / School</th>
                      <th className="py-3 px-4">Delivered Cards</th>
                      <th className="py-3 px-4">Received By</th>
                      <th className="py-3 px-4">Delivered At</th>
                      <th className="py-3 px-4">Status</th>
                      <th className="py-3 px-4 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {deliveries.map((del) => {
                      const statusCfg = DELIVERY_STATUSES[del.deliveryStatus] || {
                        label: del.deliveryStatus,
                        variant: 'neutral',
                      };

                      return (
                        <tr key={del.id} className="hover:bg-slate-50 transition-colors">
                          <td className="py-3 px-4 font-mono font-bold text-slate-900">
                            {del.deliveryNumber}
                          </td>
                          <td className="py-3 px-4">
                            <div className="font-medium text-slate-900">{del.orderNumber}</div>
                            <div className="text-[11px] text-slate-500">{del.schoolName}</div>
                          </td>
                          <td className="py-3 px-4 font-semibold text-emerald-700">
                            {del.deliveryQuantity} cards
                          </td>
                          <td className="py-3 px-4">
                            <div className="font-semibold text-slate-900">{del.receivedBy}</div>
                            <div className="text-[11px] text-slate-500 font-mono">{del.receivedMobile || '-'}</div>
                          </td>
                          <td className="py-3 px-4 text-slate-500 text-[11px]">
                            {new Date(del.deliveredAt).toLocaleDateString('en-IN', {
                              day: '2-digit',
                              month: 'short',
                              year: 'numeric',
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </td>
                          <td className="py-3 px-4">
                            <Badge variant={statusCfg.variant} size="xs">
                              {statusCfg.label}
                            </Badge>
                          </td>
                          <td className="py-3 px-4 text-right">
                            {del.deliveryStatus === 'CONFIRMED' ? (
                              <Button
                                size="xs"
                                variant="outline"
                                onClick={() => closeDeliveryMutation.mutate(del.id)}
                                disabled={closeDeliveryMutation.isPending}
                              >
                                Close Delivery
                              </Button>
                            ) : (
                              <span className="text-slate-400 text-xs">Closed</span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {deliveryPagination.totalPages > 1 && (
                <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200 bg-slate-50 text-xs">
                  <p className="text-slate-500">
                    Page <span className="font-semibold text-slate-700">{deliveryPage + 1}</span> of{' '}
                    <span className="font-semibold text-slate-700">{deliveryPagination.totalPages}</span>
                  </p>
                  <div className="flex space-x-2">
                    <Button
                      variant="outline"
                      size="xs"
                      disabled={deliveryPage === 0}
                      onClick={() => setDeliveryPage((p) => Math.max(0, p - 1))}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="xs"
                      disabled={deliveryPage >= deliveryPagination.totalPages - 1}
                      onClick={() => setDeliveryPage((p) => p + 1)}
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </Card>
          )}
        </div>
      )}

      {/* Model B: Receive at Hub Modal */}
      {selectedForHubReceive && (
        <ReceiveHubModal
          isOpen={!!selectedForHubReceive}
          onClose={() => setSelectedForHubReceive(null)}
          dispatchRecord={selectedForHubReceive}
          onSubmit={(payload) =>
            receiveHubMutation.mutate({ dispatchId: selectedForHubReceive.id, payload })
          }
          isSubmitting={receiveHubMutation.isPending}
        />
      )}
    </div>
  );
}
