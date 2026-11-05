import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import { extractArray } from '../../services/responseUtils';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import EmptyState from '../../components/common/EmptyState';
import CreateBatchModal from '../../features/production/CreateBatchModal';
import ProductionUpdateModal from '../../features/production/ProductionUpdateModal';
import { BATCH_STATUSES } from '../../features/production/productionConstants';

export default function AgencyProductionPage() {
  const queryClient = useQueryClient();
  const [selectedAssignmentId, setSelectedAssignmentId] = useState('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [updateBatch, setUpdateBatch] = useState(null);

  // 1. Fetch agency assigned jobs to populate job selector
  const { data: jobsData, isLoading: jobsLoading } = useQuery({
    queryKey: ['agency-jobs-list'],
    queryFn: async () => {
      const res = await api.get('/agency/jobs?size=50');
      return extractArray(res);
    },
  });

  const activeJobs = (jobsData || []).filter(
    (j) => j.assignmentStatus === 'AGENCY_ACCEPTED' || j.assignmentStatus === 'IN_PRODUCTION' || j.assignmentStatus === 'COMPLETED'
  );

  const currentJobId = selectedAssignmentId || (activeJobs[0]?.id || '');

  // 2. Fetch production batches for the selected job
  const {
    data: batchesData,
    isLoading: batchesLoading,
    error: batchesError,
  } = useQuery({
    queryKey: ['agency-batches', currentJobId],
    queryFn: async () => {
      if (!currentJobId) return [];
      const res = await api.get(`/agency/jobs/${currentJobId}/batches`);
      return res.data?.data?.data || [];
    },
    enabled: !!currentJobId,
  });

  // 3. Start Production Mutation
  const startProdMutation = useMutation({
    mutationFn: async (jobId) => {
      const res = await api.post(`/agency/jobs/${jobId}/start`);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['agency-jobs-list']);
      queryClient.invalidateQueries(['agency-batches', currentJobId]);
    },
  });

  // 4. Create Batch Mutation
  const createBatchMutation = useMutation({
    mutationFn: async (payload) => {
      const res = await api.post(`/agency/jobs/${currentJobId}/batches`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['agency-batches', currentJobId]);
      setIsCreateOpen(false);
    },
  });

  // 5. Post Update Mutation
  const updateMutation = useMutation({
    mutationFn: async (payload) => {
      const res = await api.post(`/agency/batches/${updateBatch.id}/updates`, payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['agency-batches', currentJobId]);
      setUpdateBatch(null);
    },
  });

  const selectedJob = activeJobs.find((j) => j.id === currentJobId);

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Production Floor</h1>
          <p className="text-sm text-slate-600 mt-1">
            Manage print runs, log manufactured quantities, and track machine outputs.
          </p>
        </div>

        {selectedJob && (
          <div className="flex flex-wrap items-center gap-3">
            {selectedJob.assignmentStatus === 'AGENCY_ACCEPTED' && (
              <Button
                variant="primary"
                onClick={() => startProdMutation.mutate(currentJobId)}
                disabled={startProdMutation.isPending}
              >
                {startProdMutation.isPending ? 'Starting...' : 'Start Production'}
              </Button>
            )}
            <Button
              variant="outline"
              onClick={() => setIsCreateOpen(true)}
              disabled={selectedJob.assignmentStatus !== 'IN_PRODUCTION'}
            >
              + New Batch
            </Button>
          </div>
        )}
      </div>

      {/* Job Selector Strip */}
      <Card className="p-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center space-x-3">
            <label className="text-sm font-semibold text-slate-700 whitespace-nowrap">
              Active Job:
            </label>
            {jobsLoading ? (
              <span className="text-sm text-slate-500">Loading jobs...</span>
            ) : activeJobs.length === 0 ? (
              <span className="text-sm text-amber-600">No accepted jobs in queue</span>
            ) : (
              <select
                value={currentJobId}
                onChange={(e) => setSelectedAssignmentId(e.target.value)}
                className="w-full sm:w-auto px-3 py-1.5 text-sm font-medium border border-slate-300 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500 bg-white"
              >
                {activeJobs.map((j) => (
                  <option key={j.id} value={j.id}>
                    {j.jobNumber} — {j.academicSession} ({j.totalQuantity} cards) [{j.assignmentStatus}]
                  </option>
                ))}
              </select>
            )}
          </div>

          {selectedJob && (
            <div className="text-xs text-slate-500 flex items-center gap-4">
              <span>Target Cards: <strong className="text-slate-800">{selectedJob.totalQuantity}</strong></span>
              <span>Due: <strong className="text-slate-800">{selectedJob.expectedDeliveryDate || 'N/A'}</strong></span>
            </div>
          )}
        </div>
      </Card>

      {/* Production Batches List */}
      {batchesLoading ? (
        <div className="py-12 flex justify-center">
          <LoadingSpinner size="lg" />
        </div>
      ) : batchesError ? (
        <ErrorState
          title="Failed to load batches"
          message={batchesError.message}
          onRetry={() => queryClient.invalidateQueries(['agency-batches', currentJobId])}
        />
      ) : !currentJobId || batchesData.length === 0 ? (
        <EmptyState
          title="No production batches found"
          description={
            selectedJob?.assignmentStatus === 'AGENCY_ACCEPTED'
              ? 'Click "Start Production" above to initiate machine batches.'
              : 'Create a new batch or initiate production for this job.'
          }
          action={
            selectedJob?.assignmentStatus === 'AGENCY_ACCEPTED' ? (
              <Button variant="primary" onClick={() => startProdMutation.mutate(currentJobId)}>
                Start Production
              </Button>
            ) : null
          }
        />
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {batchesData.map((batch) => {
              const statusCfg = BATCH_STATUSES[batch.productionStatus] || {
                label: batch.productionStatus,
                color: 'bg-slate-100 text-slate-700 border-slate-300',
              };
              const percent = batch.plannedQuantity > 0
                ? Math.min(100, Math.round((batch.actualQuantity / batch.plannedQuantity) * 100))
                : 0;

              return (
                <Card key={batch.id} className="p-5 flex flex-col justify-between">
                  <div>
                    <div className="flex items-center justify-between gap-2 mb-3">
                      <span className="font-mono font-bold text-slate-900 text-base">
                        {batch.batchNumber}
                      </span>
                      <span className={`px-2 py-0.5 text-xs font-semibold rounded-full border ${statusCfg.color}`}>
                        {statusCfg.label}
                      </span>
                    </div>

                    {batch.notes && (
                      <p className="text-xs text-slate-600 mb-4 bg-slate-50 p-2 rounded border border-slate-100">
                        {batch.notes}
                      </p>
                    )}

                    <div className="space-y-1 mb-4">
                      <div className="flex justify-between text-xs text-slate-600">
                        <span>Manufactured</span>
                        <span className="font-semibold text-slate-900">
                          {batch.actualQuantity} / {batch.plannedQuantity} cards ({percent}%)
                        </span>
                      </div>
                      <div className="w-full bg-slate-200 rounded-full h-2 overflow-hidden">
                        <div
                          className={`h-2 transition-all duration-300 ${
                            percent >= 100 ? 'bg-emerald-600' : 'bg-blue-600'
                          }`}
                          style={{ width: `${percent}%` }}
                        />
                      </div>
                    </div>

                    <div className="text-xs text-slate-500 space-y-1">
                      <div>
                        Started: {batch.startedAt ? new Date(batch.startedAt).toLocaleString() : 'Not started'}
                      </div>
                      {batch.completedAt && (
                        <div className="text-emerald-700 font-medium">
                          Completed: {new Date(batch.completedAt).toLocaleString()}
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="mt-5 pt-3 border-t border-slate-100 flex justify-end">
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => setUpdateBatch(batch)}
                      disabled={batch.productionStatus === 'PRINTING_COMPLETED' || batch.productionStatus === 'CANCELLED'}
                    >
                      {batch.productionStatus === 'PRINTING_COMPLETED' ? 'Completed' : 'Post Progress'}
                    </Button>
                  </div>
                </Card>
              );
            })}
          </div>
        </div>
      )}

      {/* Create Batch Modal */}
      <CreateBatchModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSubmit={(payload) => createBatchMutation.mutate(payload)}
        isSubmitting={createBatchMutation.isPending}
        totalAssigned={selectedJob?.totalQuantity || 100}
      />

      {/* Production Update Modal */}
      <ProductionUpdateModal
        isOpen={!!updateBatch}
        onClose={() => setUpdateBatch(null)}
        batch={updateBatch}
        onSubmit={(payload) => updateMutation.mutate(payload)}
        isSubmitting={updateMutation.isPending}
      />
    </div>
  );
}
