import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { BATCH_STATUSES } from '../../features/production/productionConstants';
import {
  Layers,
  Search,
  Filter,
  Building2,
  Calendar,
  Clock,
  CheckCircle,
  AlertCircle,
  RefreshCw,
  Printer,
  TrendingUp,
} from 'lucide-react';

export default function AdminProductionPage() {
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [statusFilter, setStatusFilter] = useState('');
  const [agencyFilter, setAgencyFilter] = useState('');

  // 1. Fetch agencies for filter dropdown
  const { data: agenciesRes } = useQuery({
    queryKey: ['admin-agencies-list'],
    queryFn: () => api.get('/admin/agencies', { params: { page: 0, size: 200 } }),
  });

  const agencies = agenciesRes?.data?.content || [];

  // 2. Fetch admin production batches
  const {
    data: batchesRes,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['admin-production-batches', page, size, statusFilter, agencyFilter],
    queryFn: () =>
      api.get('/admin/production/batches', {
        params: {
          page,
          size,
          status: statusFilter || undefined,
          agencyId: agencyFilter || undefined,
        },
      }),
  });

  const batches = batchesRes?.data?.data || [];
  const pagination = batchesRes?.data?.pagination || { page: 0, size: 10, totalElements: 0, totalPages: 0 };
  const totalPages = pagination.totalPages || 0;
  const totalElements = pagination.totalElements || 0;

  // Compute summary metrics
  const inProgressCount = batches.filter((b) => b.productionStatus === 'IN_PROGRESS').length;
  const completedCount = batches.filter((b) => b.productionStatus === 'PRINTING_COMPLETED').length;
  const totalCardsPrinted = batches.reduce((acc, b) => acc + (b.actualQuantity || 0), 0);

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
          <Printer className="h-6 w-6 text-indigo-600" />
          Production Batches & Print Runs
        </h1>
        <p className="text-xs text-slate-500 mt-1">
          Monitor active production batches across printing agencies, track yield, and supervise print deadlines.
        </p>
      </div>

      {/* Metrics Strip */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Total Batches</p>
            <p className="text-2xl font-bold text-slate-900 mt-1">{totalElements}</p>
          </div>
          <div className="p-3 bg-indigo-50 text-indigo-600 rounded-lg">
            <Layers className="h-5 w-5" />
          </div>
        </Card>

        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">In Production</p>
            <p className="text-2xl font-bold text-blue-600 mt-1">{inProgressCount}</p>
          </div>
          <div className="p-3 bg-blue-50 text-blue-600 rounded-lg">
            <TrendingUp className="h-5 w-5" />
          </div>
        </Card>

        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Completed Batches</p>
            <p className="text-2xl font-bold text-emerald-600 mt-1">{completedCount}</p>
          </div>
          <div className="p-3 bg-emerald-50 text-emerald-600 rounded-lg">
            <CheckCircle className="h-5 w-5" />
          </div>
        </Card>

        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Cards Printed (Page)</p>
            <p className="text-2xl font-bold text-slate-900 mt-1">{totalCardsPrinted.toLocaleString()}</p>
          </div>
          <div className="p-3 bg-amber-50 text-amber-600 rounded-lg">
            <Printer className="h-5 w-5" />
          </div>
        </Card>
      </div>

      {/* Filter and Control Bar */}
      <Card className="p-4">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto">
            {/* Agency Selector */}
            <div className="flex items-center space-x-2 w-full sm:w-auto">
              <Building2 className="h-4 w-4 text-slate-400 flex-shrink-0" />
              <select
                aria-label="Filter by Agency"
                className="w-full sm:w-48 text-xs border border-slate-300 rounded-md py-1.5 px-2 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
                value={agencyFilter}
                onChange={(e) => {
                  setAgencyFilter(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">All Agencies</option>
                {agencies.map((agency) => (
                  <option key={agency.id} value={agency.id}>
                    {agency.agencyName}
                  </option>
                ))}
              </select>
            </div>

            {/* Status Filter */}
            <div className="flex items-center space-x-2 w-full sm:w-auto">
              <Filter className="h-4 w-4 text-slate-400 flex-shrink-0" />
              <select
                aria-label="Filter by Status"
                className="w-full sm:w-44 text-xs border border-slate-300 rounded-md py-1.5 px-2 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">All Statuses</option>
                {Object.keys(BATCH_STATUSES).map((k) => (
                  <option key={k} value={k}>
                    {BATCH_STATUSES[k].label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex items-center space-x-2 w-full sm:w-auto justify-end">
            <Button variant="ghost" size="sm" onClick={() => refetch()} title="Refresh list">
              <RefreshCw className="h-4 w-4 text-slate-600" />
            </Button>
          </div>
        </div>
      </Card>

      {/* Batches Table View */}
      {isLoading ? (
        <div className="py-20 flex justify-center">
          <LoadingSpinner size="lg" className="text-indigo-600" />
        </div>
      ) : isError ? (
        <ErrorState
          title="Failed to Load Batches"
          message={error?.message || 'Could not retrieve production batches.'}
          onRetry={refetch}
        />
      ) : batches.length === 0 ? (
        <EmptyState
          icon={Layers}
          title="No Batches Found"
          message="No production batches match the selected criteria."
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-semibold">
                <tr>
                  <th className="py-3 px-4">Batch Number</th>
                  <th className="py-3 px-4">Order / Job</th>
                  <th className="py-3 px-4">Agency</th>
                  <th className="py-3 px-4">Progress / Quantity</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Timeline</th>
                  <th className="py-3 px-4">Notes</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {batches.map((batch) => {
                  const statusCfg = BATCH_STATUSES[batch.productionStatus] || {
                    label: batch.productionStatus,
                    variant: 'neutral',
                  };
                  const pct = batch.plannedQuantity > 0
                    ? Math.min(100, Math.round((batch.actualQuantity / batch.plannedQuantity) * 100))
                    : 0;

                  return (
                    <tr key={batch.id} className="hover:bg-slate-50 transition-colors">
                      <td className="py-3 px-4">
                        <span className="font-mono font-bold text-slate-900">{batch.batchNumber}</span>
                      </td>
                      <td className="py-3 px-4">
                        <div className="font-medium text-slate-900">{batch.orderNumber}</div>
                        <div className="text-[11px] font-mono text-slate-500">{batch.jobNumber}</div>
                      </td>
                      <td className="py-3 px-4">
                        <span className="font-medium text-slate-800">{batch.agencyName}</span>
                      </td>
                      <td className="py-3 px-4 min-w-[140px]">
                        <div className="flex items-center justify-between text-[11px] mb-1">
                          <span className="font-semibold text-slate-800">
                            {batch.actualQuantity} / {batch.plannedQuantity}
                          </span>
                          <span className="text-slate-500 font-mono">{pct}%</span>
                        </div>
                        <div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
                          <div
                            className={`h-full rounded-full ${
                              batch.productionStatus === 'PRINTING_COMPLETED' ? 'bg-emerald-500' : 'bg-indigo-600'
                            }`}
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <Badge variant={statusCfg.variant} size="xs">
                          {statusCfg.label}
                        </Badge>
                      </td>
                      <td className="py-3 px-4 text-slate-500">
                        <div className="flex items-center gap-1 text-[11px]">
                          <Clock className="h-3.5 w-3.5 text-slate-400" />
                          <span>
                            {batch.startedAt
                              ? new Date(batch.startedAt).toLocaleDateString('en-IN', {
                                  day: '2-digit',
                                  month: 'short',
                                  hour: '2-digit',
                                  minute: '2-digit',
                                })
                              : 'Not started'}
                          </span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-slate-600 max-w-xs truncate">
                        {batch.notes || <span className="text-slate-400 italic">None</span>}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Pagination Footer */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200 bg-slate-50 text-xs">
              <p className="text-slate-500">
                Page <span className="font-semibold text-slate-700">{page + 1}</span> of{' '}
                <span className="font-semibold text-slate-700">{totalPages}</span> ({totalElements} batches)
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
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </Card>
      )}
    </div>
  );
}
