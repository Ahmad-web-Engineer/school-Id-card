import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import { ASSIGNMENT_STATUSES } from '../../features/assignments/assignmentsConstants';
import {
  ArrowLeft,
  Briefcase,
  Calendar,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  Printer,
  FileText,
  Layers,
  Info,
  Package,
  Download,
  Search,
  Eye,
  LayoutGrid,
  List,
  User,
  Image as ImageIcon,
  Sliders,
  ShieldCheck,
} from 'lucide-react';
import SingleCardPreviewModal from '../../features/templates/SingleCardPreviewModal';
import PrintImpositionModal from '../../features/production/PrintImpositionModal';
import SubmitQualityCheckModal from '../../features/production/SubmitQualityCheckModal';
import { extractArray } from '../../services/responseUtils';
import { directPrintUrl } from '../../utils/printHelper';

export default function AgencyJobDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [isAcceptModalOpen, setIsAcceptModalOpen] = useState(false);
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [isImpositionModalOpen, setIsImpositionModalOpen] = useState(false);
  const [isQcModalOpen, setIsQcModalOpen] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);
  const [isDownloadingA4, setIsDownloadingA4] = useState(false);
  const [isDownloadingZip, setIsDownloadingZip] = useState(false);
  const [isDirectPrinting, setIsDirectPrinting] = useState(null); // 'a4' | 'cards' | null
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // Student Cards Gallery / Inspection State
  const [studentSearch, setStudentSearch] = useState('');
  const [viewMode, setViewMode] = useState('grid'); // 'grid' | 'table'
  const [selectedPreviewStudent, setSelectedPreviewStudent] = useState(null);
  const [downloadingStudentId, setDownloadingStudentId] = useState(null);
  const [directPrintingStudentId, setDirectPrintingStudentId] = useState(null);
  const [downloadingImageStudentId, setDownloadingImageStudentId] = useState(null);
  const [isDownloadingA4Jpg, setIsDownloadingA4Jpg] = useState(false);
  const authToken = localStorage.getItem('auth_token') || localStorage.getItem('token') || '';

  // Direct Print Handlers
  const handleDirectPrintA4 = async () => {
    try {
      setIsDirectPrinting('a4');
      setActionError(null);
      await directPrintUrl(`/agency/jobs/${id}/print-a4-sheet`);
      setActionSuccess('Sent A4 imposition sheet layout to printer.');
    } catch (err) {
      setActionError('Failed to trigger direct print for A4 sheet.');
    } finally {
      setIsDirectPrinting(null);
    }
  };

  const handleDirectPrintCards = async () => {
    try {
      setIsDirectPrinting('cards');
      setActionError(null);
      await directPrintUrl(`/agency/jobs/${id}/print-pdf`);
      setActionSuccess('Sent ID cards layout to printer.');
    } catch (err) {
      setActionError('Failed to trigger direct print for cards.');
    } finally {
      setIsDirectPrinting(null);
    }
  };

  const handleDirectPrintSingleStudent = async (student) => {
    const studentId = student.studentId || student.id;
    try {
      setDirectPrintingStudentId(studentId);
      setActionError(null);
      await directPrintUrl(`/agency/jobs/${id}/students/${studentId}/print-pdf`);
      setActionSuccess(`Sent card for ${student.studentName} to printer.`);
    } catch (err) {
      setActionError('Failed to print single card.');
    } finally {
      setDirectPrintingStudentId(null);
    }
  };

  // Download Handlers
  const handleDownloadPdf = async () => {
    try {
      setIsDownloadingPdf(true);
      setActionError(null);
      const res = await api.get(`/agency/jobs/${id}/print-pdf`, { responseType: 'blob' });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${job.jobNumber || 'job'}_cards_print_ready.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError('Failed to generate print-ready card PDF.');
    } finally {
      setIsDownloadingPdf(false);
    }
  };

  const handleDownloadA4 = async () => {
    try {
      setIsDownloadingA4(true);
      setActionError(null);
      const res = await api.get(`/agency/jobs/${id}/print-a4-sheet`, { responseType: 'blob' });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${job.jobNumber || 'job'}_a4_sheet.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError('Failed to generate A4 imposition sheet PDF.');
    } finally {
      setIsDownloadingA4(false);
    }
  };

  const handleDownloadA4Jpg = async (side = 'front') => {
    try {
      setIsDownloadingA4Jpg(true);
      setActionError(null);
      const res = await api.get(`/agency/jobs/${id}/print-a4-image?side=${side}`, { responseType: 'blob' });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'image/jpeg' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${job.jobNumber || 'job'}_a4_sheet_${side}.jpg`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError('Failed to download A4 sheet JPEG image.');
    } finally {
      setIsDownloadingA4Jpg(false);
    }
  };

  const handleDownloadImagesZip = async () => {
    try {
      setIsDownloadingZip(true);
      setActionError(null);
      const res = await api.get(`/agency/jobs/${id}/download-images`, { responseType: 'blob' });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/zip' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${job.jobNumber || 'job'}_cards_jpeg.zip`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError('Failed to generate high-resolution JPEG ZIP archive.');
    } finally {
      setIsDownloadingZip(false);
    }
  };

  const handleDownloadSinglePdf = async (student) => {
    const studentId = student.studentId || student.id;
    try {
      setDownloadingStudentId(studentId);
      setActionError(null);
      const res = await api.get(`/agency/jobs/${id}/students/${studentId}/print-pdf`, { responseType: 'blob' });
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
      setActionError('Failed to generate single student ID card PDF.');
    } finally {
      setDownloadingStudentId(null);
    }
  };

  const handleDownloadSingleImage = async (student, side = 'front') => {
    const studentId = student.studentId || student.id;
    try {
      setDownloadingImageStudentId(studentId);
      setActionError(null);
      const res = await api.get(`/agency/jobs/${id}/students/${studentId}/download-image?side=${side}`, { responseType: 'blob' });
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
      setActionError('Failed to download student JPEG image.');
    } finally {
      setDownloadingImageStudentId(null);
    }
  };

  // 1. Fetch Job Details
  const {
    data: jobRes,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['agency-job-detail', id],
    queryFn: () => api.get(`/agency/jobs/${id}`),
    enabled: !!id,
  });

  const job = jobRes?.data;

  // Accept Mutation
  const acceptMutation = useMutation({
    mutationFn: () => api.post(`/agency/jobs/${id}/accept`),
    onSuccess: () => {
      setActionSuccess('Job accepted successfully! You can now start print production.');
      setIsAcceptModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['agency-job-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['agency-jobs'] });
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to accept job.');
    },
  });

  // Reject Mutation
  const rejectMutation = useMutation({
    mutationFn: (reason) => api.post(`/agency/jobs/${id}/reject`, { rejectionReason: reason }),
    onSuccess: () => {
      setActionSuccess('Job has been rejected and returned to the platform administrator.');
      setIsRejectModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['agency-job-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['agency-jobs'] });
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to reject job.');
    },
  });

  // Quality Checks for this job
  const { data: jobQcReports = [] } = useQuery({
    queryKey: ['agency-job-qc-reports', id],
    queryFn: async () => {
      const res = await api.get(`/agency/jobs/${id}/quality-checks`);
      return extractArray(res);
    },
    enabled: !!id,
  });

  // Submit QC Mutation
  const submitQcMutation = useMutation({
    mutationFn: (payload) => api.post(`/agency/jobs/${id}/quality-checks`, payload),
    onSuccess: () => {
      setActionSuccess('Quality Inspection Report submitted successfully!');
      setIsQcModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['agency-job-qc-reports', id] });
      queryClient.invalidateQueries({ queryKey: ['agency-job-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['agency-qc-checks', id] });
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to submit quality check.');
    },
  });

  if (isLoading) {
    return (
      <div className="py-24 flex justify-center">
        <LoadingSpinner size="lg" className="text-indigo-600" />
      </div>
    );
  }

  if (isError || !job) {
    return (
      <ErrorState
        title="Job Not Found"
        message={error?.message || 'Could not load job details or access was denied.'}
        onRetry={refetch}
      />
    );
  }

  const statusConfig = ASSIGNMENT_STATUSES[job.assignmentStatus] || {
    label: job.assignmentStatus,
    variant: 'neutral',
  };

  const specs = job.cardSpecifications || {
    widthMm: 85.6,
    heightMm: 54.0,
    bleedMm: 3.0,
    templateName: 'Standard PVC Card',
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/agency/jobs')}
            className="p-1.5"
            aria-label="Back to assigned jobs"
          >
            <ArrowLeft className="h-5 w-5 text-slate-500" />
          </Button>
          <div>
            <div className="flex items-center space-x-2">
              <h1 className="text-xl font-bold text-slate-900 font-mono">{job.jobNumber}</h1>
              <Badge variant={statusConfig.variant} size="sm">
                {statusConfig.label}
              </Badge>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Academic Session: <strong className="text-slate-700">{job.academicSession}</strong> • Assigned {new Date(job.assignedAt).toLocaleDateString('en-IN')}
            </p>
          </div>
        </div>

        {/* Action Buttons: Direct Print & Export Options */}
        <div className="flex flex-wrap items-center gap-2">
          {job.assignmentStatus !== 'REJECTED' && (
            <>
              {/* Quality Inspection Button */}
              {['AGENCY_ACCEPTED', 'IN_PRODUCTION', 'COMPLETED', 'DISPATCHED'].includes(job.assignmentStatus) && (
                <Button
                  size="sm"
                  className="bg-emerald-600 hover:bg-emerald-700 text-white flex items-center gap-1.5 shadow-xs font-semibold"
                  onClick={() => setIsQcModalOpen(true)}
                  title="Perform quality check with visual card inspection & defect reporting"
                >
                  <ShieldCheck className="h-4 w-4" />
                  Quality Check {jobQcReports.length > 0 && `(${jobQcReports.length})`}
                </Button>
              )}

              {/* Prepress Studio & Direct Print Actions */}
              <Button
                size="sm"
                className="bg-indigo-600 hover:bg-indigo-700 text-white flex items-center gap-1.5 shadow-xs font-semibold"
                onClick={() => setIsImpositionModalOpen(true)}
                title="Open Prepress Imposition Studio for bulk printing and layout specs"
              >
                <Sliders className="h-4 w-4" />
                Prepress Print Studio
              </Button>

              <Button
                size="sm"
                className="bg-slate-800 hover:bg-slate-900 text-white flex items-center gap-1.5 shadow-xs"
                onClick={handleDirectPrintA4}
                disabled={isDirectPrinting === 'a4'}
                title="Send A4 Imposition Sheet (Grid Layout) directly to printer dialog"
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
                title="Download A4 Imposition Sheet as 300 DPI high-resolution JPEG image"
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
                onClick={handleDownloadImagesZip}
                disabled={isDownloadingZip}
                title="Download 300 DPI High-Resolution Front & Back JPEGs in a ZIP archive"
              >
                {isDownloadingZip ? (
                  <LoadingSpinner size="xs" />
                ) : (
                  <ImageIcon className="h-4 w-4 text-emerald-600" />
                )}
                JPEGs (ZIP)
              </Button>
            </>
          )}

          {job.assignmentStatus === 'ASSIGNED' && (
            <>
              <Button
                variant="outline"
                size="sm"
                className="text-rose-600 border-rose-200 hover:bg-rose-50"
                onClick={() => setIsRejectModalOpen(true)}
              >
                <XCircle className="h-4 w-4 mr-1.5" />
                Reject Job
              </Button>
              <Button
                size="sm"
                className="bg-emerald-600 hover:bg-emerald-700 text-white"
                onClick={() => setIsAcceptModalOpen(true)}
              >
                <CheckCircle2 className="h-4 w-4 mr-1.5" />
                Accept Job
              </Button>
            </>
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

      {/* Acceptance Status Notice */}
      {job.assignmentStatus === 'AGENCY_ACCEPTED' && (
        <div className="rounded-lg bg-sky-50 border border-sky-200 p-4 flex items-start space-x-3">
          <CheckCircle2 className="h-5 w-5 text-sky-600 mt-0.5 flex-shrink-0" />
          <div>
            <h4 className="text-sm font-semibold text-sky-900">Job Accepted by Your Agency</h4>
            <p className="text-xs text-sky-700 mt-0.5">
              Production commitment registered. Please adhere to the specified delivery deadline ({job.expectedDeliveryDate || 'N/A'}).
            </p>
          </div>
        </div>
      )}

      {job.assignmentStatus === 'REJECTED' && (
        <div className="rounded-lg bg-rose-50 border border-rose-200 p-4 flex items-start space-x-3">
          <AlertTriangle className="h-5 w-5 text-rose-600 mt-0.5 flex-shrink-0" />
          <div>
            <h4 className="text-sm font-semibold text-rose-900">Job Rejected</h4>
            <p className="text-xs text-rose-700 mt-0.5">Reason: {job.rejectionReason || 'No reason provided'}</p>
          </div>
        </div>
      )}

      {/* Grid: Print Specifications & Delivery Commitments */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Left Column: Technical Specifications */}
        <div className="md:col-span-2 space-y-6">
          {/* Card Dimensions & Technical Specs Card */}
          <Card className="p-5">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2 mb-4 border-b border-slate-100 pb-2">
              <Printer className="h-4 w-4 text-indigo-600" />
              Technical Print Specifications
            </h3>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                <span className="text-[10px] uppercase font-semibold text-slate-400 block">Card Width</span>
                <span className="text-base font-bold text-slate-900 font-mono mt-0.5 block">{specs.widthMm} mm</span>
                <span className="text-[10px] text-slate-500">Standard CR80</span>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                <span className="text-[10px] uppercase font-semibold text-slate-400 block">Card Height</span>
                <span className="text-base font-bold text-slate-900 font-mono mt-0.5 block">{specs.heightMm} mm</span>
                <span className="text-[10px] text-slate-500">Standard CR80</span>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                <span className="text-[10px] uppercase font-semibold text-slate-400 block">Bleed Margin</span>
                <span className="text-base font-bold text-indigo-700 font-mono mt-0.5 block">{specs.bleedMm} mm</span>
                <span className="text-[10px] text-slate-500">All 4 edges</span>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                <span className="text-[10px] uppercase font-semibold text-slate-400 block">Template Style</span>
                <span className="text-xs font-bold text-slate-800 truncate mt-1 block" title={specs.templateName}>
                  {specs.templateName}
                </span>
                <span className="text-[10px] text-slate-500">Double-sided</span>
              </div>
            </div>

            {job.notes && (
              <div className="mt-4 p-3 bg-amber-50 rounded-lg border border-amber-200 text-xs flex items-start space-x-2">
                <Info className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
                <div>
                  <span className="font-semibold text-amber-900">Admin Instructions:</span>
                  <p className="text-amber-800 mt-0.5">{job.notes}</p>
                </div>
              </div>
            )}
          </Card>

          {/* Allocated Production Items Table */}
          <Card className="p-5">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2 mb-4 border-b border-slate-100 pb-2">
              <Package className="h-4 w-4 text-indigo-600" />
              Allocated Job Items
            </h3>

            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
                <thead className="bg-slate-50 font-semibold text-slate-600 uppercase tracking-wider">
                  <tr>
                    <th className="px-3 py-2">Item Type</th>
                    <th className="px-3 py-2 text-right">Assigned Qty</th>
                    <th className="px-3 py-2 text-right">Printed</th>
                    <th className="px-3 py-2 text-right">Pending</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {job.items?.map((item) => (
                    <tr key={item.id} className="hover:bg-slate-50">
                      <td className="px-3 py-2.5 font-medium text-slate-800">{item.productType}</td>
                      <td className="px-3 py-2.5 text-right font-bold text-slate-900">{item.assignedQuantity}</td>
                      <td className="px-3 py-2.5 text-right font-mono text-emerald-600">{item.printedQuantity}</td>
                      <td className="px-3 py-2.5 text-right font-mono text-slate-500">{item.pendingQuantity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
        </div>

        {/* Right Column: Schedule & Summary */}
        <div className="space-y-6">
          <Card className="p-5 space-y-4">
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider border-b border-slate-100 pb-2">
              Schedule & Quantities
            </h4>

            <div>
              <span className="text-[11px] text-slate-500 block">Total Quantity to Print</span>
              <p className="text-2xl font-bold text-slate-900 mt-0.5">{job.totalQuantity} units</p>
            </div>

            <div>
              <span className="text-[11px] text-slate-500 block">Expected Delivery Date</span>
              <p className="text-sm font-semibold text-slate-800 mt-0.5 flex items-center gap-1.5">
                <Calendar className="h-4 w-4 text-indigo-600" />
                {job.expectedDeliveryDate ? (
                  new Date(job.expectedDeliveryDate).toLocaleDateString('en-IN', {
                    weekday: 'short',
                    day: '2-digit',
                    month: 'long',
                    year: 'numeric',
                  })
                ) : (
                  'Flexible / To be confirmed'
                )}
              </p>
            </div>

            <div>
              <span className="text-[11px] text-slate-500 block">Assignment Date</span>
              <p className="text-xs text-slate-700 mt-0.5">
                {new Date(job.assignedAt).toLocaleDateString('en-IN', {
                  day: '2-digit',
                  month: 'short',
                  year: 'numeric',
                })}
              </p>
            </div>
          </Card>
        </div>
      </div>

      {/* Student ID Cards & Print Proofs Gallery / Roster */}
      <Card className="p-5 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-slate-100 pb-3">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Printer className="h-4 w-4 text-indigo-600" />
              Student ID Cards & Print Proofs ({job.students?.length || 0})
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Inspect double-sided proofs, verify student data accuracy, or print individual replacement cards.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <div className="inline-flex rounded-lg border border-slate-200 bg-slate-50 p-0.5">
              <button
                type="button"
                onClick={() => setViewMode('grid')}
                className={`p-1.5 rounded-md text-xs transition-colors ${
                  viewMode === 'grid'
                    ? 'bg-white text-indigo-600 shadow-xs font-semibold'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
                title="Grid Gallery View"
              >
                <LayoutGrid className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={() => setViewMode('table')}
                className={`p-1.5 rounded-md text-xs transition-colors ${
                  viewMode === 'table'
                    ? 'bg-white text-indigo-600 shadow-xs font-semibold'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
                title="List Table View"
              >
                <List className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        {/* Search Bar */}
        <div className="relative max-w-md">
          <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
          <input
            type="text"
            placeholder="Search students by name, admission number, roll no, class..."
            value={studentSearch}
            onChange={(e) => setStudentSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        {/* View Mode: Grid Gallery */}
        {viewMode === 'grid' ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {((job.students || []).filter((s) => {
              if (!studentSearch.trim()) return true;
              const q = studentSearch.toLowerCase();
              return (
                (s.studentName || '').toLowerCase().includes(q) ||
                (s.admissionNumber || '').toLowerCase().includes(q) ||
                (s.className || '').toLowerCase().includes(q) ||
                (s.rollNumber || '').toLowerCase().includes(q)
              );
            })).length === 0 ? (
              <div className="col-span-full py-8 text-center text-slate-400 text-xs">
                No student ID cards found matching your search.
              </div>
            ) : (
              (job.students || [])
                .filter((s) => {
                  if (!studentSearch.trim()) return true;
                  const q = studentSearch.toLowerCase();
                  return (
                    (s.studentName || '').toLowerCase().includes(q) ||
                    (s.admissionNumber || '').toLowerCase().includes(q) ||
                    (s.className || '').toLowerCase().includes(q) ||
                    (s.rollNumber || '').toLowerCase().includes(q)
                  );
                })
                .map((s) => (
                  <div
                    key={s.id || s.studentId || s.admissionNumber}
                    className="bg-slate-50/70 border border-slate-200/80 rounded-xl p-3.5 hover:shadow-md transition-all flex flex-col justify-between"
                  >
                    <div className="flex items-start gap-3">
                      <div className="w-12 h-14 rounded-lg bg-indigo-50 border border-indigo-100 flex items-center justify-center font-bold text-indigo-700 text-sm overflow-hidden shrink-0 shadow-xs">
                        {s.photoFileId || s.photo_file_id ? (
                          <img
                            src={`/api/v1/files/${s.photoFileId || s.photo_file_id}${authToken ? `?token=${encodeURIComponent(authToken)}` : ''}`}
                            alt={s.studentName}
                            className="w-full h-full object-cover"
                            onError={(e) => {
                              e.target.style.display = 'none';
                            }}
                          />
                        ) : (
                          (s.studentName || 'S').charAt(0)
                        )}
                      </div>

                      <div className="min-w-0 flex-1">
                        <h4 className="text-xs font-bold text-slate-900 truncate" title={s.studentName}>
                          {s.studentName}
                        </h4>
                        <p className="text-[11px] font-mono text-indigo-600 font-semibold mt-0.5">
                          {s.admissionNumber}
                        </p>
                        <p className="text-[11px] text-slate-500 mt-0.5">
                          Class {s.className}-{s.section} {s.rollNumber ? `• Roll #${s.rollNumber}` : ''}
                        </p>
                      </div>
                    </div>

                    <div className="mt-3 pt-2.5 border-t border-slate-200/60 flex items-center gap-1.5">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setSelectedPreviewStudent(s)}
                        className="flex-1 text-[11px] py-1 h-7 flex items-center justify-center gap-1 text-indigo-700 bg-white hover:bg-indigo-50 border-indigo-200"
                      >
                        <Eye className="w-3 h-3" />
                        Preview
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleDirectPrintSingleStudent(s)}
                        disabled={directPrintingStudentId === (s.studentId || s.id)}
                        className="text-[11px] py-1 h-7 px-2 flex items-center justify-center text-indigo-600 bg-white hover:bg-indigo-50 border-indigo-200"
                        title="Direct print single card to connected printer"
                      >
                        {directPrintingStudentId === (s.studentId || s.id) ? (
                          <LoadingSpinner size="sm" />
                        ) : (
                          <Printer className="w-3 h-3" />
                        )}
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleDownloadSinglePdf(s)}
                        disabled={downloadingStudentId === (s.studentId || s.id)}
                        className="text-[11px] py-1 h-7 px-2 flex items-center justify-center text-slate-700 bg-white hover:bg-slate-100 border-slate-200"
                        title="Download print-ready single student PDF"
                      >
                        {downloadingStudentId === (s.studentId || s.id) ? (
                          <LoadingSpinner size="sm" />
                        ) : (
                          <Download className="w-3 h-3" />
                        )}
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleDownloadSingleImage(s, 'front')}
                        disabled={downloadingImageStudentId === (s.studentId || s.id)}
                        className="text-[11px] py-1 h-7 px-2 flex items-center justify-center text-emerald-600 bg-white hover:bg-emerald-50 border-emerald-200"
                        title="Download Front High-Res JPEG"
                      >
                        {downloadingImageStudentId === (s.studentId || s.id) ? (
                          <LoadingSpinner size="sm" />
                        ) : (
                          <ImageIcon className="w-3 h-3" />
                        )}
                      </Button>
                    </div>
                  </div>
                ))
            )}
          </div>
        ) : (
          /* View Mode: Table */
          <div className="overflow-x-auto border border-slate-200 rounded-xl">
            <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 uppercase font-semibold text-[11px]">
                <tr>
                  <th className="py-2.5 px-3">Student</th>
                  <th className="py-2.5 px-3">Admission No</th>
                  <th className="py-2.5 px-3">Class & Section</th>
                  <th className="py-2.5 px-3">Roll No</th>
                  <th className="py-2.5 px-3">Blood Group</th>
                  <th className="py-2.5 px-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {((job.students || []).filter((s) => {
                  if (!studentSearch.trim()) return true;
                  const q = studentSearch.toLowerCase();
                  return (
                    (s.studentName || '').toLowerCase().includes(q) ||
                    (s.admissionNumber || '').toLowerCase().includes(q) ||
                    (s.className || '').toLowerCase().includes(q) ||
                    (s.rollNumber || '').toLowerCase().includes(q)
                  );
                })).length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-6 text-center text-slate-400 text-xs">
                      No student ID cards found matching your search.
                    </td>
                  </tr>
                ) : (
                  (job.students || [])
                    .filter((s) => {
                      if (!studentSearch.trim()) return true;
                      const q = studentSearch.toLowerCase();
                      return (
                        (s.studentName || '').toLowerCase().includes(q) ||
                        (s.admissionNumber || '').toLowerCase().includes(q) ||
                        (s.className || '').toLowerCase().includes(q) ||
                        (s.rollNumber || '').toLowerCase().includes(q)
                      );
                    })
                    .map((s) => (
                      <tr key={s.id || s.studentId || s.admissionNumber} className="hover:bg-slate-50">
                        <td className="py-2 px-3">
                          <div className="flex items-center gap-2">
                            <div className="w-7 h-7 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center font-bold text-slate-600 text-xs overflow-hidden shrink-0">
                              {s.photoFileId || s.photo_file_id ? (
                                <img
                                  src={`/api/v1/files/${s.photoFileId || s.photo_file_id}${authToken ? `?token=${encodeURIComponent(authToken)}` : ''}`}
                                  alt={s.studentName}
                                  className="w-full h-full object-cover"
                                  onError={(e) => {
                                    e.target.style.display = 'none';
                                  }}
                                />
                              ) : (
                                (s.studentName || 'S').charAt(0)
                              )}
                            </div>
                            <span className="font-semibold text-slate-900">{s.studentName}</span>
                          </div>
                        </td>
                        <td className="py-2 px-3 font-mono text-indigo-600 font-medium">{s.admissionNumber}</td>
                        <td className="py-2 px-3 text-slate-700">{s.className}-{s.section}</td>
                        <td className="py-2 px-3 font-mono text-slate-600">{s.rollNumber || '-'}</td>
                        <td className="py-2 px-3 text-rose-600 font-medium">{s.bloodGroup || '-'}</td>
                        <td className="py-2 px-3 text-right whitespace-nowrap">
                          <div className="inline-flex items-center gap-1.5">
                            <button
                              type="button"
                              onClick={() => setSelectedPreviewStudent(s)}
                              className="inline-flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-indigo-700 bg-indigo-50 border border-indigo-200 rounded-md hover:bg-indigo-100"
                            >
                              <Eye className="w-3 h-3" />
                              Preview
                            </button>
                            <button
                              type="button"
                              onClick={() => handleDirectPrintSingleStudent(s)}
                              disabled={directPrintingStudentId === (s.studentId || s.id)}
                              className="inline-flex items-center gap-1 px-2 py-1 text-[11px] font-medium text-indigo-700 bg-indigo-50 border border-indigo-200 rounded-md hover:bg-indigo-100"
                              title="Direct print single card to connected printer"
                            >
                              {directPrintingStudentId === (s.studentId || s.id) ? (
                                <LoadingSpinner size="sm" />
                              ) : (
                                <Printer className="w-3 h-3" />
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
                              title="Download Front High-Res JPEG"
                            >
                              {downloadingImageStudentId === (s.studentId || s.id) ? (
                                <LoadingSpinner size="sm" />
                              ) : (
                                <ImageIcon className="w-3 h-3" />
                              )}
                              JPEG
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Accept Job Confirmation Modal */}
      <Modal
        isOpen={isAcceptModalOpen}
        onClose={() => setIsAcceptModalOpen(false)}
        title="Confirm Job Acceptance"
        size="sm"
      >
        <div className="space-y-4 text-xs">
          <p className="text-slate-600">
            By accepting Job <strong className="font-mono text-indigo-700">{job.jobNumber}</strong>, your agency commits to printing <strong>{job.totalQuantity} items</strong> by <strong>{job.expectedDeliveryDate || 'the requested timeline'}</strong>.
          </p>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button size="sm" variant="ghost" onClick={() => setIsAcceptModalOpen(false)}>
              Cancel
            </Button>
            <Button
              size="sm"
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
              onClick={() => acceptMutation.mutate()}
              disabled={acceptMutation.isPending}
            >
              {acceptMutation.isPending ? <LoadingSpinner size="sm" className="mr-1" /> : <CheckCircle2 className="h-4 w-4 mr-1.5" />}
              Confirm Acceptance
            </Button>
          </div>
        </div>
      </Modal>

      {/* Reject Job Modal */}
      <Modal
        isOpen={isRejectModalOpen}
        onClose={() => setIsRejectModalOpen(false)}
        title="Reject Job Assignment"
        size="md"
      >
        <div className="space-y-4 text-xs">
          <p className="text-slate-600">
            Please provide a reason for rejecting this print run so the platform administrator can reallocate the batch.
          </p>

          <div>
            <label className="block text-[11px] font-medium text-slate-700 mb-1">
              Rejection Reason <span className="text-rose-500">*</span>
            </label>
            <textarea
              rows="3"
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              placeholder="e.g. Current machine maintenance prevents meeting the required delivery date."
              className="w-full text-xs p-2.5 rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              required
            />
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button size="sm" variant="ghost" onClick={() => setIsRejectModalOpen(false)}>
              Cancel
            </Button>
            <Button
              size="sm"
              variant="outline"
              className="text-rose-600 border-rose-300 hover:bg-rose-50"
              onClick={() => rejectMutation.mutate(rejectionReason.trim())}
              disabled={rejectMutation.isPending || !rejectionReason.trim()}
            >
              {rejectMutation.isPending ? <LoadingSpinner size="sm" className="mr-1" /> : <XCircle className="h-4 w-4 mr-1.5" />}
              Submit Rejection
            </Button>
          </div>
        </div>
      </Modal>

      {/* Single Card Proof Inspection Modal */}
      {selectedPreviewStudent && (
        <SingleCardPreviewModal
          isOpen={!!selectedPreviewStudent}
          onClose={() => setSelectedPreviewStudent(null)}
          student={selectedPreviewStudent}
          cardSpecs={specs}
          jobId={id}
          onDownloadPdf={handleDownloadSinglePdf}
          isDownloadingPdf={downloadingStudentId === (selectedPreviewStudent?.studentId || selectedPreviewStudent?.id)}
          onDirectPrint={handleDirectPrintSingleStudent}
          onDownloadImage={handleDownloadSingleImage}
        />
      )}

      {/* Prepress Imposition & Bulk Printing Studio */}
      {job && (
        <PrintImpositionModal
          isOpen={isImpositionModalOpen}
          onClose={() => setIsImpositionModalOpen(false)}
          role="PRINTING_AGENCY"
          jobId={job.id}
          identifierName={job.jobNumber || 'Job'}
          totalStudents={job.totalQuantity || job.students?.length || 1}
          sampleStudent={job.students?.[0] || null}
          initialSettings={job.printSettings || null}
          allowAgencyOverride={job.allowAgencyOverride || false}
          onSettingsUpdated={() => {
            queryClient.invalidateQueries({ queryKey: ['agency-job-detail', id] });
          }}
        />
      )}

      {/* Submit Quality Check Modal */}
      {job && (
        <SubmitQualityCheckModal
          isOpen={isQcModalOpen}
          onClose={() => setIsQcModalOpen(false)}
          onSubmit={(payload) => submitQcMutation.mutate(payload)}
          isSubmitting={submitQcMutation.isPending}
          maxQuantity={job.totalQuantity || job.students?.length || 100}
          job={job}
          students={job.students}
          cardSpecs={job.cardSpecifications}
        />
      )}
    </div>
  );
}
