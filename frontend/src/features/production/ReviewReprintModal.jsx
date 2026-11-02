import React, { useState } from 'react';
import Modal from '../../components/common/Modal';
import Select from '../../components/common/Select';
import Textarea from '../../components/common/Textarea';
import Button from '../../components/common/Button';
import { COST_BEARERS } from './productionConstants';

export default function ReviewReprintModal({ isOpen, onClose, reprint, onApprove, onReject, isSubmitting }) {
  const [costBearer, setCostBearer] = useState('AGENCY');
  const [notes, setNotes] = useState('');
  const [actionType, setActionType] = useState('APPROVE'); // 'APPROVE' or 'REJECT'
  const [rejectionReason, setRejectionReason] = useState('');
  const [error, setError] = useState('');

  if (!reprint) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (actionType === 'REJECT') {
      if (!rejectionReason.trim()) {
        setError('Rejection reason is required');
        return;
      }
      setError('');
      onReject({ rejectionReason: rejectionReason.trim() });
    } else {
      setError('');
      onApprove({
        costBearer,
        notes: notes.trim() || null,
      });
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Review Reprint: ${reprint.requestNumber}`}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-sm text-rose-700 bg-rose-50 border border-rose-200 rounded-md">
            {error}
          </div>
        )}

        <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-sm space-y-2">
          <div className="flex justify-between">
            <span className="text-slate-500">Job Assignment:</span>
            <span className="font-semibold text-slate-800">{reprint.jobNumber}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-500">Replacement Cards:</span>
            <span className="font-semibold text-rose-600">{reprint.replacementQuantity} card(s)</span>
          </div>
          <div>
            <span className="text-slate-500">Defect Reason:</span>
            <p className="font-medium text-slate-800 mt-0.5">{reprint.reason}</p>
          </div>
        </div>

        <div className="flex space-x-2 border-b border-slate-200 pb-2">
          <button
            type="button"
            onClick={() => setActionType('APPROVE')}
            className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
              actionType === 'APPROVE'
                ? 'bg-emerald-600 text-white'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            Approve Reprint
          </button>
          <button
            type="button"
            onClick={() => setActionType('REJECT')}
            className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
              actionType === 'REJECT'
                ? 'bg-rose-600 text-white'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            Reject Request
          </button>
        </div>

        {actionType === 'APPROVE' ? (
          <div className="space-y-3">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Cost Bearer (Financial Liability) <span className="text-rose-500">*</span>
              </label>
              <Select
                value={costBearer}
                onChange={(e) => setCostBearer(e.target.value)}
                options={COST_BEARERS}
              />
              <p className="mt-1 text-xs text-slate-500">
                Rule 3: Monetary costs for replacements are tracked separately between Agency and Platform.
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Approval Notes
              </label>
              <Textarea
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="e.g. Approved. Defect verified as machine misalignment."
              />
            </div>
          </div>
        ) : (
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Rejection Reason <span className="text-rose-500">*</span>
            </label>
            <Textarea
              rows={3}
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              placeholder="Explain why this reprint request is rejected (e.g. defect is within acceptable tolerance)"
              required
            />
          </div>
        )}

        <div className="flex justify-end space-x-3 pt-3 border-t border-slate-200">
          <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant={actionType === 'APPROVE' ? 'primary' : 'danger'}
            disabled={isSubmitting}
          >
            {isSubmitting
              ? 'Processing...'
              : actionType === 'APPROVE'
              ? 'Confirm & Authorize Reprint'
              : 'Reject Reprint Request'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
