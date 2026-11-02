import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import { ASSIGNMENT_STATUSES } from '../../features/assignments/assignmentsConstants';
import {
  ArrowLeft,
  Briefcase,
  Calendar,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  Printer,
  FileText,
  Layers,
  Info,
  Package
} from 'lucide-react';

export default function AgencyJobDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [isAcceptModalOpen, setIsAcceptModalOpen] = useState(false);
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // 1. Fetch Job Details
  const {
    data: jobRes,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['agency-job-detail', id],
    queryFn: () => api.get(`/agency/jobs/${id}`),
    enabled: !!id,
  });

  const job = jobRes?.data;

  // Accept Mutation
  const acceptMutation = useMutation({
    mutationFn: () => api.post(`/agency/jobs/${id}/accept`),
    onSuccess: () => {
      setActionSuccess('Job accepted successfully! You can now start print production.');
      setIsAcceptModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['agency-job-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['agency-jobs'] });
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to accept job.');
    },
  });

  // Reject Mutation
  const rejectMutation = useMutation({
    mutationFn: (reason) => api.post(`/agency/jobs/${id}/reject`, { rejectionReason: reason }),
    onSuccess: () => {
      setActionSuccess('Job has been rejected and returned to the platform administrator.');
      setIsRejectModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['agency-job-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['agency-jobs'] });
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to reject job.');
    },
  });

  if (isLoading) {
    return (
      <div className="py-24 flex justify-center">
        <LoadingSpinner size="lg" className="text-indigo-600" />
      </div>
    );
  }

  if (isError || !job) {
    return (
      <ErrorState
        title="Job Not Found"
        message={error?.message || 'Could not load job details or access was denied.'}
        onRetry={refetch}
      />
    );
  }

  const statusConfig = ASSIGNMENT_STATUSES[job.assignmentStatus] || {
    label: job.assignmentStatus,
    variant: 'neutral',
  };

  const specs = job.cardSpecifications || {
    widthMm: 85.6,
    heightMm: 54.0,
    bleedMm: 3.0,
    templateName: 'Standard PVC Card',
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/agency/jobs')}
            className="p-1.5"
            aria-label="Back to assigned jobs"
          >
            <ArrowLeft className="h-5 w-5 text-slate-500" />
          </Button>
          <div>
            <div className="flex items-center space-x-2">
              <h1 className="text-xl font-bold text-slate-900 font-mono">{job.jobNumber}</h1>
              <Badge variant={statusConfig.variant} size="sm">
                {statusConfig.label}
              </Badge>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Academic Session: <strong className="text-slate-700">{job.academicSession}</strong> • Assigned {new Date(job.assignedAt).toLocaleDateString('en-IN')}
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        {job.assignmentStatus === 'ASSIGNED' && (
          <div className="flex items-center space-x-2">
            <Button
              variant="outline"
              size="sm"
              className="text-rose-600 border-rose-200 hover:bg-rose-50"
              onClick={() => setIsRejectModalOpen(true)}
            >
              <XCircle className="h-4 w-4 mr-1.5" />
              Reject Job
            </Button>
            <Button
              size="sm"
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
              onClick={() => setIsAcceptModalOpen(true)}
            >
              <CheckCircle2 className="h-4 w-4 mr-1.5" />
              Accept Job
            </Button>
          </div>
        )}
      </div>

      {actionSuccess && (
        <div className="rounded-lg bg-emerald-50 border border-emerald-200 p-3 flex items-center justify-between text-xs text-emerald-800">
          <div className="flex items-center space-x-2">
            <CheckCircle2 className="h-4 w-4 text-emerald-600" />
            <span>{actionSuccess}</span>
          </div>
          <button onClick={() => setActionSuccess(null)} className="font-semibold underline ml-2">Dismiss</button>
        </div>
      )}

      {actionError && (
        <div className="rounded-lg bg-rose-50 border border-rose-200 p-3 flex items-center justify-between text-xs text-rose-800">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="h-4 w-4 text-rose-600" />
            <span>{actionError}</span>
          </div>
          <button onClick={() => setActionError(null)} className="font-semibold underline ml-2">Dismiss</button>
        </div>
      )}

      {/* Acceptance Status Notice */}
      {job.assignmentStatus === 'AGENCY_ACCEPTED' && (
        <div className="rounded-lg bg-sky-50 border border-sky-200 p-4 flex items-start space-x-3">
          <CheckCircle2 className="h-5 w-5 text-sky-600 mt-0.5 flex-shrink-0" />
          <div>
            <h4 className="text-sm font-semibold text-sky-900">Job Accepted by Your Agency</h4>
            <p className="text-xs text-sky-700 mt-0.5">
              Production commitment registered. Please adhere to the specified delivery deadline ({job.expectedDeliveryDate || 'N/A'}).
            </p>
          </div>
        </div>
      )}

      {job.assignmentStatus === 'REJECTED' && (
        <div className="rounded-lg bg-rose-50 border border-rose-200 p-4 flex items-start space-x-3">
          <AlertTriangle className="h-5 w-5 text-rose-600 mt-0.5 flex-shrink-0" />
          <div>
            <h4 className="text-sm font-semibold text-rose-900">Job Rejected</h4>
            <p className="text-xs text-rose-700 mt-0.5">Reason: {job.rejectionReason || 'No reason provided'}</p>
          </div>
        </div>
      )}

      {/* Grid: Print Specifications & Delivery Commitments */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Left Column: Technical Specifications */}
        <div className="md:col-span-2 space-y-6">
          {/* Card Dimensions & Technical Specs Card */}
          <Card className="p-5">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2 mb-4 border-b border-slate-100 pb-2">
              <Printer className="h-4 w-4 text-indigo-600" />
              Technical Print Specifications
            </h3>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                <span className="text-[10px] uppercase font-semibold text-slate-400 block">Card Width</span>
                <span className="text-base font-bold text-slate-900 font-mono mt-0.5 block">{specs.widthMm} mm</span>
                <span className="text-[10px] text-slate-500">Standard CR80</span>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                <span className="text-[10px] uppercase font-semibold text-slate-400 block">Card Height</span>
                <span className="text-base font-bold text-slate-900 font-mono mt-0.5 block">{specs.heightMm} mm</span>
                <span className="text-[10px] text-slate-500">Standard CR80</span>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                <span className="text-[10px] uppercase font-semibold text-slate-400 block">Bleed Margin</span>
                <span className="text-base font-bold text-indigo-700 font-mono mt-0.5 block">{specs.bleedMm} mm</span>
                <span className="text-[10px] text-slate-500">All 4 edges</span>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                <span className="text-[10px] uppercase font-semibold text-slate-400 block">Template Style</span>
                <span className="text-xs font-bold text-slate-800 truncate mt-1 block" title={specs.templateName}>
                  {specs.templateName}
                </span>
                <span className="text-[10px] text-slate-500">Double-sided</span>
              </div>
            </div>

            {job.notes && (
              <div className="mt-4 p-3 bg-amber-50 rounded-lg border border-amber-200 text-xs flex items-start space-x-2">
                <Info className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
                <div>
                  <span className="font-semibold text-amber-900">Admin Instructions:</span>
                  <p className="text-amber-800 mt-0.5">{job.notes}</p>
                </div>
              </div>
            )}
          </Card>

          {/* Allocated Production Items Table */}
          <Card className="p-5">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2 mb-4 border-b border-slate-100 pb-2">
              <Package className="h-4 w-4 text-indigo-600" />
              Allocated Job Items
            </h3>

            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
                <thead className="bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider">
                  <tr>
                    <th className="px-3 py-2">Item Type</th>
                    <th className="px-3 py-2 text-right">Assigned Qty</th>
                    <th className="px-3 py-2 text-right">Printed</th>
                    <th className="px-3 py-2 text-right">Pending</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {job.items?.map((item) => (
                    <tr key={item.id} className="hover:bg-slate-50">
                      <td className="px-3 py-2.5 font-medium text-slate-800">{item.productType}</td>
                      <td className="px-3 py-2.5 text-right font-bold text-slate-900">{item.assignedQuantity}</td>
                      <td className="px-3 py-2.5 text-right font-mono text-emerald-600">{item.printedQuantity}</td>
                      <td className="px-3 py-2.5 text-right font-mono text-slate-500">{item.pendingQuantity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
        </div>

        {/* Right Column: Schedule & Summary */}
        <div className="space-y-6">
          <Card className="p-5 space-y-4">
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
              Schedule & Quantities
            </h4>

            <div>
              <span className="text-[11px] text-slate-500 block">Total Quantity to Print</span>
              <p className="text-2xl font-bold text-slate-900 mt-0.5">{job.totalQuantity} units</p>
            </div>

            <div>
              <span className="text-[11px] text-slate-500 block">Expected Delivery Date</span>
              <p className="text-sm font-semibold text-slate-800 mt-0.5 flex items-center gap-1.5">
                <Calendar className="h-4 w-4 text-indigo-600" />
                {job.expectedDeliveryDate ? (
                  new Date(job.expectedDeliveryDate).toLocaleDateString('en-IN', {
                    weekday: 'short',
                    day: '2-digit',
                    month: 'long',
                    year: 'numeric',
                  })
                ) : (
                  'Flexible / To be confirmed'
                )}
              </p>
            </div>

            <div>
              <span className="text-[11px] text-slate-500 block">Assignment Date</span>
              <p className="text-xs text-slate-700 mt-0.5">
                {new Date(job.assignedAt).toLocaleDateString('en-IN', {
                  day: '2-digit',
                  month: 'short',
                  year: 'numeric',
                })}
              </p>
            </div>
          </Card>
        </div>
      </div>

      {/* Accept Job Confirmation Modal */}
      <Modal
        isOpen={isAcceptModalOpen}
        onClose={() => setIsAcceptModalOpen(false)}
        title="Confirm Job Acceptance"
        size="sm"
      >
        <div className="space-y-4 text-xs">
          <p className="text-slate-600">
            By accepting Job <strong className="font-mono text-indigo-700">{job.jobNumber}</strong>, your agency commits to printing <strong>{job.totalQuantity} items</strong> by <strong>{job.expectedDeliveryDate || 'the requested timeline'}</strong>.
          </p>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button size="sm" variant="ghost" onClick={() => setIsAcceptModalOpen(false)}>
              Cancel
            </Button>
            <Button
              size="sm"
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
              onClick={() => acceptMutation.mutate()}
              disabled={acceptMutation.isPending}
            >
              {acceptMutation.isPending ? <LoadingSpinner size="sm" className="mr-1" /> : <CheckCircle2 className="h-4 w-4 mr-1.5" />}
              Confirm Acceptance
            </Button>
          </div>
        </div>
      </Modal>

      {/* Reject Job Modal */}
      <Modal
        isOpen={isRejectModalOpen}
        onClose={() => setIsRejectModalOpen(false)}
        title="Reject Job Assignment"
        size="md"
      >
        <div className="space-y-4 text-xs">
          <p className="text-slate-600">
            Please provide a reason for rejecting this print run so the platform administrator can reallocate the batch.
          </p>

          <div>
            <label className="block text-[11px] font-medium text-slate-700 mb-1">
              Rejection Reason <span className="text-rose-500">*</span>
            </label>
            <textarea
              rows="3"
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              placeholder="e.g. Current machine maintenance prevents meeting the required delivery date."
              className="w-full text-xs p-2.5 rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button size="sm" variant="ghost" onClick={() => setIsRejectModalOpen(false)}>
              Cancel
            </Button>
            <Button
              size="sm"
              variant="outline"
              className="text-rose-600 border-rose-300 hover:bg-rose-50"
              onClick={() => rejectMutation.mutate(rejectionReason.trim())}
              disabled={rejectMutation.isPending || !rejectionReason.trim()}
            >
              {rejectMutation.isPending ? <LoadingSpinner size="sm" className="mr-1" /> : <XCircle className="h-4 w-4 mr-1.5" />}
              Submit Rejection
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
