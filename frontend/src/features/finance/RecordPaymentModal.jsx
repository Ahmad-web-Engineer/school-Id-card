import React, { useState } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Textarea from '../../components/common/Textarea';
import Button from '../../components/common/Button';
import { PAYMENT_METHODS } from './financeConstants';
import api from '../../services/api';

export default function RecordPaymentModal({ isOpen, onClose, orderId, orderNumber, dueAmount, role = 'SCHOOL', onPaymentRecorded }) {
  const [amount, setAmount] = useState(dueAmount ? String(dueAmount) : '');
  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [transactionReference, setTransactionReference] = useState('');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    if (!amount || parseFloat(amount) <= 0) {
      setError('Please enter a valid payment amount greater than zero');
      return;
    }

    try {
      setLoading(true);
      const endpoint = role === 'SUPER_ADMIN'
        ? `/admin/finance/orders/${orderId}/payments`
        : `/school/finance/orders/${orderId}/payments`;

      await api.post(endpoint, {
        amount: parseFloat(amount),
        paymentMethod,
        transactionReference: transactionReference || undefined,
        notes: notes || undefined,
      });

      if (onPaymentRecorded) {
        onPaymentRecorded();
      }
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to record payment');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Record Payment for ${orderNumber || 'Order'}`}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-sm text-red-700 bg-red-50 border border-red-200 rounded-md">
            {error}
          </div>
        )}

        <Input
          label="Amount (₹)"
          type="number"
          step="0.01"
          required
          min="0.01"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="e.g. 5000.00"
          helperText={dueAmount ? `Outstanding balance: ₹${dueAmount}` : undefined}
        />

        <Select
          label="Payment Method"
          value={paymentMethod}
          onChange={(e) => setPaymentMethod(e.target.value)}
          options={PAYMENT_METHODS}
          required
        />

        <Input
          label="Transaction / Reference ID"
          type="text"
          value={transactionReference}
          onChange={(e) => setTransactionReference(e.target.value)}
          placeholder="e.g. UPI/2026/83921 or NEFT-48201"
        />

        <Textarea
          label="Notes (Optional)"
          rows={3}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="Enter payment remarks, cheque number, or bank details..."
        />

        <div className="flex justify-end space-x-3 pt-3">
          <Button variant="secondary" onClick={onClose} type="button" disabled={loading}>
            Cancel
          </Button>
          <Button variant="primary" type="submit" loading={loading}>
            Record Payment
          </Button>
        </div>
      </form>
    </Modal>
  );
}
