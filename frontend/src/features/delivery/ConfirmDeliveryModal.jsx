import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Textarea from '../../components/common/Textarea';
import Button from '../../components/common/Button';

export default function ConfirmDeliveryModal({ isOpen, onClose, order, onSubmit, isSubmitting }) {
  const [receivedQuantity, setReceivedQuantity] = useState(1);
  const [receivedBy, setReceivedBy] = useState('');
  const [receivedMobile, setReceivedMobile] = useState('');
  const [deliveryNotes, setDeliveryNotes] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (order) {
      setReceivedQuantity(order.totalStudents || order.totalQuantity || 1);
      setReceivedBy(order.contactPerson || '');
      setReceivedMobile(order.contactPhone || order.contactMobile || '');
    }
  }, [order]);

  if (!order) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!receivedBy.trim()) {
      setError('Receiver name is required');
      return;
    }
    const qty = parseInt(receivedQuantity, 10);
    if (!qty || qty <= 0) {
      setError('Received quantity must be at least 1');
      return;
    }

    setError('');
    onSubmit({
      receivedQuantity: qty,
      receivedBy: receivedBy.trim(),
      receivedMobile: receivedMobile.trim() || null,
      deliveryNotes: deliveryNotes.trim() || null,
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Confirm Receipt: ${order.orderNumber}`}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-sm text-rose-700 bg-rose-50 border border-rose-200 rounded-md">
            {error}
          </div>
        )}

        <div className="p-3 bg-indigo-50/50 border border-indigo-100 rounded-lg text-xs space-y-1">
          <p className="font-semibold text-slate-900">Acknowledge Shipment Receipt</p>
          <p className="text-slate-600">
            Please verify package contents and confirm the total number of student ID cards received in good condition.
          </p>
        </div>

        <Input
          label="Received Cards Quantity"
          type="number"
          required
          min="1"
          value={receivedQuantity}
          onChange={(e) => setReceivedQuantity(e.target.value)}
        />

        <Input
          label="Received By (Staff / Authority Name)"
          required
          value={receivedBy}
          onChange={(e) => setReceivedBy(e.target.value)}
          placeholder="e.g. Dr. Rajesh Sharma (Principal)"
        />

        <Input
          label="Contact Mobile Number"
          value={receivedMobile}
          onChange={(e) => setReceivedMobile(e.target.value)}
          placeholder="e.g. +91 98765 43210"
        />

        <Textarea
          label="Receipt Remarks / Condition Feedback"
          rows={3}
          value={deliveryNotes}
          onChange={(e) => setDeliveryNotes(e.target.value)}
          placeholder="e.g. Consignment verified with class teachers. All cards received in perfect condition."
        />

        <div className="flex justify-end space-x-2 pt-2 border-t border-slate-200">
          <Button variant="outline" type="button" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" className="bg-emerald-600 hover:bg-emerald-700 text-white" disabled={isSubmitting}>
            {isSubmitting ? 'Confirming...' : 'Acknowledge & Confirm Receipt'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
