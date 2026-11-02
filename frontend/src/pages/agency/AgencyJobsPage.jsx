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
import { ASSIGNMENT_STATUSES } from '../../features/assignments/assignmentsConstants';
import {
  Briefcase,
  Search,
  Filter,
  Clock,
  CheckCircle,
  AlertTriangle,
  Calendar,
  Eye,
  Printer,
  ArrowRight
} from 'lucide-react';

export default function AgencyJobsPage() {
  const navigate = useNavigate();

  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [statusFilter, setStatusFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Fetch agency's own jobs
  const {
    data: jobsRes,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['agency-jobs', page, size, statusFilter, searchQuery],
    queryFn: () =>
      api.get('/agency/jobs', {
        params: {
          page,
          size,
          status: statusFilter || undefined,
          search: searchQuery || undefined,
        },
      }),
  });

  const jobs = jobsRes?.data?.content || [];
  const totalPages = jobsRes?.data?.totalPages || 0;
  const totalElements = jobsRes?.data?.totalElements || 0;

  // Compute metrics
  const pendingCount = jobs.filter((j) => j.assignmentStatus === 'ASSIGNED').length;
  const acceptedCount = jobs.filter((j) => j.assignmentStatus === 'AGENCY_ACCEPTED').length;
  const inProductionCount = jobs.filter((j) => j.assignmentStatus === 'IN_PRODUCTION').length;

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
          <Briefcase className="h-6 w-6 text-indigo-600" />
          Assigned Printing Jobs
        </h1>
        <p className="text-xs text-slate-500 mt-1">
          Review incoming card print orders, confirm production schedules, and track active print runs.
        </p>
      </div>

      {/* Metrics Banner */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Total Assigned</p>
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
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Awaiting Response</p>
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
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">Accepted</p>
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
              <p className="text-[11px] font-medium uppercase tracking-wider text-slate-500">In Production</p>
              <h3 className="text-2xl font-bold text-indigo-600 mt-1">{inProductionCount}</h3>
            </div>
            <div className="h-10 w-10 rounded-full bg-indigo-50 flex items-center justify-center text-indigo-600">
              <Printer className="h-5 w-5" />
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
              placeholder="Search by job number (e.g. JOB-2026-)..."
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
              <option value="ASSIGNED">Awaiting Acceptance</option>
              <option value="AGENCY_ACCEPTED">Accepted</option>
              <option value="IN_PRODUCTION">In Production</option>
              <option value="COMPLETED">Completed</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Jobs List Table */}
      {isLoading ? (
        <Card className="py-12 flex justify-center">
          <LoadingSpinner size="lg" className="text-indigo-600" />
        </Card>
      ) : isError ? (
        <ErrorState
          title="Could not load jobs"
          message={error?.message || 'Failed to fetch assigned jobs.'}
          onRetry={refetch}
        />
      ) : jobs.length === 0 ? (
        <Card className="p-12 text-center">
          <EmptyState
            title="No print jobs assigned"
            description="You currently have no jobs matching your filter criteria."
          />
        </Card>
      ) : (
        <div className="space-y-3">
          <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
            <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
              <thead className="bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Job Number</th>
                  <th className="px-4 py-3">Academic Session</th>
                  <th className="px-4 py-3">Allocated Cards</th>
                  <th className="px-4 py-3">Assigned Date</th>
                  <th className="px-4 py-3">Expected Delivery</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-800">
                {jobs.map((job) => {
                  const statusConfig = ASSIGNMENT_STATUSES[job.assignmentStatus] || {
                    label: job.assignmentStatus,
                    variant: 'neutral',
                  };
                  return (
                    <tr
                      key={job.id}
                      className="hover:bg-slate-50 transition-colors cursor-pointer"
                      onClick={() => navigate(`/agency/jobs/${job.id}`)}
                    >
                      <td className="px-4 py-3 font-semibold font-mono text-indigo-600">
                        {job.jobNumber}
                      </td>
                      <td className="px-4 py-3 text-slate-700">
                        {job.academicSession}
                      </td>
                      <td className="px-4 py-3 font-semibold text-slate-900">
                        {job.totalQuantity} items
                      </td>
                      <td className="px-4 py-3 text-slate-500">
                        {new Date(job.assignedAt).toLocaleDateString('en-IN', {
                          day: '2-digit',
                          month: 'short',
                          year: 'numeric',
                        })}
                      </td>
                      <td className="px-4 py-3 text-slate-700">
                        {job.expectedDeliveryDate ? (
                          new Date(job.expectedDeliveryDate).toLocaleDateString('en-IN', {
                            day: '2-digit',
                            month: 'short',
                            year: 'numeric',
                          })
                        ) : (
                          <span className="text-slate-400">Not specified</span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <Badge variant={statusConfig.variant} size="sm">
                          {statusConfig.label}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-right" onClick={(e) => e.stopPropagation()}>
                        <div className="flex items-center justify-end space-x-2">
                          {job.assignmentStatus === 'ASSIGNED' ? (
                            <Button
                              size="xs"
                              className="bg-indigo-600 hover:bg-indigo-700 text-white"
                              onClick={() => navigate(`/agency/jobs/${job.id}`)}
                            >
                              Review & Accept
                            </Button>
                          ) : (
                            <Button
                              size="xs"
                              variant="ghost"
                              onClick={() => navigate(`/agency/jobs/${job.id}`)}
                            >
                              <Eye className="h-3.5 w-3.5 mr-1" />
                              View
                            </Button>
                          )}
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
                Showing {page * size + 1} to {Math.min((page + 1) * size, totalElements)} of {totalElements} jobs
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
