import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ReassignJobModal from '../../features/assignments/ReassignJobModal';
import { ASSIGNMENT_STATUSES } from '../../features/assignments/assignmentsConstants';
import {
  Briefcase,
  Search,
  Filter,
  Building2,
  Calendar,
  Clock,
  CheckCircle,
  AlertTriangle,
  RefreshCw,
  ShoppingBag,
  ArrowRight
} from 'lucide-react';

export default function AdminAssignmentsPage() {
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [statusFilter, setStatusFilter] = useState('');
  const [agencyFilter, setAgencyFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Selected assignment for reassignment modal
  const [selectedForReassign, setSelectedForReassign] = useState(null);

  // 1. Fetch agencies for filter dropdown
  const { data: agenciesRes } = useQuery({
    queryKey: ['admin-agencies-list'],
    queryFn: () => api.get('/admin/agencies', { params: { page: 0, size: 200 } }),
  });

  const agencies = agenciesRes?.data?.content || [];

  // 2. Fetch assignments
  const {
    data: assignmentsRes,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['admin-assignments', page, size, statusFilter, agencyFilter, searchQuery],
    queryFn: () =>
      api.get('/admin/assignments', {
        params: {
          page,
          size,
          status: statusFilter || undefined,
          agencyId: agencyFilter || undefined,
          search: searchQuery || undefined,
        },
      }),
  });

  const assignments = assignmentsRes?.data?.content || [];
  const totalPages = assignmentsRes?.data?.totalPages || 0;
  const totalElements = assignmentsRes?.data?.totalElements || 0;

  // Compute metrics
  const pendingCount = assignments.filter((a) => a.assignmentStatus === 'ASSIGNED').length;
  const acceptedCount = assignments.filter(
    (a) => a.assignmentStatus === 'AGENCY_ACCEPTED' || a.assignmentStatus === 'IN_PRODUCTION'
  ).length;
  const rejectedCount = assignments.filter((a) => a.assignmentStatus === 'REJECTED').length;

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
          <Briefcase className="h-6 w-6 text-indigo-600" />
          Agency Job Assignments
        </h1>
        <p className="text-xs text-slate-500 mt-1">
          Monitor printing agency workloads, track acceptance deadlines, and manage job reallocations.
        </p>
      </div>

      {/* Metrics Banner */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Total Assignments</p>
              <h3 className="text-2xl font-bold text-slate-900 mt-1">{totalElements}</h3>
            </div>
            <div className="h-10 w-10 rounded-full bg-slate-100 flex items-center justify-center text-slate-600">
              <Briefcase className="h-5 w-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Pending Acceptance</p>
              <h3 className="text-2xl font-bold text-amber-600 mt-1">{pendingCount}</h3>
            </div>
            <div className="h-10 w-10 rounded-full bg-amber-50 flex items-center justify-center text-amber-600">
              <Clock className="h-5 w-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Accepted / Active</p>
              <h3 className="text-2xl font-bold text-sky-600 mt-1">{acceptedCount}</h3>
            </div>
            <div className="h-10 w-10 rounded-full bg-sky-50 flex items-center justify-center text-sky-600">
              <CheckCircle className="h-5 w-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Rejected / Reassign</p>
              <h3 className="text-2xl font-bold text-rose-600 mt-1">{rejectedCount}</h3>
            </div>
            <div className="h-10 w-10 rounded-full bg-rose-50 flex items-center justify-center text-rose-600">
              <AlertTriangle className="h-5 w-5" />
            </div>
          </div>
        </Card>
      </div>

      {/* Filter Bar */}
      <Card className="p-4">
        <div className="flex flex-col lg:flex-row items-center justify-between gap-3">
          <div className="relative flex-1 w-full">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search by job number, agency name, or order..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="flex flex-col sm:flex-row items-center gap-2 w-full lg:w-auto">
            {/* Agency filter */}
            <select
              value={agencyFilter}
              onChange={(e) => {
                setAgencyFilter(e.target.value);
                setPage(0);
              }}
              className="text-xs px-3 py-1.5 rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 w-full sm:w-48"
            >
              <option value="">All Agencies</option>
              {agencies.map((agency) => (
                <option key={agency.id} value={agency.id}>
                  {agency.name}
                </option>
              ))}
            </select>

            {/* Status filter */}
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="text-xs px-3 py-1.5 rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 w-full sm:w-44"
            >
              <option value="">All Statuses</option>
              <option value="ASSIGNED">Assigned (Pending)</option>
              <option value="AGENCY_ACCEPTED">Agency Accepted</option>
              <option value="REJECTED">Rejected</option>
              <option value="REASSIGNED">Reassigned</option>
              <option value="IN_PRODUCTION">In Production</option>
              <option value="COMPLETED">Completed</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Assignments Table */}
      {isLoading ? (
        <Card className="py-12 flex justify-center">
          <LoadingSpinner size="lg" className="text-indigo-600" />
        </Card>
      ) : isError ? (
        <ErrorState
          title="Could not load assignments"
          message={error?.message || 'Failed to fetch agency assignments.'}
          onRetry={refetch}
        />
      ) : assignments.length === 0 ? (
        <Card className="p-12 text-center">
          <EmptyState
            title="No assignments found"
            description="No agency jobs match the selected filter criteria."
          />
        </Card>
      ) : (
        <div className="space-y-3">
          <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
            <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
              <thead className="bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Job Number</th>
                  <th className="px-4 py-3">Order Number</th>
                  <th className="px-4 py-3">Agency</th>
                  <th className="px-4 py-3">Allocated Qty</th>
                  <th className="px-4 py-3">Purchase Rate</th>
                  <th className="px-4 py-3">Expected Delivery</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-800">
                {assignments.map((assignment) => {
                  const statusConfig = ASSIGNMENT_STATUSES[assignment.assignmentStatus] || {
                    label: assignment.assignmentStatus,
                    variant: 'neutral',
                  };
                  return (
                    <tr key={assignment.id} className="hover:bg-slate-50 transition-colors">
                      <td className="px-4 py-3 font-semibold font-mono text-indigo-600">
                        {assignment.assignmentNumber}
                      </td>
                      <td className="px-4 py-3 font-mono text-slate-700">
                        {assignment.orderNumber}
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-900">
                        {assignment.agencyName}
                      </td>
                      <td className="px-4 py-3 font-semibold">
                        {assignment.totalAssignedQuantity} items
                      </td>
                      <td className="px-4 py-3 font-mono text-slate-700">
                        ₹{Number(assignment.agreedRate || 0).toFixed(2)}/card
                        <span className="text-[10px] text-slate-400 block font-normal">
                          Total: ₹{Number(assignment.totalAgreedAmount || 0).toFixed(2)}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-slate-600">
                        {assignment.expectedDeliveryDate ? (
                          new Date(assignment.expectedDeliveryDate).toLocaleDateString('en-IN', {
                            day: '2-digit',
                            month: 'short',
                            year: 'numeric',
                          })
                        ) : (
                          <span className="text-slate-400">N/A</span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <Badge variant={statusConfig.variant} size="sm">
                          {statusConfig.label}
                        </Badge>
                        {assignment.assignmentStatus === 'REJECTED' && assignment.rejectionReason && (
                          <span className="text-[10px] text-rose-600 block mt-0.5 truncate max-w-[140px]" title={assignment.rejectionReason}>
                            Reason: {assignment.rejectionReason}
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-right">
                        {(assignment.assignmentStatus === 'REJECTED' ||
                          assignment.assignmentStatus === 'ASSIGNED') && (
                          <Button
                            size="xs"
                            variant="outline"
                            onClick={() => setSelectedForReassign(assignment)}
                            className="text-purple-700 border-purple-200 hover:bg-purple-50"
                          >
                            <RefreshCw className="h-3 w-3 mr-1" />
                            Reassign
                          </Button>
                        )}
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
                Showing {page * size + 1} to {Math.min((page + 1) * size, totalElements)} of {totalElements} assignments
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

      {/* Reassign Job Modal */}
      {selectedForReassign && (
        <ReassignJobModal
          isOpen={!!selectedForReassign}
          onClose={() => setSelectedForReassign(null)}
          assignment={selectedForReassign}
          onSuccess={() => {
            queryClient.invalidateQueries({ queryKey: ['admin-assignments'] });
          }}
        />
      )}
    </div>
  );
}
