import React, { useState, useEffect } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Textarea from '../../components/common/Textarea';
import Button from '../../components/common/Button';
import { POPULAR_COURIERS } from './deliveryConstants';

export default function SecondaryDispatchModal({ isOpen, onClose, order, onSubmit, isSubmitting }) {
  const [courierName, setCourierName] = useState(POPULAR_COURIERS[0]);
  const [customCourier, setCustomCourier] = useState('');
  const [trackingNumber, setTrackingNumber] = useState('');
  const [dispatchQuantity, setDispatchQuantity] = useState(1);
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (order) {
      setDispatchQuantity(order.totalStudents || order.totalQuantity || 1);
    }
  }, [order]);

  if (!order) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    const finalCourier = courierName === 'Other' ? customCourier.trim() : courierName;
    if (!finalCourier) {
      setError('Courier service provider is required');
      return;
    }
    if (!trackingNumber.trim()) {
      setError('Tracking number / AWB is required');
      return;
    }
    const qty = parseInt(dispatchQuantity, 10);
    if (!qty || qty <= 0) {
      setError('Dispatch quantity must be at least 1');
      return;
    }

    setError('');
    onSubmit({
      courierName: finalCourier,
      trackingNumber: trackingNumber.trim(),
      dispatchQuantity: qty,
      notes: notes.trim() || null,
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Secondary Hub Dispatch: ${order.orderNumber}`}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-sm text-rose-700 bg-rose-50 border border-rose-200 rounded-md">
            {error}
          </div>
        )}

        <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs space-y-1">
          <p className="font-semibold text-slate-800">Model B Logistics: Central Hub to School</p>
          <p className="text-slate-500">
            Destination: {order.deliveryAddress || order.schoolName || 'School premises'}
          </p>
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">
            Courier Service Provider <span className="text-rose-500">*</span>
          </label>
          <select
            className="w-full text-sm border border-slate-300 rounded-md py-2 px-3 bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500"
            value={courierName}
            onChange={(e) => setCourierName(e.target.value)}
          >
            {POPULAR_COURIERS.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>

        {courierName === 'Other' && (
          <Input
            label="Custom Courier Name"
            required
            value={customCourier}
            onChange={(e) => setCustomCourier(e.target.value)}
            placeholder="e.g. Dedicated Van Service"
          />
        )}

        <Input
          label="Courier Tracking Number / AWB"
          required
          value={trackingNumber}
          onChange={(e) => setTrackingNumber(e.target.value)}
          placeholder="e.g. DELH-HUB-992381"
        />

        <Input
          label="Dispatch Quantity (Cards)"
          type="number"
          required
          min="1"
          value={dispatchQuantity}
          onChange={(e) => setDispatchQuantity(e.target.value)}
        />

        <Textarea
          label="Hub Dispatch Notes"
          rows={3}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="e.g. Dispatched directly to school principal reception with sealed tamper-evident security tape."
        />

        <div className="flex justify-end space-x-2 pt-2 border-t border-slate-200">
          <Button variant="outline" type="button" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Dispatching...' : 'Dispatch to School'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
