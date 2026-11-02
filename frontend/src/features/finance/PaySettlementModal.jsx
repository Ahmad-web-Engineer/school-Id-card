import React, { useState } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Textarea from '../../components/common/Textarea';
import Button from '../../components/common/Button';
import api from '../../services/api';

export default function PaySettlementModal({ isOpen, onClose, settlementId, settlementNumber, payableAmount, outstandingAmount, onSettlementPaid }) {
  const [amount, setAmount] = useState(outstandingAmount ? String(outstandingAmount) : '');
  const [paymentReference, setPaymentReference] = useState('');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    if (!amount || parseFloat(amount) <= 0) {
      setError('Please enter a valid payout amount');
      return;
    }

    try {
      setLoading(true);
      await api.post(`/admin/finance/settlements/${settlementId}/pay`, {
        amount: parseFloat(amount),
        paymentReference: paymentReference || undefined,
        notes: notes || undefined,
      });

      if (onSettlementPaid) {
        onSettlementPaid();
      }
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to record settlement payout');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Disburse Payout for ${settlementNumber || 'Settlement'}`}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-sm text-red-700 bg-red-50 border border-red-200 rounded-md">
            {error}
          </div>
        )}

        <Input
          label="Payout Amount (₹)"
          type="number"
          step="0.01"
          required
          min="0.01"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="e.g. 15000.00"
          helperText={`Net payable: ₹${payableAmount || '0.00'} | Outstanding: ₹${outstandingAmount || '0.00'}`}
        />

        <Input
          label="Payment Reference / UTR Number"
          type="text"
          value={paymentReference}
          onChange={(e) => setPaymentReference(e.target.value)}
          placeholder="e.g. UTR-HDFC-991823901"
          required
        />

        <Textarea
          label="Disbursement Notes"
          rows={3}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="e.g. Transferred via Corporate NEFT to agency registered bank account."
        />

        <div className="flex justify-end space-x-3 pt-3">
          <Button variant="secondary" onClick={onClose} type="button" disabled={loading}>
            Cancel
          </Button>
          <Button variant="primary" type="submit" loading={loading}>
            Confirm Payout
          </Button>
        </div>
      </form>
    </Modal>
  );
}
