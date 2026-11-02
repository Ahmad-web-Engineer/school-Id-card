import React, { useState } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Textarea from '../../components/common/Textarea';
import Button from '../../components/common/Button';
import api from '../../services/api';

export default function GenerateInvoiceModal({ isOpen, onClose, orderId, orderNumber, onInvoiceGenerated }) {
  const [dueDate, setDueDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 30);
    return d.toISOString().split('T')[0];
  });
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    try {
      setLoading(true);
      await api.post(`/admin/finance/orders/${orderId}/invoices`, {
        dueDate: dueDate || undefined,
        notes: notes || undefined,
      });

      if (onInvoiceGenerated) {
        onInvoiceGenerated();
      }
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to generate tax invoice');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Generate Tax Invoice for ${orderNumber || 'Order'}`}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-sm text-red-700 bg-red-50 border border-red-200 rounded-md">
            {error}
          </div>
        )}

        <Input
          label="Due Date"
          type="date"
          value={dueDate}
          onChange={(e) => setDueDate(e.target.value)}
          helperText="Payment due date for the school"
        />

        <Textarea
          label="Invoice Notes / Terms"
          rows={3}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="e.g. Terms: 30 days credit, 18% GST included as per approved quotation."
        />

        <div className="flex justify-end space-x-3 pt-3">
          <Button variant="secondary" onClick={onClose} type="button" disabled={loading}>
            Cancel
          </Button>
          <Button variant="primary" type="submit" loading={loading}>
            Generate Invoice
          </Button>
        </div>
      </form>
    </Modal>
  );
}
