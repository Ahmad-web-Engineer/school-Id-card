import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Input from '../../components/common/Input';
import Textarea from '../../components/common/Textarea';
import Modal from '../../components/common/Modal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import { ORDER_STATUSES } from '../../features/orders/ordersConstants';
import {
  ArrowLeft,
  ShoppingBag,
  CheckCircle,
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
  Check,
  School,
  XCircle,
  Calculator,
  Plus,
  Trash2,
  Briefcase,
  Printer,
  Download,
  Eye,
  Sliders,
} from 'lucide-react';
import AssignAgencyModal from '../../features/assignments/AssignAgencyModal';
import SingleCardPreviewModal from '../../features/templates/SingleCardPreviewModal';
import PrintImpositionModal from '../../features/production/PrintImpositionModal';
import { ASSIGNMENT_STATUSES } from '../../features/assignments/assignmentsConstants';
import { directPrintUrl } from '../../utils/printHelper';

export default function AdminOrderDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [studentSearch, setStudentSearch] = useState('');
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // Student Card Preview & Download State
  const [selectedPreviewStudent, setSelectedPreviewStudent] = useState(null);
  const [downloadingStudentId, setDownloadingStudentId] = useState(null);

  // Assignment Modal State
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [isImpositionModalOpen, setIsImpositionModalOpen] = useState(false);

  // Correction Modal State
  const [isCorrectionModalOpen, setIsCorrectionModalOpen] = useState(false);
  const [correctionRemarks, setCorrectionRemarks] = useState('');

  // Quotation Modal State
  const [isQuotationModalOpen, setIsQuotationModalOpen] = useState(false);
  const [quoteItems, setQuoteItems] = useState([]);
  const [quoteDiscount, setQuoteDiscount] = useState('0.00');
  const [quoteNotes, setQuoteNotes] = useState('Payment within 15 days of invoice date.');
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);
  const [isDirectPrinting, setIsDirectPrinting] = useState(null);
  const [isDownloadingA4, setIsDownloadingA4] = useState(false);
  const [isDownloadingA4Jpg, setIsDownloadingA4Jpg] = useState(false);
  const [isDownloadingZip, setIsDownloadingZip] = useState(false);
  const [downloadingImageStudentId, setDownloadingImageStudentId] = useState(null);

  const handleDirectPrintA4 = async () => {
    try {
      setIsDirectPrinting('a4');
      setActionError(null);
      await directPrintUrl(`/admin/orders/${id}/print-a4-sheet`, 'A4 Imposition Sheet');
    } catch (err) {
      setActionError('Direct print failed: ' + (err.message || 'Printer error'));
    } finally {
      setIsDirectPrinting(null);
    }
  };

  const handleDirectPrintCards = async () => {
    try {
      setIsDirectPrinting('cards');
      setActionError(null);
      await directPrintUrl(`/admin/orders/${id}/print-pdf`, 'Order Cards');
    } catch (err) {
      setActionError('Direct print failed: ' + (err.message || 'Printer error'));
    } finally {
      setIsDirectPrinting(null);
    }
  };

  const handleDownloadA4 = async () => {
    try {
      setIsDownloadingA4(true);
      setActionError(null);
      const res = await api.get(`/admin/orders/${id}/print-a4-sheet`, { responseType: 'blob', timeout: 180000 });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${order.orderNumber || 'order'}_a4_imposition_sheet.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError(err.message || 'Failed to generate A4 imposition sheet PDF.');
    } finally {
      setIsDownloadingA4(false);
    }
  };

  const handleDownloadA4Jpg = async (side = 'front') => {
    try {
      setIsDownloadingA4Jpg(true);
      setActionError(null);
      const res = await api.get(`/admin/orders/${id}/print-a4-image?side=${side}`, { responseType: 'blob', timeout: 180000 });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'image/jpeg' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${order.orderNumber || 'order'}_a4_sheet_${side}.jpg`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError(err.message || 'Failed to download A4 sheet JPEG image.');
    } finally {
      setIsDownloadingA4Jpg(false);
    }
  };

  const handleDownloadZip = async () => {
    try {
      setIsDownloadingZip(true);
      setActionError(null);
      const res = await api.get(`/admin/orders/${id}/download-images`, { responseType: 'blob', timeout: 180000 });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/zip' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${order.orderNumber || 'order'}_cards_300dpi_jpegs.zip`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError(err.message || 'Failed to download card JPEG ZIP bundle.');
    } finally {
      setIsDownloadingZip(false);
    }
  };

  const handleDownloadSingleImage = async (student, side = 'front') => {
    const studentId = student.studentId || student.id;
    try {
      setDownloadingImageStudentId(studentId);
      setActionError(null);
      const res = await api.get(`/admin/orders/${id}/students/${studentId}/download-image?side=${side}`, { responseType: 'blob', timeout: 60000 });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'image/jpeg' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${student.admissionNumber || studentId}_${side}.jpg`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError(err.message || 'Failed to download student JPEG image.');
    } finally {
      setDownloadingImageStudentId(null);
    }
  };

  const handleDirectPrintSingleStudent = async (student) => {
    const studentId = student.studentId || student.id;
    try {
      setIsDirectPrinting(studentId);
      setActionError(null);
      await directPrintUrl(`/admin/orders/${id}/students/${studentId}/print-pdf`, `Student Card ${student.studentName}`);
    } catch (err) {
      setActionError('Failed to print student card: ' + (err.message || 'Printer error'));
    } finally {
      setIsDirectPrinting(null);
    }
  };

  const handleDownloadPdf = async () => {
    try {
      setIsDownloadingPdf(true);
      setActionError(null);
      const res = await api.get(`/admin/orders/${id}/print-pdf`, { responseType: 'blob', timeout: 180000 });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${order.orderNumber || 'order'}_cards_print_ready.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError(err.message || 'Failed to generate print-ready card PDF.');
    } finally {
      setIsDownloadingPdf(false);
    }
  };

  const handleDownloadSinglePdf = async (student) => {
    const studentId = student.studentId || student.id;
    try {
      setDownloadingStudentId(studentId);
      setActionError(null);
      const res = await api.get(`/admin/orders/${id}/students/${studentId}/print-pdf`, { responseType: 'blob', timeout: 60000 });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `card_${student.admissionNumber || studentId}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError(err.message || 'Failed to generate single student ID card PDF.');
    } finally {
      setDownloadingStudentId(null);
    }
  };

  const generateInvoiceMutation = useMutation({
    mutationFn: () => api.post(`/admin/finance/orders/${id}/invoices`, {}),
    onSuccess: (res) => {
      setActionSuccess(`Tax Invoice ${res?.data?.invoiceNumber || ''} generated successfully!`);
      queryClient.invalidateQueries({ queryKey: ['admin-order-detail', id] });
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to generate tax invoice.');
    },
  });

  const closeOrderMutation = useMutation({
    mutationFn: () => api.post(`/admin/orders/${id}/close`),
    onSuccess: () => {
      setActionSuccess('Order marked as CLOSED and archived successfully.');
      queryClient.invalidateQueries({ queryKey: ['admin-order-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['admin-orders'] });
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to close order.');
    },
  });

  // 1. Fetch Order Details
  const {
    data: orderRes,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['admin-order-detail', id],
    queryFn: () => api.get(`/admin/orders/${id}`),
    enabled: !!id,
  });

  // 1b. Fetch Platform Settings for Dynamic Pricing Defaults
  const { data: settingsRes } = useQuery({
    queryKey: ['admin-platform-settings'],
    queryFn: () => api.get('/admin/settings'),
  });
  const platformSettings = settingsRes?.data;

  // Fetch Child Agency Assignments
  const { data: assignmentsRes } = useQuery({
    queryKey: ['admin-order-assignments', id],
    queryFn: () => api.get('/admin/assignments', { params: { orderId: id, page: 0, size: 50 } }),
    enabled: !!id,
  });

  const order = orderRes?.data;

  // Mutations
  const startReviewMutation = useMutation({
    mutationFn: () => api.post(`/admin/orders/${id}/start-review`),
    onSuccess: () => {
      setActionSuccess('Order moved to Under Review.');
      queryClient.invalidateQueries({ queryKey: ['admin-order-detail', id] });
    },
    onError: (err) => setActionError(err.message || 'Failed to start review.'),
  });

  const verifyOrderMutation = useMutation({
    mutationFn: () => api.post(`/admin/orders/${id}/verify`),
    onSuccess: () => {
      setActionSuccess('Order verified successfully! Ready for quotation generation.');
      queryClient.invalidateQueries({ queryKey: ['admin-order-detail', id] });
    },
    onError: (err) => setActionError(err.message || 'Failed to verify order.'),
  });

  const requestCorrectionMutation = useMutation({
    mutationFn: () =>
      api.post(`/admin/orders/${id}/corrections`, {
        remarks: correctionRemarks,
      }),
    onSuccess: () => {
      setActionSuccess('Correction requested from school.');
      setIsCorrectionModalOpen(false);
      setCorrectionRemarks('');
      queryClient.invalidateQueries({ queryKey: ['admin-order-detail', id] });
    },
    onError: (err) => setActionError(err.message || 'Failed to request correction.'),
  });

  const generateQuotationMutation = useMutation({
    mutationFn: () => {
      const discount = parseFloat(quoteDiscount) || 0;
      const taxRate = platformSettings?.taxRatePercent != null ? parseFloat(platformSettings.taxRatePercent) : 18.00;
      const validDays = platformSettings?.quotationValidityDays != null ? parseInt(platformSettings.quotationValidityDays, 10) : 15;
      return api.post(`/admin/orders/${id}/quotation`, {
        discount: discount,
        taxRate: taxRate,
        validDays: validDays,
        notes: quoteNotes.trim() || undefined,
        items: quoteItems.map((item) => ({
          productDescription: item.itemDescription || item.productDescription || 'PVC ID Card',
          quantity: parseInt(item.quantity, 10),
          unitPrice: parseFloat(item.unitPrice),
        })),
      });
    },
    onSuccess: () => {
      setActionSuccess('Quotation generated and sent to school!');
      setIsQuotationModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['admin-order-detail', id] });
    },
    onError: (err) => setActionError(err.message || 'Failed to generate quotation.'),
  });

  // Open Quotation Modal with initial items
  const handleOpenQuotationModal = () => {
    const defaultPvcPrice = platformSettings?.pricingPvcCard != null ? Number(platformSettings.pricingPvcCard).toFixed(2) : '45.00';
    if (order?.items) {
      const initialItems = order.items.map((i) => ({
        itemDescription: i.productType || i.itemDescription || 'PVC ID Card',
        quantity: i.quantity,
        unitPrice: (i.unitSellingPrice && Number(i.unitSellingPrice) > 0) ? Number(i.unitSellingPrice).toFixed(2) : defaultPvcPrice,
      }));
      setQuoteItems(initialItems);
      setQuoteDiscount('0.00');
    }
    setIsQuotationModalOpen(true);
  };

  const handleUpdateQuoteItem = (index, field, value) => {
    setQuoteItems((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [field]: value };
      return copy;
    });
  };

  const handleAddQuoteItem = () => {
    setQuoteItems((prev) => [
      ...prev,
      { itemDescription: 'Special Packaging / Service', quantity: 1, unitPrice: '100.00' },
    ]);
  };

  const handleRemoveQuoteItem = (index) => {
    setQuoteItems((prev) => prev.filter((_, idx) => idx !== index));
  };

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
        message={error?.message || 'Could not load order details.'}
        onRetry={refetch}
      />
    );
  }

  const statusConfig = ORDER_STATUSES[order.orderStatus] || {
    label: order.orderStatus,
    variant: 'neutral',
  };

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

  // Quotation Math Calculation for modal preview
  const quoteSubtotal = quoteItems.reduce((acc, curr) => {
    const qty = parseInt(curr.quantity, 10) || 0;
    const price = parseFloat(curr.unitPrice) || 0;
    return acc + qty * price;
  }, 0);
  const discountVal = Math.min(quoteSubtotal, Math.max(0, parseFloat(quoteDiscount) || 0));
  const quoteTaxable = Math.max(0, quoteSubtotal - discountVal);
  const quoteTax = quoteTaxable * 0.18;
  const quoteTotal = quoteTaxable + quoteTax;

  const quotation = order.quotation;

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/admin/orders')}
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
            <p className="text-xs text-slate-500 mt-0.5 flex items-center gap-1">
              <School className="h-3.5 w-3.5 text-indigo-600" />
              <span className="font-semibold text-slate-700">{order.schoolName}</span>
              <span>• Created {new Date(order.createdAt).toLocaleDateString('en-IN')}</span>
            </p>
          </div>
        </div>

        {/* Admin Workflow Action Buttons */}
        <div className="flex items-center space-x-2">
          {order.orderStatus === 'SUBMITTED' && (
            <Button
              size="sm"
              onClick={() => startReviewMutation.mutate()}
              disabled={startReviewMutation.isPending}
            >
              {startReviewMutation.isPending ? <LoadingSpinner size="sm" className="mr-1" /> : <Clock className="h-4 w-4 mr-1.5" />}
              Start Review
            </Button>
          )}

          {order.orderStatus === 'UNDER_REVIEW' && (
            <>
              <Button
                variant="outline"
                size="sm"
                className="text-rose-600 border-rose-200 hover:bg-rose-50"
                onClick={() => setIsCorrectionModalOpen(true)}
              >
                <XCircle className="h-4 w-4 mr-1.5" />
                Request Correction
              </Button>
              <Button
                size="sm"
                className="bg-emerald-600 hover:bg-emerald-700 text-white"
                onClick={() => verifyOrderMutation.mutate()}
                disabled={verifyOrderMutation.isPending}
              >
                {verifyOrderMutation.isPending ? <LoadingSpinner size="sm" className="mr-1" /> : <CheckCircle2 className="h-4 w-4 mr-1.5" />}
                Verify Order
              </Button>
            </>
          )}

          {['VERIFIED', 'QUOTATION_GENERATED'].includes(order.orderStatus) && (
            <Button
              size="sm"
              className="bg-indigo-600 hover:bg-indigo-700 text-white"
              onClick={handleOpenQuotationModal}
            >
              <Calculator className="h-4 w-4 mr-1.5" />
              {order.orderStatus === 'QUOTATION_GENERATED' ? 'Regenerate Quotation' : 'Generate Quotation'}
            </Button>
          )}
          {['APPROVED', 'AWAITING_ASSIGNMENT', 'ASSIGNED', 'IN_PRODUCTION', 'QUALITY_CHECK', 'DISPATCHED', 'DELIVERED', 'CLOSED'].includes(order.orderStatus) && (
            <>
              {/* Prepress Studio & Direct Print Actions */}
              <Button
                size="sm"
                className="bg-indigo-600 hover:bg-indigo-700 text-white flex items-center gap-1.5 shadow-xs font-semibold"
                onClick={() => setIsImpositionModalOpen(true)}
                title="Open Prepress Imposition Studio to configure grid, gaps, margins, and bulk print"
              >
                <Sliders className="h-4 w-4" />
                Prepress Print Studio
              </Button>

              <Button
                size="sm"
                className="bg-slate-800 hover:bg-slate-900 text-white flex items-center gap-1.5 shadow-xs"
                onClick={handleDirectPrintA4}
                disabled={isDirectPrinting === 'a4'}
                title="Send A4 Imposition Sheet directly to printer dialog"
              >
                {isDirectPrinting === 'a4' ? (
                  <LoadingSpinner size="xs" />
                ) : (
                  <Printer className="h-4 w-4" />
                )}
                Direct Print (A4 Sheet)
              </Button>

              <Button
                variant="outline"
                size="sm"
                className="border-indigo-300 text-indigo-700 hover:bg-indigo-50 flex items-center gap-1.5"
                onClick={handleDirectPrintCards}
                disabled={isDirectPrinting === 'cards'}
                title="Send individual CR80 ID cards directly to card printer dialog"
              >
                {isDirectPrinting === 'cards' ? (
                  <LoadingSpinner size="xs" />
                ) : (
                  <Printer className="h-4 w-4 text-indigo-600" />
                )}
                Direct Print (Cards)
              </Button>

              {/* Download File Options */}
              <Button
                variant="outline"
                size="sm"
                className="text-slate-700 hover:bg-slate-100 flex items-center gap-1.5"
                onClick={handleDownloadA4}
                disabled={isDownloadingA4}
                title="Download A4 Grid Layout PDF with duplex alignment and crop marks"
              >
                {isDownloadingA4 ? (
                  <LoadingSpinner size="xs" />
                ) : (
                  <FileText className="h-4 w-4 text-indigo-600" />
                )}
                A4 PDF Sheet
              </Button>

              <Button
                variant="outline"
                size="sm"
                className="text-slate-700 hover:bg-slate-100 flex items-center gap-1.5"
                onClick={() => handleDownloadA4Jpg('front')}
                disabled={isDownloadingA4Jpg}
                title="Download A4 Imposition Sheet as 300 DPI high-resolution JPEG"
              >
                {isDownloadingA4Jpg ? (
                  <LoadingSpinner size="xs" />
                ) : (
                  <Download className="h-4 w-4 text-emerald-600" />
                )}
                A4 JPEG Sheet
              </Button>

              <Button
                variant="outline"
                size="sm"
                className="text-slate-700 hover:bg-slate-100 flex items-center gap-1.5"
                onClick={handleDownloadPdf}
                disabled={isDownloadingPdf}
                title="Download CR80 Individual Cards multi-page PDF"
              >
                {isDownloadingPdf ? (
                  <LoadingSpinner size="xs" />
                ) : (
                  <Download className="h-4 w-4 text-indigo-600" />
                )}
                Cards PDF
              </Button>

              <Button
                variant="outline"
                size="sm"
                className="text-slate-700 hover:bg-slate-100 flex items-center gap-1.5"
                onClick={handleDownloadZip}
                disabled={isDownloadingZip}
                title="Download all cards as 300 DPI high-resolution JPEG images in a ZIP"
              >
                {isDownloadingZip ? (
                  <LoadingSpinner size="xs" />
                ) : (
                  <Package className="h-4 w-4 text-indigo-600" />
                )}
                300 DPI ZIP
              </Button>

              <Button
                size="sm"
                className="bg-emerald-600 hover:bg-emerald-700 text-white flex items-center gap-1.5"
                onClick={() => generateInvoiceMutation.mutate()}
                disabled={generateInvoiceMutation.isPending}
              >
                <Receipt className="h-4 w-4" />
                {generateInvoiceMutation.isPending ? 'Generating...' : 'Issue Tax Invoice'}
              </Button>
            </>
          )}

          {['APPROVED', 'AWAITING_ASSIGNMENT', 'ASSIGNED'].includes(order.orderStatus) && (
            <Button
              size="sm"
              className="bg-indigo-600 hover:bg-indigo-700 text-white"
              onClick={() => setIsAssignModalOpen(true)}
            >
              <Briefcase className="h-4 w-4 mr-1.5" />
              Assign to Agency
            </Button>
          )}

          {order.orderStatus === 'DELIVERED' && (
            <Button
              size="sm"
              className="bg-slate-900 hover:bg-slate-800 text-white flex items-center gap-1.5"
              onClick={() => closeOrderMutation.mutate()}
              disabled={closeOrderMutation.isPending}
            >
              {closeOrderMutation.isPending ? <LoadingSpinner size="xs" /> : <CheckCircle2 className="h-4 w-4 text-emerald-400" />}
              Close Order
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

      {/* Approved Notice */}
      {order.orderStatus === 'APPROVED' && (
        <div className="rounded-lg bg-emerald-50 border border-emerald-200 p-4 flex items-start space-x-3">
          <CheckCircle className="h-5 w-5 text-emerald-600 mt-0.5 flex-shrink-0" />
          <div>
            <h4 className="text-sm font-semibold text-emerald-900">Quotation Approved by School</h4>
            <p className="text-xs text-emerald-700 mt-0.5">
              This order has been verified and approved by the school. It is queued and ready for assignment to a printing agency.
            </p>
          </div>
        </div>
      )}

      {/* Correction Banner if active */}
      {order.orderStatus === 'CORRECTION_REQUIRED' && (
        <div className="rounded-lg bg-rose-50 border border-rose-200 p-4 flex items-start space-x-3">
          <AlertTriangle className="h-5 w-5 text-rose-600 mt-0.5 flex-shrink-0" />
          <div>
            <h4 className="text-sm font-semibold text-rose-900">Correction Remarks Sent to School</h4>
            <p className="text-xs text-rose-700 mt-0.5">{order.rejectionRemarks}</p>
          </div>
        </div>
      )}

      {/* Active Quotation Card */}
      {quotation && (
        <Card className="p-5 border-amber-200 bg-amber-50/20">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-amber-100 gap-3">
            <div className="flex items-center space-x-2">
              <Receipt className="h-5 w-5 text-amber-600" />
              <div>
                <h3 className="text-sm font-bold text-slate-900">
                  Quotation: <span className="font-mono text-amber-700">{quotation.quotationNumber}</span>
                </h3>
                <p className="text-[11px] text-slate-500">
                  Generated {new Date(quotation.createdAt).toLocaleDateString('en-IN')}
                </p>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              <Badge
                variant={quotation.quotationStatus === 'ACCEPTED' ? 'success' : 'warning'}
                size="sm"
              >
                {quotation.quotationStatus === 'ACCEPTED' ? 'Accepted by School' : 'Awaiting School Approval'}
              </Badge>
              <Button size="xs" variant="outline" onClick={handleOpenQuotationModal}>
                Edit / Regenerate
              </Button>
            </div>
          </div>

          <div className="mt-3 overflow-x-auto">
            <table className="min-w-full text-xs divide-y divide-slate-200">
              <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider">
                <tr>
                  <th className="px-3 py-2 text-left">Item Description</th>
                  <th className="px-3 py-2 text-right">Quantity</th>
                  <th className="px-3 py-2 text-right">Unit Price</th>
                  <th className="px-3 py-2 text-right">Total Price</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {quotation.items.map((item, idx) => (
                  <tr key={item.id || idx}>
                    <td className="px-3 py-2 text-slate-800 font-medium">{item.productDescription || item.itemDescription}</td>
                    <td className="px-3 py-2 text-right text-slate-600">{item.quantity}</td>
                    <td className="px-3 py-2 text-right text-slate-600 font-mono">₹{Number(item.unitPrice).toFixed(2)}</td>
                    <td className="px-3 py-2 text-right text-slate-900 font-semibold font-mono">₹{Number(item.amount || item.totalPrice || 0).toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-3 pt-3 border-t border-slate-200 flex justify-end text-xs">
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
                <span>Total Payable:</span>
                <span className="font-mono text-indigo-700">₹{Number(quotation.totalAmount).toFixed(2)}</span>
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Order Details & Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="p-4 space-y-3">
          <div className="flex items-center space-x-2 text-slate-900 font-semibold text-sm">
            <Layers className="h-4 w-4 text-indigo-600" />
            <h4>Template & Items</h4>
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
            <h5 className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-2">Item Breakdown</h5>
            <div className="space-y-1.5 text-xs">
              {order.items?.map((item, idx) => (
                <div key={idx} className="flex justify-between text-slate-700">
                  <span className="truncate max-w-[180px]">{item.productType || item.itemDescription}</span>
                  <span className="font-medium font-mono">× {item.quantity}</span>
                </div>
              ))}
            </div>
          </div>
        </Card>

        <Card className="p-4 space-y-3">
          <div className="flex items-center space-x-2 text-slate-900 font-semibold text-sm">
            <Truck className="h-4 w-4 text-indigo-600" />
            <h4>School & Delivery</h4>
          </div>
          <div className="space-y-2 text-xs text-slate-600">
            <div>
              <span className="text-slate-500 block text-[11px]">School Name</span>
              <span className="font-medium text-slate-800">{order.schoolName}</span>
            </div>
            <div>
              <span className="text-slate-500 block text-[11px]">Contact Person & Phone</span>
              <span className="font-medium text-slate-800">{order.contactPerson} ({order.contactPhone})</span>
            </div>
            <div>
              <span className="text-slate-500 block text-[11px]">Delivery Address</span>
              <span className="font-medium text-slate-800">{order.deliveryAddress}</span>
            </div>
          </div>
        </Card>

        <Card className="p-4 space-y-3">
          <div className="flex items-center space-x-2 text-slate-900 font-semibold text-sm">
            <FileText className="h-4 w-4 text-indigo-600" />
            <h4>Order Notes</h4>
          </div>
          <p className="text-xs text-slate-600 leading-relaxed bg-slate-50 p-3 rounded-lg border border-slate-100">
            {order.notes || 'No notes provided by school.'}
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
                Verified Student Roster ({order.students?.length || 0} Students)
              </h3>
              <p className="text-[11px] text-slate-500">
                Captured student snapshots frozen for production.
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
                <th className="px-3 py-2.5 text-right">Card Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50 text-slate-800 bg-white">
              {filteredStudents.map((s, idx) => (
                <tr key={s.id || idx} className="hover:bg-slate-50">
                  <td className="px-3 py-2 text-slate-400 font-mono">{idx + 1}</td>
                  <td className="px-3 py-2 font-medium text-slate-900">{s.studentName}</td>
                  <td className="px-3 py-2 text-slate-600 font-mono">{s.admissionNumber}</td>
                  <td className="px-3 py-2 text-slate-700">
                    {s.standard || s.className}{s.section ? ` - ${s.section}` : ''}
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
                      <span className="inline-flex items-center text-[10px] text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full font-medium">
                        <AlertTriangle className="h-3 w-3 mr-0.5" /> Missing
                      </span>
                    )}
                  </td>
                  <td className="px-3 py-2 text-right whitespace-nowrap">
                    <div className="inline-flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={() => setSelectedPreviewStudent(s)}
                        className="inline-flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-indigo-700 bg-indigo-50 border border-indigo-200 rounded-md hover:bg-indigo-100"
                        title="Preview ID card front & back"
                      >
                        <Eye className="w-3 h-3" />
                        Preview
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDirectPrintSingleStudent(s)}
                        disabled={isDirectPrinting === (s.studentId || s.id)}
                        className="inline-flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-indigo-700 bg-indigo-50/50 border border-indigo-200 rounded-md hover:bg-indigo-100"
                        title="Direct print single student card"
                      >
                        {isDirectPrinting === (s.studentId || s.id) ? (
                          <LoadingSpinner size="sm" />
                        ) : (
                          <Printer className="w-3 h-3 text-indigo-600" />
                        )}
                        Print
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDownloadSinglePdf(s)}
                        disabled={downloadingStudentId === (s.studentId || s.id)}
                        className="inline-flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-slate-700 bg-slate-100 border border-slate-200 rounded-md hover:bg-slate-200"
                        title="Download print-ready PDF"
                      >
                        {downloadingStudentId === (s.studentId || s.id) ? (
                          <LoadingSpinner size="sm" />
                        ) : (
                          <Download className="w-3 h-3" />
                        )}
                        PDF
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDownloadSingleImage(s, 'front')}
                        disabled={downloadingImageStudentId === (s.studentId || s.id)}
                        className="inline-flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-emerald-700 bg-emerald-50 border border-emerald-200 rounded-md hover:bg-emerald-100"
                        title="Download 300 DPI JPEG"
                      >
                        {downloadingImageStudentId === (s.studentId || s.id) ? (
                          <LoadingSpinner size="sm" />
                        ) : (
                          <Download className="w-3 h-3 text-emerald-600" />
                        )}
                        JPEG
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Request Correction Modal */}
      <Modal
        isOpen={isCorrectionModalOpen}
        onClose={() => setIsCorrectionModalOpen(false)}
        title="Request Correction from School"
        footer={
          <div className="flex justify-end space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsCorrectionModalOpen(false)}
              disabled={requestCorrectionMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              className="bg-rose-600 hover:bg-rose-700 text-white"
              onClick={() => requestCorrectionMutation.mutate()}
              disabled={!correctionRemarks.trim() || requestCorrectionMutation.isPending}
            >
              {requestCorrectionMutation.isPending ? <LoadingSpinner size="sm" className="mr-1" /> : null}
              Send Correction Request
            </Button>
          </div>
        }
      >
        <div className="space-y-3">
          <p className="text-xs text-slate-600">
            Specify the reasons why this order batch cannot be verified (e.g. blurry photos, incorrect admission numbers, missing class details).
          </p>
          <Textarea
            label="Correction Remarks"
            rows={4}
            placeholder="Describe what the school needs to fix..."
            value={correctionRemarks}
            onChange={(e) => setCorrectionRemarks(e.target.value)}
            required
          />
        </div>
      </Modal>


      {/* Generate Quotation Modal */}
      <Modal
        isOpen={isQuotationModalOpen}
        onClose={() => setIsQuotationModalOpen(false)}
        title="Generate Order Quotation"
        maxWidth="max-w-2xl"
        footer={
          <div className="flex justify-end space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsQuotationModalOpen(false)}
              disabled={generateQuotationMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              className="bg-indigo-600 hover:bg-indigo-700 text-white"
              onClick={() => generateQuotationMutation.mutate()}
              disabled={quoteItems.length === 0 || generateQuotationMutation.isPending}
            >
              {generateQuotationMutation.isPending ? <LoadingSpinner size="sm" className="mr-1" /> : null}
              Issue Quotation to School
            </Button>
          </div>
        }
      >
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-semibold text-slate-900 uppercase tracking-wider">
              Itemized Quotation Lines
            </h4>
            <Button size="xs" variant="outline" onClick={handleAddQuoteItem}>
              <Plus className="h-3 w-3 mr-1" />
              Add Line Item
            </Button>
          </div>

          <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
            {quoteItems.map((item, idx) => (
              <div key={idx} className="flex items-center gap-2 bg-slate-50 p-2 rounded-lg border border-slate-200">
                <input
                  type="text"
                  placeholder="Item description"
                  value={item.itemDescription}
                  onChange={(e) => handleUpdateQuoteItem(idx, 'itemDescription', e.target.value)}
                  className="flex-1 text-xs px-2 py-1 rounded border border-slate-200 bg-white"
                />
                <div className="w-20">
                  <input
                    type="number"
                    min="1"
                    placeholder="Qty"
                    value={item.quantity}
                    onChange={(e) => handleUpdateQuoteItem(idx, 'quantity', e.target.value)}
                    className="w-full text-xs px-2 py-1 rounded border border-slate-200 bg-white text-center font-mono"
                  />
                </div>
                <div className="w-24">
                  <input
                    type="number"
                    step="0.50"
                    placeholder="Price (₹)"
                    value={item.unitPrice}
                    onChange={(e) => handleUpdateQuoteItem(idx, 'unitPrice', e.target.value)}
                    className="w-full text-xs px-2 py-1 rounded border border-slate-200 bg-white text-right font-mono"
                  />
                </div>
                <button
                  type="button"
                  onClick={() => handleRemoveQuoteItem(idx)}
                  className="p-1 text-slate-400 hover:text-rose-600 rounded"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
            <Input
              label="Discount Amount (₹)"
              type="number"
              step="1"
              value={quoteDiscount}
              onChange={(e) => setQuoteDiscount(e.target.value)}
            />
            <Input
              label="Payment Terms / Notes"
              value={quoteNotes}
              onChange={(e) => setQuoteNotes(e.target.value)}
            />
          </div>

          {/* Quotation Preview Box */}
          <div className="rounded-lg bg-indigo-50/50 border border-indigo-100 p-3 space-y-1.5 text-xs">
            <div className="flex justify-between text-slate-600">
              <span>Subtotal:</span>
              <span className="font-mono">₹{quoteSubtotal.toFixed(2)}</span>
            </div>
            {discountVal > 0 && (
              <div className="flex justify-between text-emerald-600">
                <span>Discount:</span>
                <span className="font-mono">- ₹{discountVal.toFixed(2)}</span>
              </div>
            )}
            <div className="flex justify-between text-slate-600">
              <span>GST (18%):</span>
              <span className="font-mono">₹{quoteTax.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-slate-900 font-bold text-sm pt-2 border-t border-indigo-200">
              <span>Total Payable Amount:</span>
              <span className="font-mono text-indigo-700">₹{quoteTotal.toFixed(2)}</span>
            </div>
          </div>
        </div>
      </Modal>


      {order && (
        <AssignAgencyModal
          isOpen={isAssignModalOpen}
          onClose={() => setIsAssignModalOpen(false)}
          order={order}
          onSuccess={() => {
            setActionSuccess('Order successfully assigned to agency!');
            queryClient.invalidateQueries({ queryKey: ['admin-order-detail', id] });
            queryClient.invalidateQueries({ queryKey: ['admin-order-assignments', id] });
          }}
        />
      )}

      {/* Prepress Imposition & Bulk Printing Studio */}
      {order && (
        <PrintImpositionModal
          isOpen={isImpositionModalOpen}
          onClose={() => setIsImpositionModalOpen(false)}
          role="SUPER_ADMIN"
          orderId={order.id}
          identifierName={order.orderNumber || 'Order'}
          totalStudents={order.students?.length || order.items?.reduce((acc, i) => acc + (i.quantity || 0), 0) || 1}
          sampleStudent={order.students?.[0] || null}
          initialSettings={order.printSettings || null}
          allowAgencyOverride={false}
          onSettingsUpdated={() => {
            queryClient.invalidateQueries({ queryKey: ['admin-order-detail', id] });
          }}
        />
      )}

      {/* Single Card Proof Inspection Modal */}
      {selectedPreviewStudent && (
        <SingleCardPreviewModal
          isOpen={!!selectedPreviewStudent}
          onClose={() => setSelectedPreviewStudent(null)}
          student={selectedPreviewStudent}
          cardSpecs={{
            widthMm: order.cardWidthMm || 85.6,
            heightMm: order.cardHeightMm || 54.0,
            bleedMarginMm: 3.0,
            frontDesignJson: order.frontDesignJson,
            backDesignJson: order.backDesignJson,
            schoolName: order.schoolName,
            schoolLogoFileId: order.schoolLogoFileId,
          }}
          onDownloadPdf={handleDownloadSinglePdf}
          isDownloadingPdf={downloadingStudentId === (selectedPreviewStudent?.studentId || selectedPreviewStudent?.id)}
        />
      )}
    </div>
  );
}
