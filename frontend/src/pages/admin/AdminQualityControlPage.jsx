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
import ReviewReprintModal from '../../features/production/ReviewReprintModal';
import SubmitQualityCheckModal from '../../features/production/SubmitQualityCheckModal';
import {
  QC_STATUSES,
  REPRINT_STATUSES,
  DEFECT_CATEGORIES,
  COST_BEARERS,
} from '../../features/production/productionConstants';
import {
  CheckCircle,
  AlertTriangle,
  ClipboardCheck,
  RefreshCw,
  Clock,
  RotateCcw,
  Building2,
  Filter,
  ShieldCheck,
  Check,
  X,
} from 'lucide-react';

export default function AdminQualityControlPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState('REPRINTS'); // 'REPRINTS' or 'INSPECTIONS'

  // Pagination & Filters for Reprints
  const [reprintPage, setReprintPage] = useState(0);
  const [reprintStatusFilter, setReprintStatusFilter] = useState('');
  const [selectedReprint, setSelectedReprint] = useState(null);

  // Pagination & Filters for QC Reports
  const [qcPage, setQcPage] = useState(0);
  const [qcStatusFilter, setQcStatusFilter] = useState('');

  // QC Submission State
  const [isSubmitQcOpen, setIsSubmitQcOpen] = useState(false);
  const [selectedAssignmentId, setSelectedAssignmentId] = useState('');

  // 1. Fetch Reprint Requests
  const {
    data: reprintsRes,
    isLoading: reprintsLoading,
    isError: reprintsError,
    error: reprintsErr,
    refetch: refetchReprints,
  } = useQuery({
    queryKey: ['admin-reprints', reprintPage, reprintStatusFilter],
    queryFn: () =>
      api.get('/admin/production/reprints', {
        params: {
          page: reprintPage,
          size: 10,
          status: reprintStatusFilter || undefined,
        },
      }),
  });

  const reprints = extractArray(reprintsRes);
  const reprintPagination = extractPagination(reprintsRes, reprints.length);

  // 2. Fetch QC Inspections
  const {
    data: qcRes,
    isLoading: qcLoading,
    isError: qcError,
    error: qcErr,
    refetch: refetchQc,
  } = useQuery({
    queryKey: ['admin-qc-inspections', qcPage, qcStatusFilter],
    queryFn: () =>
      api.get('/admin/production/quality-checks', {
        params: {
          page: qcPage,
          size: 10,
          status: qcStatusFilter || undefined,
        },
      }),
  });

  const qcReports = extractArray(qcRes);
  const qcPagination = extractPagination(qcRes, qcReports.length);

  // 3. Fetch Assignments for Quality Checks
  const { data: assignmentsRes } = useQuery({
    queryKey: ['admin-assignments-for-qc'],
    queryFn: () => api.get('/admin/assignments', { params: { page: 0, size: 200 } }),
  });
  const assignments = extractArray(assignmentsRes);

  const effectiveAssignmentId = selectedAssignmentId || assignments[0]?.id;

  // Fetch full details of the chosen assignment (includes student cards and specs)
  const { data: assignmentDetailRes } = useQuery({
    queryKey: ['admin-assignment-detail-qc', effectiveAssignmentId],
    queryFn: async () => {
      if (!effectiveAssignmentId) return null;
      const res = await api.get(`/admin/assignments/${effectiveAssignmentId}`);
      return res.data?.data || null;
    },
    enabled: !!effectiveAssignmentId && isSubmitQcOpen,
  });

  // 4. Submit Admin QC Mutation
  const submitAdminQcMutation = useMutation({
    mutationFn: async (payload) => {
      if (!selectedAssignmentId) {
        throw new Error('Please select an assignment to inspect.');
      }
      const res = await api.post(`/admin/assignments/${selectedAssignmentId}/quality-checks`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['admin-qc-inspections']);
      queryClient.invalidateQueries(['admin-reprints']);
      queryClient.invalidateQueries(['admin-assignments']);
      setIsSubmitQcOpen(false);
      setSelectedAssignmentId('');
    },
  });

  // 5. Approve Reprint Mutation
  const approveMutation = useMutation({
    mutationFn: async ({ id, payload }) => {
      const res = await api.post(`/admin/reprints/${id}/approve`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['admin-reprints']);
      setSelectedReprint(null);
    },
  });

  // 6. Reject Reprint Mutation
  const rejectMutation = useMutation({
    mutationFn: async ({ id, payload }) => {
      const res = await api.post(`/admin/reprints/${id}/reject`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['admin-reprints']);
      setSelectedReprint(null);
    },
  });

  const pendingReprintsCount = reprints.filter((r) => r.approvalStatus === 'PENDING').length;
  const approvedReprintsCount = reprints.filter((r) => r.approvalStatus === 'APPROVED').length;
  const totalInspected = qcReports.reduce((acc, q) => acc + (q.inspectedQuantity || 0), 0);
  const totalDefects = qcReports.reduce((acc, q) => acc + (q.rejectedQuantity || 0), 0);

  const activeSelectedAssignment = assignments.find((a) => a.id === selectedAssignmentId) || assignments[0];

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <ShieldCheck className="h-6 w-6 text-indigo-600" />
            Quality Assurance & Reprint Authorization
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Review batch quality check reports, authorize replacement card printings, and allocate financial liabilities.
          </p>
        </div>
        <Button
          size="sm"
          className="bg-indigo-600 hover:bg-indigo-700 text-white flex items-center gap-1.5 self-start sm:self-auto"
          onClick={() => {
            if (assignments.length > 0 && !selectedAssignmentId) {
              setSelectedAssignmentId(assignments[0].id);
            }
            setIsSubmitQcOpen(true);
          }}
        >
          <ClipboardCheck className="h-4 w-4" />
          Perform Quality Check
        </Button>
      </div>

      {/* Metrics Strip */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Pending Reprints</p>
            <p className="text-2xl font-bold text-amber-600 mt-1">{pendingReprintsCount}</p>
          </div>
          <div className="p-3 bg-amber-50 text-amber-600 rounded-lg">
            <Clock className="h-5 w-5" />
          </div>
        </Card>

        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Approved Reprints</p>
            <p className="text-2xl font-bold text-emerald-600 mt-1">{approvedReprintsCount}</p>
          </div>
          <div className="p-3 bg-emerald-50 text-emerald-600 rounded-lg">
            <Check className="h-5 w-5" />
          </div>
        </Card>

        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Cards Inspected (Page)</p>
            <p className="text-2xl font-bold text-slate-900 mt-1">{totalInspected.toLocaleString()}</p>
          </div>
          <div className="p-3 bg-blue-50 text-blue-600 rounded-lg">
            <ClipboardCheck className="h-5 w-5" />
          </div>
        </Card>

        <Card className="p-4 flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Defect Cards (Page)</p>
            <p className="text-2xl font-bold text-rose-600 mt-1">{totalDefects.toLocaleString()}</p>
          </div>
          <div className="p-3 bg-rose-50 text-rose-600 rounded-lg">
            <AlertTriangle className="h-5 w-5" />
          </div>
        </Card>
      </div>

      {/* Tab Navigation */}
      <div className="flex border-b border-slate-200">
        <button
          type="button"
          onClick={() => setActiveTab('REPRINTS')}
          className={`py-2 px-4 text-xs font-semibold border-b-2 flex items-center gap-2 transition-colors ${
            activeTab === 'REPRINTS'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          }`}
        >
          <RotateCcw className="h-4 w-4" />
          Reprint Requests Queue
          {pendingReprintsCount > 0 && (
            <span className="bg-amber-100 text-amber-800 text-[10px] font-bold px-1.5 py-0.5 rounded-full">
              {pendingReprintsCount}
            </span>
          )}
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('INSPECTIONS')}
          className={`py-2 px-4 text-xs font-semibold border-b-2 flex items-center gap-2 transition-colors ${
            activeTab === 'INSPECTIONS'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          }`}
        >
          <ClipboardCheck className="h-4 w-4" />
          Inspection Audit Trail
        </button>
      </div>

      {/* TAB 1: REPRINT REQUESTS */}
      {activeTab === 'REPRINTS' && (
        <div className="space-y-4">
          {/* Filter Bar */}
          <Card className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Filter className="h-4 w-4 text-slate-400" />
                <select
                  aria-label="Filter reprint status"
                  className="text-xs border border-slate-300 rounded-md py-1.5 px-2 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  value={reprintStatusFilter}
                  onChange={(e) => {
                    setReprintStatusFilter(e.target.value);
                    setReprintPage(0);
                  }}
                >
                  <option value="">All Reprint Statuses</option>
                  {Object.keys(REPRINT_STATUSES).map((k) => (
                    <option key={k} value={k}>
                      {REPRINT_STATUSES[k].label}
                    </option>
                  ))}
                </select>
              </div>

              <Button variant="ghost" size="sm" onClick={() => refetchReprints()}>
                <RefreshCw className="h-4 w-4 text-slate-600" />
              </Button>
            </div>
          </Card>

          {reprintsLoading ? (
            <div className="py-20 flex justify-center">
              <LoadingSpinner size="lg" className="text-indigo-600" />
            </div>
          ) : reprintsError ? (
            <ErrorState
              title="Failed to Load Reprint Requests"
              message={reprintsErr?.message || 'Could not retrieve reprint requests.'}
              onRetry={refetchReprints}
            />
          ) : reprints.length === 0 ? (
            <EmptyState
              icon={RotateCcw}
              title="No Reprint Requests"
              message="No reprint requests match the selected status filter."
            />
          ) : (
            <Card className="overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-semibold">
                    <tr>
                      <th className="py-3 px-4">Request #</th>
                      <th className="py-3 px-4">Job / Assignment</th>
                      <th className="py-3 px-4">Replacement Cards</th>
                      <th className="py-3 px-4">Reason / Defect</th>
                      <th className="py-3 px-4">Cost Bearer</th>
                      <th className="py-3 px-4">Approval Status</th>
                      <th className="py-3 px-4">Decided By</th>
                      <th className="py-3 px-4 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {reprints.map((rep) => {
                      const statusCfg = REPRINT_STATUSES[rep.approvalStatus] || {
                        label: rep.approvalStatus,
                        variant: 'neutral',
                      };
                      const costBearerCfg = COST_BEARERS.find((c) => c.value === rep.costBearer);

                      return (
                        <tr key={rep.id} className="hover:bg-slate-50 transition-colors">
                          <td className="py-3 px-4 font-mono font-bold text-slate-900">
                            {rep.requestNumber}
                          </td>
                          <td className="py-3 px-4 font-mono text-slate-600">
                            {rep.jobNumber}
                          </td>
                          <td className="py-3 px-4">
                            <span className="font-semibold text-rose-600">{rep.replacementQuantity} cards</span>
                          </td>
                          <td className="py-3 px-4 max-w-xs">
                            <p className="font-medium text-slate-800 truncate">{rep.reason}</p>
                            {rep.rejectionReason && (
                              <p className="text-[11px] text-rose-600 mt-0.5 truncate">
                                Rejection: {rep.rejectionReason}
                              </p>
                            )}
                          </td>
                          <td className="py-3 px-4">
                            {rep.costBearer ? (
                              <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold bg-slate-100 text-slate-800">
                                {costBearerCfg?.label || rep.costBearer}
                              </span>
                            ) : (
                              <span className="text-slate-400 italic">Unassigned</span>
                            )}
                          </td>
                          <td className="py-3 px-4">
                            <Badge variant={statusCfg.variant} size="xs">
                              {statusCfg.label}
                            </Badge>
                          </td>
                          <td className="py-3 px-4 text-slate-500 text-[11px]">
                            {rep.approverName || 'Pending Review'}
                          </td>
                          <td className="py-3 px-4 text-right">
                            {rep.approvalStatus === 'PENDING' ? (
                              <Button
                                size="xs"
                                className="bg-indigo-600 hover:bg-indigo-700 text-white"
                                onClick={() => setSelectedReprint(rep)}
                              >
                                Review
                              </Button>
                            ) : (
                              <span className="text-slate-400 text-xs">Resolved</span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {reprintPagination.totalPages > 1 && (
                <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200 bg-slate-50 text-xs">
                  <p className="text-slate-500">
                    Page <span className="font-semibold text-slate-700">{reprintPage + 1}</span> of{' '}
                    <span className="font-semibold text-slate-700">{reprintPagination.totalPages}</span>
                  </p>
                  <div className="flex space-x-2">
                    <Button
                      variant="outline"
                      size="xs"
                      disabled={reprintPage === 0}
                      onClick={() => setReprintPage((p) => Math.max(0, p - 1))}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="xs"
                      disabled={reprintPage >= reprintPagination.totalPages - 1}
                      onClick={() => setReprintPage((p) => p + 1)}
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

      {/* TAB 2: QUALITY INSPECTION AUDIT TRAIL */}
      {activeTab === 'INSPECTIONS' && (
        <div className="space-y-4">
          <Card className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Filter className="h-4 w-4 text-slate-400" />
                <select
                  aria-label="Filter QC status"
                  className="text-xs border border-slate-300 rounded-md py-1.5 px-2 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  value={qcStatusFilter}
                  onChange={(e) => {
                    setQcStatusFilter(e.target.value);
                    setQcPage(0);
                  }}
                >
                  <option value="">All Inspection Statuses</option>
                  {Object.keys(QC_STATUSES).map((k) => (
                    <option key={k} value={k}>
                      {QC_STATUSES[k].label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex items-center space-x-2">
                <Button
                  size="xs"
                  className="bg-indigo-600 hover:bg-indigo-700 text-white flex items-center gap-1"
                  onClick={() => {
                    if (assignments.length > 0 && !selectedAssignmentId) {
                      setSelectedAssignmentId(assignments[0].id);
                    }
                    setIsSubmitQcOpen(true);
                  }}
                >
                  <ClipboardCheck className="h-3.5 w-3.5" />
                  Perform QC
                </Button>
                <Button variant="ghost" size="sm" onClick={() => refetchQc()}>
                  <RefreshCw className="h-4 w-4 text-slate-600" />
                </Button>
              </div>
            </div>
          </Card>

          {qcLoading ? (
            <div className="py-20 flex justify-center">
              <LoadingSpinner size="lg" className="text-indigo-600" />
            </div>
          ) : qcError ? (
            <ErrorState
              title="Failed to Load Quality Inspections"
              message={qcErr?.message || 'Could not retrieve inspection reports.'}
              onRetry={refetchQc}
            />
          ) : qcReports.length === 0 ? (
            <EmptyState
              icon={ClipboardCheck}
              title="No Inspection Reports"
              message="No quality checks found for the selected filter."
            />
          ) : (
            <Card className="overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase tracking-wider font-semibold">
                    <tr>
                      <th className="py-3 px-4">QC Number</th>
                      <th className="py-3 px-4">Order / Job</th>
                      <th className="py-3 px-4">Agency</th>
                      <th className="py-3 px-4">Inspected</th>
                      <th className="py-3 px-4">Accepted / Rejected</th>
                      <th className="py-3 px-4">Defect Classification</th>
                      <th className="py-3 px-4">Status</th>
                      <th className="py-3 px-4">Inspector</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {qcReports.map((qc) => {
                      const statusCfg = QC_STATUSES[qc.inspectionStatus] || {
                        label: qc.inspectionStatus,
                        variant: 'neutral',
                      };
                      const defectCfg = DEFECT_CATEGORIES.find((d) => d.value === qc.defectCategory);

                      return (
                        <tr key={qc.id} className="hover:bg-slate-50 transition-colors">
                          <td className="py-3 px-4 font-mono font-bold text-slate-900">
                            {qc.checkNumber}
                          </td>
                          <td className="py-3 px-4">
                            <div className="font-medium text-slate-900">{qc.orderNumber}</div>
                            <div className="text-[11px] font-mono text-slate-500">{qc.jobNumber}</div>
                          </td>
                          <td className="py-3 px-4 font-medium text-slate-800">
                            {qc.agencyName}
                          </td>
                          <td className="py-3 px-4 font-semibold text-slate-900">
                            {qc.inspectedQuantity} cards
                          </td>
                          <td className="py-3 px-4">
                            <span className="text-emerald-700 font-semibold">{qc.acceptedQuantity} pass</span>
                            {qc.rejectedQuantity > 0 && (
                              <span className="text-rose-600 font-semibold ml-2">
                                / {qc.rejectedQuantity} fail
                              </span>
                            )}
                          </td>
                          <td className="py-3 px-4">
                            {qc.defectCategory && qc.defectCategory !== 'NONE' ? (
                              <div>
                                <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium bg-rose-50 text-rose-700 border border-rose-200">
                                  {defectCfg?.label || qc.defectCategory}
                                </span>
                                <p className="text-[11px] text-slate-600 mt-0.5 truncate max-w-xs">
                                  {qc.defectDescription}
                                </p>
                              </div>
                            ) : (
                              <span className="text-emerald-600 text-[11px]">No Defects</span>
                            )}
                          </td>
                          <td className="py-3 px-4">
                            <Badge variant={statusCfg.variant} size="xs">
                              {statusCfg.label}
                            </Badge>
                          </td>
                          <td className="py-3 px-4 text-slate-500 text-[11px]">
                            <div>{qc.inspectorName}</div>
                            <div className="text-[10px] text-slate-400">
                              {new Date(qc.inspectedAt).toLocaleDateString('en-IN', {
                                day: '2-digit',
                                month: 'short',
                              })}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {qcPagination.totalPages > 1 && (
                <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200 bg-slate-50 text-xs">
                  <p className="text-slate-500">
                    Page <span className="font-semibold text-slate-700">{qcPage + 1}</span> of{' '}
                    <span className="font-semibold text-slate-700">{qcPagination.totalPages}</span>
                  </p>
                  <div className="flex space-x-2">
                    <Button
                      variant="outline"
                      size="xs"
                      disabled={qcPage === 0}
                      onClick={() => setQcPage((p) => Math.max(0, p - 1))}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="xs"
                      disabled={qcPage >= qcPagination.totalPages - 1}
                      onClick={() => setQcPage((p) => p + 1)}
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

      {/* Review & Authorize Reprint Modal */}
      {selectedReprint && (
        <ReviewReprintModal
          isOpen={!!selectedReprint}
          onClose={() => setSelectedReprint(null)}
          reprint={selectedReprint}
          onApprove={(payload) => approveMutation.mutate({ id: selectedReprint.id, payload })}
          onReject={(payload) => rejectMutation.mutate({ id: selectedReprint.id, payload })}
          isSubmitting={approveMutation.isPending || rejectMutation.isPending}
        />
      )}

      {/* Admin Submit Quality Check Modal */}
      <SubmitQualityCheckModal
        isOpen={isSubmitQcOpen}
        onClose={() => {
          setIsSubmitQcOpen(false);
          setSelectedAssignmentId('');
        }}
        onSubmit={(payload) => submitAdminQcMutation.mutate(payload)}
        isSubmitting={submitAdminQcMutation.isPending}
        maxQuantity={assignmentDetailRes?.totalAssignedQuantity || activeSelectedAssignment?.totalQuantity || 100}
        assignments={assignments}
        selectedAssignmentId={selectedAssignmentId || effectiveAssignmentId}
        onSelectAssignment={setSelectedAssignmentId}
        assignment={assignmentDetailRes || activeSelectedAssignment}
        students={assignmentDetailRes?.students}
        cardSpecs={assignmentDetailRes?.specs}
      />
    </div>
  );
}
