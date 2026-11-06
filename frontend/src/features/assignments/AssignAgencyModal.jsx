import React, { useState, useEffect } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import api from '../../services/api';
import { extractArray } from '../../services/responseUtils';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Card from '../../components/common/Card';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { Plus, Trash2, Building2, Calendar, DollarSign, AlertCircle, CheckCircle2 } from 'lucide-react';

export default function AssignAgencyModal({ isOpen, onClose, order, onSuccess }) {
  const [assignments, setAssignments] = useState([]);
  const [error, setError] = useState(null);

  // 1. Fetch available agencies
  const { data: agenciesRes, isLoading: isLoadingAgencies } = useQuery({
    queryKey: ['admin-agencies-list'],
    queryFn: () => api.get('/admin/agencies', { params: { page: 0, size: 200 } }),
    enabled: isOpen,
  });

  const agencies = extractArray(agenciesRes);

  // Initialize with single assignment row on open
  useEffect(() => {
    if (isOpen && order?.items) {
      setError(null);
      const defaultDelivery = new Date();
      defaultDelivery.setDate(defaultDelivery.getDate() + 7);
      const deliveryStr = defaultDelivery.toISOString().split('T')[0];

      setAssignments([
        {
          agencyId: '',
          expectedDeliveryDate: deliveryStr,
          agreedRate: '25.00',
          notes: '',
          allowAgencyOverride: false,
          jobItems: order.items.map((item) => ({
            orderItemId: item.id,
            productType: item.productType,
            totalAvailable: item.quantity,
            assignedQuantity: item.quantity,
          })),
        },
      ]);
    }
  }, [isOpen, order]);

  // Add another agency assignment (for splitting)
  const handleAddAgency = () => {
    if (!order?.items) return;
    const defaultDelivery = new Date();
    defaultDelivery.setDate(defaultDelivery.getDate() + 7);

    setAssignments((prev) => [
      ...prev,
      {
        agencyId: '',
        expectedDeliveryDate: defaultDelivery.toISOString().split('T')[0],
        agreedRate: '25.00',
        notes: '',
        allowAgencyOverride: false,
        jobItems: order.items.map((item) => ({
          orderItemId: item.id,
          productType: item.productType,
          totalAvailable: item.quantity,
          assignedQuantity: 0,
        })),
      },
    ]);
  };

  const handleRemoveAgency = (index) => {
    setAssignments((prev) => prev.filter((_, idx) => idx !== index));
  };

  const handleUpdateAssignment = (index, field, value) => {
    setAssignments((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [field]: value };
      return copy;
    });
  };

  const handleUpdateJobItemQty = (agencyIndex, itemIndex, qty) => {
    const parsed = parseInt(qty, 10);
    const validQty = isNaN(parsed) || parsed < 0 ? 0 : parsed;

    setAssignments((prev) => {
      const copy = [...prev];
      const itemsCopy = [...copy[agencyIndex].jobItems];
      itemsCopy[itemIndex] = {
        ...itemsCopy[itemIndex],
        assignedQuantity: validQty,
      };
      copy[agencyIndex] = { ...copy[agencyIndex], jobItems: itemsCopy };
      return copy;
    });
  };

  // Submit Mutation
  const assignMutation = useMutation({
    mutationFn: (payload) => api.post(`/admin/orders/${order.id}/assignments`, payload),
    onSuccess: (res) => {
      if (onSuccess) onSuccess(res.data);
      onClose();
    },
    onError: (err) => {
      setError(err.message || 'Failed to assign order to agencies.');
    },
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    setError(null);

    // Basic Validations
    for (let i = 0; i < assignments.length; i++) {
      const a = assignments[i];
      if (!a.agencyId) {
        setError(`Please select an agency for Assignment #${i + 1}.`);
        return;
      }
      const rate = parseFloat(a.agreedRate);
      if (isNaN(rate) || rate < 0) {
        setError(`Please enter a valid agreed rate for Assignment #${i + 1}.`);
        return;
      }
      const totalQty = a.jobItems.reduce((acc, curr) => acc + curr.assignedQuantity, 0);
      if (totalQty === 0) {
        setError(`Assignment #${i + 1} must have at least one assigned item.`);
        return;
      }
    }

    // Check total allocated per order item doesn't exceed order item quantity
    if (order?.items) {
      for (const item of order.items) {
        let totalAllocated = 0;
        for (const a of assignments) {
          const match = a.jobItems.find((ji) => ji.orderItemId === item.id);
          if (match) totalAllocated += match.assignedQuantity;
        }
        if (totalAllocated > item.quantity) {
          setError(
            `Total allocated quantity (${totalAllocated}) exceeds order quantity (${item.quantity}) for ${item.productType}.`
          );
          return;
        }
      }
    }

    const payload = {
      assignments: assignments.map((a) => ({
        agencyId: a.agencyId,
        expectedDeliveryDate: a.expectedDeliveryDate || undefined,
        agreedRate: parseFloat(a.agreedRate),
        notes: a.notes.trim() || undefined,
        allowAgencyOverride: !!a.allowAgencyOverride,
        jobItems: a.jobItems
          .filter((ji) => ji.assignedQuantity > 0)
          .map((ji) => ({
            orderItemId: ji.orderItemId,
            assignedQuantity: ji.assignedQuantity,
          })),
      })),
    };

    assignMutation.mutate(payload);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Assign Order to Printing Agencies"
      size="xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg flex items-center gap-2 text-xs text-rose-700">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Order Summary Header */}
        <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 flex flex-wrap items-center justify-between gap-3 text-xs">
          <div>
            <span className="text-slate-500">Order:</span>{' '}
            <strong className="font-mono text-indigo-700">{order?.orderNumber}</strong>
          </div>
          <div>
            <span className="text-slate-500">School:</span>{' '}
            <strong className="text-slate-800">{order?.schoolName}</strong>
          </div>
          <div>
            <span className="text-slate-500">Total Cards:</span>{' '}
            <strong className="text-slate-900">{order?.totalStudents} cards</strong>
          </div>
          <div>
            <span className="text-slate-500">Total Items:</span>{' '}
            <strong className="text-slate-900">{order?.totalQuantity} items</strong>
          </div>
        </div>

        {/* Allocation breakdown per agency */}
        <div className="space-y-4 max-h-[55vh] overflow-y-auto pr-1">
          {assignments.map((assignment, aIdx) => (
            <Card key={aIdx} className="p-4 border-slate-300 shadow-sm relative">
              <div className="flex items-center justify-between mb-3 border-b border-slate-100 pb-2">
                <h4 className="text-xs font-bold text-slate-800 flex items-center gap-2">
                  <Building2 className="h-4 w-4 text-indigo-600" />
                  Agency Assignment #{aIdx + 1}
                </h4>
                {assignments.length > 1 && (
                  <Button
                    type="button"
                    size="xs"
                    variant="ghost"
                    onClick={() => handleRemoveAgency(aIdx)}
                    className="text-rose-600 hover:text-rose-700 hover:bg-rose-50"
                  >
                    <Trash2 className="h-3.5 w-3.5 mr-1" />
                    Remove
                  </Button>
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-3">
                {/* Agency Selection */}
                <div>
                  <label className="block text-[11px] font-medium text-slate-600 mb-1">
                    Select Agency <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={assignment.agencyId}
                    onChange={(e) => handleUpdateAssignment(aIdx, 'agencyId', e.target.value)}
                    className="w-full text-xs px-2.5 py-1.5 rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    required
                  >
                    <option value="">-- Choose Agency --</option>
                    {agencies.map((agency) => (
                      <option key={agency.id} value={agency.id}>
                        {agency.name} (Cap: {agency.capacityPerDay || 'N/A'}/day)
                      </option>
                    ))}
                  </select>
                </div>

                {/* Agreed Rate */}
                <div>
                  <label className="block text-[11px] font-medium text-slate-600 mb-1">
                    Agreed Purchase Rate (₹) <span className="text-rose-500">*</span>
                  </label>
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    value={assignment.agreedRate}
                    onChange={(e) => handleUpdateAssignment(aIdx, 'agreedRate', e.target.value)}
                    placeholder="25.00"
                    required
                  />
                </div>

                {/* Expected Delivery Date */}
                <div>
                  <label className="block text-[11px] font-medium text-slate-600 mb-1">
                    Expected Delivery Date
                  </label>
                  <Input
                    type="date"
                    value={assignment.expectedDeliveryDate}
                    onChange={(e) =>
                      handleUpdateAssignment(aIdx, 'expectedDeliveryDate', e.target.value)
                    }
                  />
                </div>
              </div>

              {/* Quantity Splitting per Order Item */}
              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                <p className="text-[11px] font-semibold text-slate-700 uppercase tracking-wider mb-2">
                  Allocate Quantities
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {assignment.jobItems.map((jobItem, jiIdx) => (
                    <div
                      key={jiIdx}
                      className="flex items-center justify-between p-2 rounded bg-white border border-slate-200 text-xs"
                    >
                      <div>
                        <span className="font-medium text-slate-800">{jobItem.productType}</span>
                        <span className="text-[10px] text-slate-500 block">
                          Total ordered: {jobItem.totalAvailable}
                        </span>
                      </div>
                      <div className="flex items-center gap-1.5 w-28">
                        <Input
                          type="number"
                          min="0"
                          max={jobItem.totalAvailable}
                          value={jobItem.assignedQuantity}
                          onChange={(e) => handleUpdateJobItemQty(aIdx, jiIdx, e.target.value)}
                          className="text-right font-mono"
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Notes */}
              <div className="mt-3">
                <label className="block text-[11px] font-medium text-slate-600 mb-1">
                  Internal Instructions / Agency Notes
                </label>
                <input
                  type="text"
                  placeholder="e.g. Expedited batch for seniors"
                  value={assignment.notes}
                  onChange={(e) => handleUpdateAssignment(aIdx, 'notes', e.target.value)}
                  className="w-full text-xs px-2.5 py-1.5 rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              {/* Prepress Permissions */}
              <div className="mt-3 p-2.5 bg-indigo-50/50 rounded-lg border border-indigo-100 flex items-center justify-between">
                <div>
                  <span className="text-xs font-semibold text-slate-800 block">
                    Allow Agency to Customize Print Layout
                  </span>
                  <span className="text-[11px] text-slate-500 block">
                    If unchecked, agency is strictly locked to Admin's imposition layout specifications.
                  </span>
                </div>
                <label className="flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    checked={assignment.allowAgencyOverride || false}
                    onChange={(e) => handleUpdateAssignment(aIdx, 'allowAgencyOverride', e.target.checked)}
                    className="w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500 border-slate-300"
                  />
                </label>
              </div>
            </Card>
          ))}
        </div>

        {/* Action button to split order across another agency */}
        <div className="flex justify-between items-center pt-2 border-t border-slate-200">
          <Button
            type="button"
            size="xs"
            variant="outline"
            onClick={handleAddAgency}
            disabled={isLoadingAgencies}
          >
            <Plus className="h-3.5 w-3.5 mr-1" />
            Split to Another Agency
          </Button>

          <div className="flex items-center gap-2">
            <Button type="button" size="sm" variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button
              type="submit"
              size="sm"
              disabled={assignMutation.isPending || isLoadingAgencies}
            >
              {assignMutation.isPending ? (
                <>
                  <LoadingSpinner size="sm" className="mr-1.5" />
                  Assigning...
                </>
              ) : (
                <>
                  <CheckCircle2 className="h-4 w-4 mr-1.5" />
                  Confirm Assignment
                </>
              )}
            </Button>
          </div>
        </div>
      </form>
    </Modal>
  );
}
