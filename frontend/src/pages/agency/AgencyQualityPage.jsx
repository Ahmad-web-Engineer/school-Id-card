import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import EmptyState from '../../components/common/EmptyState';
import SubmitQualityCheckModal from '../../features/production/SubmitQualityCheckModal';
import { QC_STATUSES } from '../../features/production/productionConstants';

export default function AgencyQualityPage() {
  const queryClient = useQueryClient();
  const [selectedAssignmentId, setSelectedAssignmentId] = useState('');
  const [isSubmitOpen, setIsSubmitOpen] = useState(false);

  // Fetch agency jobs
  const { data: jobsData, isLoading: jobsLoading } = useQuery({
    queryKey: ['agency-jobs-list-qc'],
    queryFn: async () => {
      const res = await api.get('/agency/jobs?size=50');
      return res.data?.data?.data || [];
    },
  });

  const activeJobs = (jobsData || []).filter(
    (j) => j.assignmentStatus === 'IN_PRODUCTION' || j.assignmentStatus === 'COMPLETED'
  );

  const currentJobId = selectedAssignmentId || (activeJobs[0]?.id || '');

  // Fetch quality checks
  const {
    data: qcData,
    isLoading: qcLoading,
    error: qcError,
  } = useQuery({
    queryKey: ['agency-qc-checks', currentJobId],
    queryFn: async () => {
      if (!currentJobId) return [];
      const res = await api.get(`/agency/jobs/${currentJobId}/quality-checks`);
      return res.data?.data?.data || [];
    },
    enabled: !!currentJobId,
  });

  // Submit QC Mutation
  const submitQcMutation = useMutation({
    mutationFn: async (payload) => {
      const res = await api.post(`/agency/jobs/${currentJobId}/quality-checks`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['agency-qc-checks', currentJobId]);
      setIsSubmitOpen(false);
    },
  });

  const selectedJob = activeJobs.find((j) => j.id === currentJobId);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Quality Control (QC)</h1>
          <p className="text-sm text-slate-600 mt-1">
            Conduct visual, dimensional, and barcode readability checks before final dispatch packaging.
          </p>
        </div>

        {selectedJob && (
          <Button variant="primary" onClick={() => setIsSubmitOpen(true)}>
            + Submit Inspection
          </Button>
        )}
      </div>

      {/* Job Selector Strip */}
      <Card className="p-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center space-x-3">
            <label className="text-sm font-semibold text-slate-700 whitespace-nowrap">
              Inspecting Job:
            </label>
            {jobsLoading ? (
              <span className="text-sm text-slate-500">Loading jobs...</span>
            ) : activeJobs.length === 0 ? (
              <span className="text-sm text-amber-600">No active production jobs to inspect</span>
            ) : (
              <select
                value={currentJobId}
                onChange={(e) => setSelectedAssignmentId(e.target.value)}
                className="w-full sm:w-auto px-3 py-1.5 text-sm font-medium border border-slate-300 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500 bg-white"
              >
                {activeJobs.map((j) => (
                  <option key={j.id} value={j.id}>
                    {j.jobNumber} — {j.academicSession} ({j.totalQuantity} cards)
                  </option>
                ))}
              </select>
            )}
          </div>

          {selectedJob && (
            <div className="text-xs text-slate-500">
              Total Cards: <strong className="text-slate-800">{selectedJob.totalQuantity}</strong>
            </div>
          )}
        </div>
      </Card>

      {/* QC Reports Table */}
      {qcLoading ? (
        <div className="py-12 flex justify-center">
          <LoadingSpinner size="lg" />
        </div>
      ) : qcError ? (
        <ErrorState
          title="Failed to load quality reports"
          message={qcError.message}
          onRetry={() => queryClient.invalidateQueries(['agency-qc-checks', currentJobId])}
        />
      ) : !currentJobId || qcData.length === 0 ? (
        <EmptyState
          title="No inspection reports recorded"
          description="Perform quality checks on finished print batches to ensure compliance."
          action={
            selectedJob ? (
              <Button variant="primary" onClick={() => setIsSubmitOpen(true)}>
                Record First Inspection
              </Button>
            ) : null
          }
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Check #
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Inspected / Accepted
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Defects & Category
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Inspected By
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Date & Time
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-slate-200">
                {qcData.map((qc) => {
                  const statusCfg = QC_STATUSES[qc.inspectionStatus] || {
                    label: qc.inspectionStatus,
                    color: 'bg-slate-100 text-slate-700 border-slate-300',
                  };

                  return (
                    <tr key={qc.id} className="hover:bg-slate-50/70 transition-colors">
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-mono font-bold text-slate-900">
                        {qc.checkNumber}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`px-2.5 py-0.5 text-xs font-semibold rounded-full border ${statusCfg.color}`}>
                          {statusCfg.label}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-900">
                        <div>
                          <span className="font-semibold text-emerald-600">{qc.acceptedQuantity}</span>
                          <span className="text-slate-400"> / </span>
                          <span className="font-semibold text-slate-800">{qc.inspectedQuantity}</span> cards
                        </div>
                        {qc.rejectedQuantity > 0 && (
                          <div className="text-xs text-rose-600 font-medium mt-0.5">
                            {qc.rejectedQuantity} card(s) rejected
                          </div>
                        )}
                      </td>
                      <td className="px-6 py-4 text-sm text-slate-600 max-w-xs">
                        {qc.defectCategory ? (
                          <div className="space-y-1">
                            <span className="inline-block px-2 py-0.5 text-xs font-medium bg-rose-50 text-rose-700 border border-rose-200 rounded">
                              {qc.defectCategory}
                            </span>
                            {qc.defectDescription && (
                              <p className="text-xs text-slate-700 truncate">{qc.defectDescription}</p>
                            )}
                          </div>
                        ) : (
                          <span className="text-xs text-slate-400">Zero defects found</span>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-600">
                        {qc.inspectorName}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-xs text-slate-500">
                        {new Date(qc.inspectedAt).toLocaleString()}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Submit Quality Check Modal */}
      <SubmitQualityCheckModal
        isOpen={isSubmitOpen}
        onClose={() => setIsSubmitOpen(false)}
        onSubmit={(payload) => submitQcMutation.mutate(payload)}
        isSubmitting={submitQcMutation.isPending}
        maxQuantity={selectedJob?.totalQuantity || 100}
      />
    </div>
  );
}
