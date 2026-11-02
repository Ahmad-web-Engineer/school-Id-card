import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Textarea from '../../components/common/Textarea';
import Button from '../../components/common/Button';
import { DEFECT_CATEGORIES } from './productionConstants';

export default function SubmitQualityCheckModal({
  isOpen,
  onClose,
  onSubmit,
  isSubmitting,
  maxQuantity = 100,
  assignments = [],
  selectedAssignmentId = '',
  onSelectAssignment,
}) {
  const [inspectedQuantity, setInspectedQuantity] = useState(maxQuantity > 0 ? maxQuantity : '');
  const [acceptedQuantity, setAcceptedQuantity] = useState(maxQuantity > 0 ? maxQuantity : '');
  const [rejectedQuantity, setRejectedQuantity] = useState('0');
  const [defectCategory, setDefectCategory] = useState('PRINT_ALIGNMENT');
  const [defectDescription, setDefectDescription] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      const initial = maxQuantity > 0 ? maxQuantity : 100;
      setInspectedQuantity(initial);
      setAcceptedQuantity(initial);
      setRejectedQuantity('0');
      setError('');
    }
  }, [isOpen, maxQuantity]);

  const handleInspectedChange = (val) => {
    setInspectedQuantity(val);
    const ins = parseInt(val, 10) || 0;
    const rej = parseInt(rejectedQuantity, 10) || 0;
    setAcceptedQuantity(Math.max(0, ins - rej));
  };

  const handleRejectedChange = (val) => {
    setRejectedQuantity(val);
    const ins = parseInt(inspectedQuantity, 10) || 0;
    const rej = parseInt(val, 10) || 0;
    setAcceptedQuantity(Math.max(0, ins - rej));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const ins = parseInt(inspectedQuantity, 10);
    const acc = parseInt(acceptedQuantity, 10);
    const rej = parseInt(rejectedQuantity, 10);

    if (isNaN(ins) || ins <= 0) {
      setError('Inspected quantity must be at least 1');
      return;
    }
    if (isNaN(acc) || acc < 0) {
      setError('Accepted quantity cannot be negative');
      return;
    }
    if (isNaN(rej) || rej < 0) {
      setError('Rejected quantity cannot be negative');
      return;
    }
    if (ins !== acc + rej) {
      setError(`Sum of accepted (${acc}) and rejected (${rej}) must equal inspected (${ins})`);
      return;
    }
    if (rej > 0 && !defectDescription.trim()) {
      setError('Please provide defect description for rejected cards');
      return;
    }

    setError('');
    onSubmit({
      inspectedQuantity: ins,
      acceptedQuantity: acc,
      rejectedQuantity: rej,
      defectCategory: rej > 0 ? defectCategory : null,
      defectDescription: rej > 0 ? defectDescription.trim() : null,
      notes: notes.trim() || null,
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Submit Quality Inspection Report">
      <form onSubmit={handleSubmit} className="space-y-4">
        {assignments && assignments.length > 0 && (
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Select Production Job / Assignment <span className="text-rose-500">*</span>
            </label>
            <select
              value={selectedAssignmentId}
              onChange={(e) => {
                const id = e.target.value;
                onSelectAssignment?.(id);
                const found = assignments.find((a) => a.id === id);
                if (found?.totalQuantity) {
                  setInspectedQuantity(found.totalQuantity);
                  setAcceptedQuantity(found.totalQuantity);
                  setRejectedQuantity('0');
                }
              }}
              className="w-full text-xs border border-slate-300 rounded-md py-2 px-2.5 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
              required
            >
              <option value="">-- Choose an Assignment --</option>
              {assignments.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.jobNumber || a.orderNumber} — {a.agencyName || 'Agency'} ({a.totalQuantity || 0} cards) [{a.assignmentStatus}]
                </option>
              ))}
            </select>
          </div>
        )}

        {error && (
          <div className="p-3 text-sm text-rose-700 bg-rose-50 border border-rose-200 rounded-md">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Total Inspected <span className="text-rose-500">*</span>
            </label>
            <Input
              type="number"
              min="1"
              value={inspectedQuantity}
              onChange={(e) => handleInspectedChange(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Accepted Cards <span className="text-rose-500">*</span>
            </label>
            <Input
              type="number"
              min="0"
              value={acceptedQuantity}
              onChange={(e) => setAcceptedQuantity(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Rejected Cards <span className="text-rose-500">*</span>
            </label>
            <Input
              type="number"
              min="0"
              value={rejectedQuantity}
              onChange={(e) => handleRejectedChange(e.target.value)}
              required
            />
          </div>
        </div>

        {parseInt(rejectedQuantity, 10) > 0 && (
          <div className="p-4 bg-rose-50 border border-rose-200 rounded-lg space-y-3">
            <h4 className="text-sm font-semibold text-rose-900">
              Defect Analysis & Automatic Reprint Trigger
            </h4>
            <p className="text-xs text-rose-700">
              {rejectedQuantity} rejected card(s) will automatically create a reprint request for Super Admin review.
            </p>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Defect Category <span className="text-rose-500">*</span>
              </label>
              <Select
                value={defectCategory}
                onChange={(e) => setDefectCategory(e.target.value)}
                options={DEFECT_CATEGORIES}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Defect Description <span className="text-rose-500">*</span>
              </label>
              <Textarea
                rows={2}
                value={defectDescription}
                onChange={(e) => setDefectDescription(e.target.value)}
                placeholder="Explain the defect (e.g. alignment shift > 2mm, blurred barcode, edge delamination)"
                required
              />
            </div>
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            General Inspection Notes
          </label>
          <Textarea
            rows={2}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="e.g. Colors match approved digital proofs, barcode scan verified"
          />
        </div>

        <div className="flex justify-end space-x-3 pt-3 border-t border-slate-200">
          <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" disabled={isSubmitting}>
            {isSubmitting ? 'Submitting Inspection...' : 'Submit Quality Check'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
