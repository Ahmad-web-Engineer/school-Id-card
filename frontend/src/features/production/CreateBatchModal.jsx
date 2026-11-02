import React, { useState } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Textarea from '../../components/common/Textarea';
import Button from '../../components/common/Button';

export default function CreateBatchModal({ isOpen, onClose, onSubmit, isSubmitting, totalAssigned = 0 }) {
  const [plannedQuantity, setPlannedQuantity] = useState(totalAssigned > 0 ? totalAssigned : 100);
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!plannedQuantity || plannedQuantity <= 0) {
      setError('Planned quantity must be greater than 0');
      return;
    }
    setError('');
    onSubmit({
      plannedQuantity: parseInt(plannedQuantity, 10),
      notes: notes.trim(),
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Create Production Batch">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-sm text-rose-700 bg-rose-50 border border-rose-200 rounded-md">
            {error}
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Planned Quantity <span className="text-rose-500">*</span>
          </label>
          <Input
            type="number"
            min="1"
            value={plannedQuantity}
            onChange={(e) => setPlannedQuantity(e.target.value)}
            required
            placeholder="e.g. 150"
          />
          <p className="mt-1 text-xs text-slate-500">
            Number of ID cards to produce in this specific printing batch.
          </p>
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Batch Notes / Partition
          </label>
          <Textarea
            rows={3}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="e.g. Class 10th Sections A & B, Morning Shift"
          />
        </div>

        <div className="flex justify-end space-x-3 pt-3 border-t border-slate-200">
          <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" disabled={isSubmitting}>
            {isSubmitting ? 'Creating Batch...' : 'Create Batch'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
