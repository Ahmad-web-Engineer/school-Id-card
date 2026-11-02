import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import Modal from '../../components/common/Modal';
import { ORDER_STATUSES } from '../../features/orders/ordersConstants';
import {
  ArrowLeft,
  ShoppingBag,
  Send,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Layers,
  Users,
  Package,
  Truck,
  FileCheck,
  Receipt,
  FileText,
  Search,
  Check
} from 'lucide-react';

const ORDER_STEPS = [
  { key: 'DRAFT', label: 'Draft' },
  { key: 'SUBMITTED', label: 'Submitted' },
  { key: 'UNDER_REVIEW', label: 'Under Review' },
  { key: 'VERIFIED', label: 'Verified' },
  { key: 'QUOTATION_GENERATED', label: 'Quotation Ready' },
  { key: 'APPROVED', label: 'Approved' },
];

function getStepIndex(status) {
  if (status === 'CORRECTION_REQUIRED') return 2; // under review phase
  const idx = ORDER_STEPS.findIndex((s) => s.key === status);
  return idx >= 0 ? idx : 0;
}

export default function OrderDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [studentSearch, setStudentSearch] = useState('');
  const [isApproveModalOpen, setIsApproveModalOpen] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // Fetch Order Details
  const {
    data: orderRes,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['school-order-detail', id],
    queryFn: () => api.get(`/school/orders/${id}`),
    enabled: !!id,
  });

  // Submit Draft Order mutation
  const submitMutation = useMutation({
    mutationFn: () => api.post(`/school/orders/${id}/submit`),
    onSuccess: () => {
      setActionSuccess('Order successfully submitted for verification.');
      queryClient.invalidateQueries({ queryKey: ['school-order-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['school-orders'] });
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to submit order.');
    },
  });

  // Approve Quotation mutation
  const approveQuotationMutation = useMutation({
    mutationFn: () => api.post(`/school/orders/${id}/approve-quotation`),
    onSuccess: () => {
      setActionSuccess('Quotation approved successfully! Your order has moved to production queue.');
      setIsApproveModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['school-order-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['school-orders'] });
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to approve quotation.');
    },
  });

  const order = orderRes?.data;

  if (isLoading) {
    return (
      <div className="py-24 flex justify-center">
        <LoadingSpinner size="lg" className="text-indigo-600" />
      </div>
    );
  }

  if (isError || !order) {
    return (
      <ErrorState
        title="Order Not Found"
        message={error?.message || 'Could not find the requested order or access was denied.'}
        onRetry={refetch}
      />
    );
  }

  const statusConfig = ORDER_STATUSES[order.orderStatus] || {
    label: order.orderStatus,
    variant: 'neutral',
  };

  const currentStepIdx = getStepIndex(order.orderStatus);

  // Filter snapshot students
  const filteredStudents = (order.students || []).filter((s) => {
    if (!studentSearch) return true;
    const q = studentSearch.toLowerCase();
    return (
      s.studentName?.toLowerCase().includes(q) ||
      s.admissionNumber?.toLowerCase().includes(q) ||
      s.standard?.toLowerCase().includes(q)
    );
  });

  const quotation = order.quotation;

  return (
    <div className="space-y-6">
      {/* Top Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/school/orders')}
            className="p-1.5"
            aria-label="Back to orders"
          >
            <ArrowLeft className="h-5 w-5 text-slate-500" />
          </Button>
          <div>
            <div className="flex items-center space-x-2">
              <h1 className="text-xl font-bold text-slate-900 font-mono">{order.orderNumber}</h1>
              <Badge variant={statusConfig.variant} size="sm">
                {statusConfig.label}
              </Badge>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Created on{' '}
              {new Date(order.createdAt).toLocaleDateString('en-IN', {
                day: '2-digit',
                month: 'long',
                year: 'numeric',
              })}
            </p>
          </div>
        </div>

        {/* Header Actions */}
        <div className="flex items-center space-x-2">
          {order.orderStatus === 'DRAFT' && (
            <Button
              size="sm"
              onClick={() => submitMutation.mutate()}
              disabled={submitMutation.isPending}
            >
              {submitMutation.isPending ? <LoadingSpinner size="sm" className="mr-1" /> : <Send className="h-4 w-4 mr-1.5" />}
              Submit Order
            </Button>
          )}

          {order.orderStatus === 'QUOTATION_GENERATED' && (
            <Button
              size="sm"
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
              onClick={() => setIsApproveModalOpen(true)}
            >
              <FileCheck className="h-4 w-4 mr-1.5" />
              Approve Quotation
            </Button>
          )}
        </div>
      </div>

      {actionSuccess && (
        <div className="rounded-lg bg-emerald-50 border border-emerald-200 p-3 flex items-center justify-between text-xs text-emerald-800">
          <div className="flex items-center space-x-2">
            <CheckCircle2 className="h-4 w-4 text-emerald-600" />
            <span>{actionSuccess}</span>
          </div>
          <button onClick={() => setActionSuccess(null)} className="font-semibold underline ml-2">Dismiss</button>
        </div>
      )}

      {actionError && (
        <div className="rounded-lg bg-rose-50 border border-rose-200 p-3 flex items-center justify-between text-xs text-rose-800">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="h-4 w-4 text-rose-600" />
            <span>{actionError}</span>
          </div>
          <button onClick={() => setActionError(null)} className="font-semibold underline ml-2">Dismiss</button>
        </div>
      )}

      {/* Correction Required Alert */}
      {order.orderStatus === 'CORRECTION_REQUIRED' && (
        <div className="rounded-lg bg-rose-50 border border-rose-200 p-4">
          <div className="flex items-start space-x-3">
            <AlertTriangle className="h-5 w-5 text-rose-600 mt-0.5 flex-shrink-0" />
            <div className="space-y-1">
              <h4 className="text-sm font-semibold text-rose-900">Correction Requested by Printing Team</h4>
              <p className="text-xs text-rose-800">
                {order.rejectionRemarks || 'Please review student information or photo requirements and contact the administrator.'}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Status Workflow Progress Stepper */}
      <Card className="p-4 sm:p-6">
        <div className="flex items-center justify-between overflow-x-auto pb-2">
          {ORDER_STEPS.map((step, idx) => {
            const isCompleted = idx < currentStepIdx;
            const isCurrent = idx === currentStepIdx;
            return (
              <div key={step.key} className="flex-1 min-w-[100px] text-center relative">
                {/* Connecting Line */}
                {idx > 0 && (
                  <div
                    className={`absolute top-3.5 -left-1/2 w-full h-0.5 -z-0 ${
                      idx <= currentStepIdx ? 'bg-indigo-600' : 'bg-slate-200'
                    }`}
                  />
                )}
                {/* Step Circle */}
                <div
                  className={`relative z-10 w-7 h-7 mx-auto rounded-full flex items-center justify-center text-xs font-semibold ${
                    isCompleted
                      ? 'bg-indigo-600 text-white'
                      : isCurrent
                      ? 'bg-indigo-600 text-white ring-4 ring-indigo-100'
                      : 'bg-slate-100 text-slate-400'
                  }`}
                >
                  {isCompleted ? <Check className="h-4 w-4" /> : idx + 1}
                </div>
                <p
                  className={`mt-2 text-[11px] font-medium ${
                    isCurrent ? 'text-indigo-900 font-semibold' : 'text-slate-500'
                  }`}
                >
                  {step.label}
                </p>
              </div>
            );
          })}
        </div>
      </Card>

      {/* Quotation Review Card (if Quotation is generated or approved) */}
      {quotation && (
        <Card className="p-5 border-amber-200 bg-amber-50/20">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-amber-100 gap-3">
            <div className="flex items-center space-x-2">
              <Receipt className="h-5 w-5 text-amber-600" />
              <div>
                <h3 className="text-sm font-bold text-slate-900">
                  Quotation: <span className="font-mono text-amber-700">{quotation.quotationNumber}</span>
                </h3>
                <p className="text-[11px] text-slate-500">
                  Generated on{' '}
                  {new Date(quotation.createdAt).toLocaleDateString('en-IN', {
                    day: '2-digit',
                    month: 'short',
                    year: 'numeric',
                  })}
                </p>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              <Badge
                variant={quotation.quotationStatus === 'ACCEPTED' ? 'success' : 'warning'}
                size="sm"
              >
                {quotation.quotationStatus === 'ACCEPTED' ? 'Quotation Approved' : 'Pending Approval'}
              </Badge>
              {order.orderStatus === 'QUOTATION_GENERATED' && (
                <Button
                  size="xs"
                  className="bg-emerald-600 hover:bg-emerald-700 text-white"
                  onClick={() => setIsApproveModalOpen(true)}
                >
                  <FileCheck className="h-3.5 w-3.5 mr-1" />
                  Approve Quotation
                </Button>
              )}
            </div>
          </div>

          {/* Quotation Line Items Table */}
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-xs divide-y divide-slate-200">
              <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider">
                <tr>
                  <th className="px-3 py-2 text-left">Description</th>
                  <th className="px-3 py-2 text-right">Quantity</th>
                  <th className="px-3 py-2 text-right">Unit Price</th>
                  <th className="px-3 py-2 text-right">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {quotation.items.map((item, idx) => (
                  <tr key={item.id || idx}>
                    <td className="px-3 py-2 text-slate-800 font-medium">{item.itemDescription}</td>
                    <td className="px-3 py-2 text-right text-slate-600">{item.quantity}</td>
                    <td className="px-3 py-2 text-right text-slate-600 font-mono">₹{Number(item.unitPrice).toFixed(2)}</td>
                    <td className="px-3 py-2 text-right text-slate-900 font-semibold font-mono">₹{Number(item.totalPrice).toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pricing Totals Breakdown */}
          <div className="mt-4 pt-3 border-t border-slate-200 flex flex-col sm:flex-row justify-between items-start sm:items-end gap-3 text-xs">
            <div className="text-slate-500 max-w-sm">
              {quotation.notes && (
                <p>
                  <span className="font-semibold text-slate-700">Quotation Notes:</span> {quotation.notes}
                </p>
              )}
            </div>

            <div className="w-full sm:w-64 space-y-1 bg-white p-3 rounded-lg border border-slate-200 font-medium">
              <div className="flex justify-between text-slate-600">
                <span>Subtotal:</span>
                <span className="font-mono">₹{Number(quotation.subtotalAmount).toFixed(2)}</span>
              </div>
              {Number(quotation.discountAmount) > 0 && (
                <div className="flex justify-between text-emerald-600">
                  <span>Discount:</span>
                  <span className="font-mono">- ₹{Number(quotation.discountAmount).toFixed(2)}</span>
                </div>
              )}
              <div className="flex justify-between text-slate-600">
                <span>GST (18%):</span>
                <span className="font-mono">₹{Number(quotation.taxAmount).toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-slate-900 font-bold text-sm pt-2 border-t border-slate-200">
                <span>Total Amount:</span>
                <span className="font-mono text-indigo-700">₹{Number(quotation.totalAmount).toFixed(2)}</span>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Order Details & Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Template & Items Summary */}
        <Card className="p-4 space-y-3">
          <div className="flex items-center space-x-2 text-slate-900 font-semibold text-sm">
            <Layers className="h-4 w-4 text-indigo-600" />
            <h4>Template & Quantity</h4>
          </div>
          <div className="space-y-2 text-xs text-slate-600">
            <div className="flex justify-between">
              <span className="text-slate-500">Template:</span>
              <span className="font-semibold text-slate-800">{order.templateName}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Total Cards:</span>
              <span className="font-semibold text-slate-800">{order.totalStudents}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Total Accessories & Items:</span>
              <span className="font-semibold text-slate-800">{order.totalQuantity} items</span>
            </div>
          </div>

          <div className="pt-2 border-t border-slate-100">
            <h5 className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-2">Itemized Breakdown</h5>
            <div className="space-y-1.5 text-xs">
              {order.items?.map((item, idx) => (
                <div key={idx} className="flex justify-between text-slate-700">
                  <span className="truncate max-w-[180px]">{item.itemDescription}</span>
                  <span className="font-medium font-mono">× {item.quantity}</span>
                </div>
              ))}
            </div>
          </div>
        </Card>

        {/* Delivery Details */}
        <Card className="p-4 space-y-3">
          <div className="flex items-center space-x-2 text-slate-900 font-semibold text-sm">
            <Truck className="h-4 w-4 text-indigo-600" />
            <h4>Delivery Preferences</h4>
          </div>
          <div className="space-y-2 text-xs text-slate-600">
            <div>
              <span className="text-slate-500 block text-[11px]">Contact Person</span>
              <span className="font-medium text-slate-800">{order.contactPerson || '-'}</span>
            </div>
            <div>
              <span className="text-slate-500 block text-[11px]">Contact Phone</span>
              <span className="font-medium text-slate-800 font-mono">{order.contactPhone || '-'}</span>
            </div>
            <div>
              <span className="text-slate-500 block text-[11px]">Delivery Address</span>
              <span className="font-medium text-slate-800">{order.deliveryAddress || '-'}</span>
            </div>
          </div>
        </Card>

        {/* Order Notes */}
        <Card className="p-4 space-y-3">
          <div className="flex items-center space-x-2 text-slate-900 font-semibold text-sm">
            <FileText className="h-4 w-4 text-indigo-600" />
            <h4>Special Instructions</h4>
          </div>
          <p className="text-xs text-slate-600 leading-relaxed bg-slate-50 p-3 rounded-lg border border-slate-100">
            {order.notes || 'No special notes provided for this order.'}
          </p>
        </Card>
      </div>

      {/* Frozen Student Snapshots Roster */}
      <Card className="p-4 sm:p-5 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center space-x-2">
            <Users className="h-5 w-5 text-indigo-600" />
            <div>
              <h3 className="text-sm font-bold text-slate-900">
                Student Roster Snapshot ({order.students?.length || 0} Students)
              </h3>
              <p className="text-[11px] text-slate-500">
                Immutable snapshots captured at the time of order placement.
              </p>
            </div>
          </div>

          <div className="relative w-full sm:w-64">
            <Search className="absolute left-3 top-2.5 h-3.5 w-3.5 text-slate-400" />
            <input
              type="text"
              placeholder="Search in roster..."
              value={studentSearch}
              onChange={(e) => setStudentSearch(e.target.value)}
              className="w-full pl-8 pr-3 py-1.5 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        </div>

        <div className="overflow-x-auto rounded-lg border border-slate-200">
          <table className="min-w-full text-xs divide-y divide-slate-100">
            <thead className="bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider">
              <tr>
                <th className="px-3 py-2.5 text-left">#</th>
                <th className="px-3 py-2.5 text-left">Student Name</th>
                <th className="px-3 py-2.5 text-left">Admission No</th>
                <th className="px-3 py-2.5 text-left">Class & Sec</th>
                <th className="px-3 py-2.5 text-left">Roll No</th>
                <th className="px-3 py-2.5 text-left">Blood Group</th>
                <th className="px-3 py-2.5 text-left">Emergency Contact</th>
                <th className="px-3 py-2.5 text-left">Photo</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50 text-slate-800 bg-white">
              {filteredStudents.map((s, idx) => (
                <tr key={s.id || idx} className="hover:bg-slate-50">
                  <td className="px-3 py-2 text-slate-400 font-mono">{idx + 1}</td>
                  <td className="px-3 py-2 font-medium text-slate-900">{s.studentName}</td>
                  <td className="px-3 py-2 text-slate-600 font-mono">{s.admissionNumber}</td>
                  <td className="px-3 py-2 text-slate-700">
                    {s.standard}{s.section ? ` - ${s.section}` : ''}
                  </td>
                  <td className="px-3 py-2 text-slate-500">{s.rollNumber || '-'}</td>
                  <td className="px-3 py-2 text-slate-600">{s.bloodGroup || '-'}</td>
                  <td className="px-3 py-2 text-slate-600 font-mono">{s.emergencyContact || '-'}</td>
                  <td className="px-3 py-2">
                    {s.photoFileId ? (
                      <span className="inline-flex items-center text-[10px] text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full font-medium">
                        <Check className="h-3 w-3 mr-0.5" /> Attached
                      </span>
                    ) : (
                      <span className="inline-flex items-center text-[10px] text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full font-medium">
                        None
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Approve Quotation Confirmation Modal */}
      <Modal
        isOpen={isApproveModalOpen}
        onClose={() => setIsApproveModalOpen(false)}
        title="Approve Order Quotation"
        footer={
          <div className="flex justify-end space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsApproveModalOpen(false)}
              disabled={approveQuotationMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
              onClick={() => approveQuotationMutation.mutate()}
              disabled={approveQuotationMutation.isPending}
            >
              {approveQuotationMutation.isPending ? <LoadingSpinner size="sm" className="mr-1" /> : null}
              Confirm & Approve Quotation
            </Button>
          </div>
        }
      >
        <div className="space-y-3 text-xs text-slate-600">
          <p>
            You are approving quotation <span className="font-bold text-slate-900 font-mono">{quotation?.quotationNumber}</span> for a total of{' '}
            <span className="font-bold text-slate-900 font-mono">₹{Number(quotation?.totalAmount || 0).toFixed(2)}</span> (inclusive of 18% GST).
          </p>
          <p className="bg-amber-50 border border-amber-200 text-amber-900 p-2.5 rounded-lg">
            Once approved, the order status changes to <span className="font-semibold">APPROVED</span> and will be assigned to a verified printing agency for production.
          </p>
        </div>
      </Modal>
    </div>
  );
}
