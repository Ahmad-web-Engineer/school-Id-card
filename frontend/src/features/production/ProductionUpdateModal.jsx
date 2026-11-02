import React, { useState } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Textarea from '../../components/common/Textarea';
import Button from '../../components/common/Button';

export default function ProductionUpdateModal({ isOpen, onClose, batch, onSubmit, isSubmitting }) {
  const [printedQuantity, setPrintedQuantity] = useState('');
  const [rejectedQuantity, setRejectedQuantity] = useState('0');
  const [remarks, setRemarks] = useState('');
  const [evidenceFileId, setEvidenceFileId] = useState('');
  const [error, setError] = useState('');

  if (!batch) return null;

  const remaining = Math.max(0, batch.plannedQuantity - batch.actualQuantity);

  const handleSubmit = (e) => {
    e.preventDefault();
    const printed = parseInt(printedQuantity, 10);
    const rejected = parseInt(rejectedQuantity, 10);

    if (isNaN(printed) || printed < 0) {
      setError('Printed quantity must be 0 or greater');
      return;
    }
    if (isNaN(rejected) || rejected < 0) {
      setError('Rejected quantity must be 0 or greater');
      return;
    }
    if (printed === 0 && rejected === 0) {
      setError('Either printed or rejected quantity must be greater than 0');
      return;
    }

    setError('');
    onSubmit({
      printedQuantity: printed,
      rejectedQuantity: rejected,
      remarks: remarks.trim(),
      evidenceFileId: evidenceFileId.trim() || null,
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Post Progress: Batch ${batch.batchNumber}`}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-sm text-rose-700 bg-rose-50 border border-rose-200 rounded-md">
            {error}
          </div>
        )}

        <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-sm grid grid-cols-2 gap-2">
          <div>
            <span className="text-slate-500">Planned:</span>{' '}
            <span className="font-semibold text-slate-800">{batch.plannedQuantity} cards</span>
          </div>
          <div>
            <span className="text-slate-500">Current Actual:</span>{' '}
            <span className="font-semibold text-slate-800">{batch.actualQuantity} cards</span>
          </div>
          <div className="col-span-2">
            <span className="text-slate-500">Remaining to print:</span>{' '}
            <span className="font-semibold text-blue-600">{remaining} cards</span>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Newly Printed Cards <span className="text-rose-500">*</span>
            </label>
            <Input
              type="number"
              min="0"
              value={printedQuantity}
              onChange={(e) => setPrintedQuantity(e.target.value)}
              placeholder="e.g. 50"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Damaged / Scrapped <span className="text-rose-500">*</span>
            </label>
            <Input
              type="number"
              min="0"
              value={rejectedQuantity}
              onChange={(e) => setRejectedQuantity(e.target.value)}
              placeholder="0"
              required
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Progress Remarks
          </label>
          <Textarea
            rows={2}
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
            placeholder="e.g. Machine calibration completed, printing run smooth"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Defect / Proof Image Path (Optional)
          </label>
          <Input
            type="text"
            value={evidenceFileId}
            onChange={(e) => setEvidenceFileId(e.target.value)}
            placeholder="e.g. uploads/defects/streak-evidence.jpg"
          />
        </div>

        <div className="flex justify-end space-x-3 pt-3 border-t border-slate-200">
          <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" disabled={isSubmitting}>
            {isSubmitting ? 'Posting Update...' : 'Submit Progress'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
