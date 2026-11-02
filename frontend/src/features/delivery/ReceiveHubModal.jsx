import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Textarea from '../../components/common/Textarea';
import Button from '../../components/common/Button';

export default function ReceiveHubModal({ isOpen, onClose, dispatchRecord, onSubmit, isSubmitting }) {
  const [receivedQuantity, setReceivedQuantity] = useState(1);
  const [conditionStatus, setConditionStatus] = useState('EXCELLENT');
  const [hubNotes, setHubNotes] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (dispatchRecord) {
      setReceivedQuantity(dispatchRecord.dispatchQuantity || 1);
    }
  }, [dispatchRecord]);

  if (!dispatchRecord) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    const qty = parseInt(receivedQuantity, 10);
    if (!qty || qty <= 0) {
      setError('Received quantity must be at least 1');
      return;
    }

    setError('');
    onSubmit({
      receivedQuantity: qty,
      conditionStatus,
      hubNotes: hubNotes.trim() || null,
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Receive at Central Hub: ${dispatchRecord.dispatchNumber}`}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-sm text-rose-700 bg-rose-50 border border-rose-200 rounded-md">
            {error}
          </div>
        )}

        <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs space-y-1">
          <div className="flex justify-between">
            <span className="text-slate-500">Consignment:</span>
            <span className="font-semibold text-slate-800 font-mono">{dispatchRecord.dispatchNumber}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-500">Courier / AWB:</span>
            <span className="font-semibold text-slate-800">{dispatchRecord.courierName} ({dispatchRecord.trackingNumber})</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-500">Agency Dispatched:</span>
            <span className="font-semibold text-indigo-600">{dispatchRecord.dispatchQuantity} cards</span>
          </div>
        </div>

        <Input
          label="Verified Received Quantity"
          type="number"
          required
          min="1"
          value={receivedQuantity}
          onChange={(e) => setReceivedQuantity(e.target.value)}
        />

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Consignment Outer Packaging Condition
          </label>
          <select
            className="w-full text-sm border border-slate-300 rounded-md py-2 px-3 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
            value={conditionStatus}
            onChange={(e) => setConditionStatus(e.target.value)}
          >
            <option value="EXCELLENT">Excellent - Completely Sealed & Protected</option>
            <option value="GOOD">Good - Intact with Normal Transit Wear</option>
            <option value="MINOR_BOX_DAMAGE">Minor Outer Box Wear (Cards Safe)</option>
            <option value="DAMAGED">Damaged Package - Further Inspection Required</option>
          </select>
        </div>

        <Textarea
          label="Hub Inspection Remarks"
          rows={3}
          value={hubNotes}
          onChange={(e) => setHubNotes(e.target.value)}
          placeholder="e.g. Received at Central Hub sorting desk. Barcodes scanned and ready for final distribution."
        />

        <div className="flex justify-end space-x-2 pt-2 border-t border-slate-200">
          <Button variant="outline" type="button" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" className="bg-amber-600 hover:bg-amber-700 text-white" disabled={isSubmitting}>
            {isSubmitting ? 'Processing...' : 'Mark Received at Hub'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
