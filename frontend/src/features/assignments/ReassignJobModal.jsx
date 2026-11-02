import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import api from '../../services/api';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { AlertCircle, RefreshCw, Building2 } from 'lucide-react';

export default function ReassignJobModal({ isOpen, onClose, assignment, onSuccess }) {
  const [newAgencyId, setNewAgencyId] = useState('');
  const [agreedRate, setAgreedRate] = useState(
    assignment?.agreedRate ? Number(assignment.agreedRate).toFixed(2) : '25.00'
  );
  const [expectedDeliveryDate, setExpectedDeliveryDate] = useState('');
  const [reassignmentReason, setReassignmentReason] = useState('');
  const [error, setError] = useState(null);

  // Fetch agencies
  const { data: agenciesRes, isLoading: isLoadingAgencies } = useQuery({
    queryKey: ['admin-agencies-list'],
    queryFn: () => api.get('/admin/agencies', { params: { page: 0, size: 200 } }),
    enabled: isOpen,
  });

  const agencies = agenciesRes?.data?.content || [];

  const reassignMutation = useMutation({
    mutationFn: (payload) =>
      api.post(`/admin/assignments/${assignment.id}/reassign`, payload),
    onSuccess: (res) => {
      if (onSuccess) onSuccess(res.data);
      onClose();
    },
    onError: (err) => {
      setError(err.message || 'Failed to reassign job.');
    },
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    setError(null);

    if (!newAgencyId) {
      setError('Please select a replacement agency.');
      return;
    }

    if (!reassignmentReason.trim()) {
      setError('Please provide a reason for the reassignment.');
      return;
    }

    const rate = parseFloat(agreedRate);
    if (isNaN(rate) || rate < 0) {
      setError('Please enter a valid agreed rate.');
      return;
    }

    reassignMutation.mutate({
      newAgencyId,
      agreedRate: rate,
      expectedDeliveryDate: expectedDeliveryDate || undefined,
      reassignmentReason: reassignmentReason.trim(),
    });
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Reassign Job: ${assignment?.assignmentNumber || ''}`}
      size="md"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg flex items-center gap-2 text-xs text-rose-700">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 text-xs space-y-1">
          <p className="text-slate-600">
            Current Agency: <strong className="text-slate-800">{assignment?.agencyName}</strong>
          </p>
          <p className="text-slate-600">
            Quantity to Reallocate: <strong className="text-slate-900">{assignment?.totalAssignedQuantity} items</strong>
          </p>
          {assignment?.rejectionReason && (
            <p className="text-rose-700">
              Rejection Reason: <em>"{assignment.rejectionReason}"</em>
            </p>
          )}
        </div>

        {/* Replacement Agency */}
        <div>
          <label className="block text-xs font-medium text-slate-700 mb-1">
            Replacement Agency <span className="text-rose-500">*</span>
          </label>
          <select
            value={newAgencyId}
            onChange={(e) => setNewAgencyId(e.target.value)}
            className="w-full text-xs px-3 py-2 rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            required
          >
            <option value="">-- Select Replacement Agency --</option>
            {agencies
              .filter((a) => a.id !== assignment?.agencyId)
              .map((agency) => (
                <option key={agency.id} value={agency.id}>
                  {agency.name} (Capacity: {agency.capacityPerDay || 'N/A'}/day)
                </option>
              ))}
          </select>
        </div>

        {/* Agreed Rate */}
        <div>
          <label className="block text-xs font-medium text-slate-700 mb-1">
            Agreed Purchase Rate (₹) <span className="text-rose-500">*</span>
          </label>
          <Input
            type="number"
            step="0.01"
            min="0"
            value={agreedRate}
            onChange={(e) => setAgreedRate(e.target.value)}
            placeholder="25.00"
            required
          />
        </div>

        {/* Expected Delivery Date */}
        <div>
          <label className="block text-xs font-medium text-slate-700 mb-1">
            Expected Delivery Date
          </label>
          <Input
            type="date"
            value={expectedDeliveryDate}
            onChange={(e) => setExpectedDeliveryDate(e.target.value)}
          />
        </div>

        {/* Reason */}
        <div>
          <label className="block text-xs font-medium text-slate-700 mb-1">
            Reassignment Reason <span className="text-rose-500">*</span>
          </label>
          <textarea
            rows="3"
            value={reassignmentReason}
            onChange={(e) => setReassignmentReason(e.target.value)}
            placeholder="Explain why this job is being reassigned..."
            className="w-full text-xs p-2.5 rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            required
          />
        </div>

        <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
          <Button type="button" size="sm" variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            size="sm"
            disabled={reassignMutation.isPending || isLoadingAgencies}
          >
            {reassignMutation.isPending ? (
              <>
                <LoadingSpinner size="sm" className="mr-1.5" />
                Reassigning...
              </>
            ) : (
              <>
                <RefreshCw className="h-3.5 w-3.5 mr-1.5" />
                Confirm Reassignment
              </>
            )}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
