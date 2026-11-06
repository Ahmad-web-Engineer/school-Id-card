import React, { useState, useMemo } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import CardCanvas from './CardCanvas';
import { Download, CreditCard, User, CheckCircle, Eye, AlertCircle, Printer, Image as ImageIcon } from 'lucide-react';
import { directPrintUrl } from '../../utils/printHelper';
import api from '../../services/api';

export default function SingleCardPreviewModal({
  isOpen,
  onClose,
  student,
  cardSpecs,
  jobId,
  onDownloadPdf,
  isDownloadingPdf = false,
  onDirectPrint,
  onDownloadImage,
}) {
  const [activeSide, setActiveSide] = useState('front'); // 'front' | 'back'
  const [isDirectPrinting, setIsDirectPrinting] = useState(false);
  const [isDownloadingImage, setIsDownloadingImage] = useState(false);

  const handleTriggerDirectPrint = async () => {
    if (onDirectPrint) {
      onDirectPrint(student);
      return;
    }
    if (!jobId) return;
    try {
      setIsDirectPrinting(true);
      const studentId = student.studentId || student.id;
      await directPrintUrl(`/agency/jobs/${jobId}/students/${studentId}/print-pdf`);
    } catch (err) {
      console.error('Failed to direct print student card:', err);
    } finally {
      setIsDirectPrinting(false);
    }
  };

  const handleTriggerDownloadImage = async () => {
    if (onDownloadImage) {
      onDownloadImage(student, activeSide);
      return;
    }
    if (!jobId) return;
    try {
      setIsDownloadingImage(true);
      const studentId = student.studentId || student.id;
      const res = await api.get(`/agency/jobs/${jobId}/students/${studentId}/download-image?side=${activeSide}`, { responseType: 'blob', timeout: 60000 });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'image/jpeg' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${student.admissionNumber || 'card'}_${activeSide}.jpg`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to download student JPEG image:', err);
    } finally {
      setIsDownloadingImage(false);
    }
  };

  const { frontElements, frontBgColor } = useMemo(() => {
    if (!cardSpecs?.frontDesignJson) return { frontElements: [], frontBgColor: '#ffffff' };
    try {
      const parsed = typeof cardSpecs.frontDesignJson === 'string'
        ? JSON.parse(cardSpecs.frontDesignJson)
        : cardSpecs.frontDesignJson;
      const elements = Array.isArray(parsed) ? parsed : (parsed?.elements || []);
      const bgColor = parsed?.backgroundColor || '#ffffff';
      return { frontElements: elements, frontBgColor: bgColor };
    } catch (e) {
      console.warn('Failed to parse frontDesignJson', e);
      return { frontElements: [], frontBgColor: '#ffffff' };
    }
  }, [cardSpecs?.frontDesignJson]);

  const { backElements, backBgColor } = useMemo(() => {
    if (!cardSpecs?.backDesignJson) return { backElements: [], backBgColor: '#f8fafc' };
    try {
      const parsed = typeof cardSpecs.backDesignJson === 'string'
        ? JSON.parse(cardSpecs.backDesignJson)
        : cardSpecs.backDesignJson;
      const elements = Array.isArray(parsed) ? parsed : (parsed?.elements || []);
      const bgColor = parsed?.backgroundColor || '#f8fafc';
      return { backElements: elements, backBgColor: bgColor };
    } catch (e) {
      console.warn('Failed to parse backDesignJson', e);
      return { backElements: [], backBgColor: '#f8fafc' };
    }
  }, [cardSpecs?.backDesignJson]);

  if (!student) return null;

  const currentElements = activeSide === 'front' ? frontElements : backElements;
  const currentBgColor = activeSide === 'front' ? frontBgColor : backBgColor;
  const widthMm = Number(cardSpecs?.widthMm || cardSpecs?.cardWidthMm || 85.6);
  const heightMm = Number(cardSpecs?.heightMm || cardSpecs?.cardHeightMm || 54.0);
  const bleedMarginMm = Number(cardSpecs?.bleedMarginMm || 3.0);
  const authToken = localStorage.getItem('auth_token') || localStorage.getItem('token') || '';

  // Normalise student snapshot fields for CardCanvas preview
  const previewStudent = {
    ...student,
    studentName: student.studentName || 'Student Name',
    admissionNumber: student.admissionNumber || 'ADM-0000',
    className: student.className || '10',
    section: student.section || 'A',
    rollNumber: student.rollNumber || '01',
    fatherName: student.fatherName || '-',
    motherName: student.motherName || '-',
    dateOfBirth: student.dateOfBirth || '-',
    bloodGroup: student.bloodGroup || '-',
    emergencyContact: student.emergencyContact || '-',
    address: student.address || '-',
    photoFileId: student.photoFileId || student.photo_file_id || student.photoId || null,
    photoUrl: student.photoUrl || student.photo_url || null,
    schoolLogoFileId: cardSpecs?.schoolLogoFileId || student?.schoolLogoFileId || student?.school_logo_file_id || cardSpecs?.logoFileId || null,
    signatureFileId: cardSpecs?.signatureFileId || student?.signatureFileId || student?.signature_file_id || null,
    schoolName: cardSpecs?.schoolName || student?.schoolName || 'Greenwood International School',
    schoolCode: cardSpecs?.schoolCode || student?.schoolCode || 'GREENWOOD',
    schoolAddress: cardSpecs?.schoolAddress || student?.schoolAddress || 'Sarjapur Main Road, Bengaluru',
    schoolPhone: cardSpecs?.schoolPhone || student?.schoolPhone || '+91 80 2844 0000',
    schoolEmail: cardSpecs?.schoolEmail || student?.schoolEmail || 'office@greenwoodhigh.edu.in',
    schoolWebsite: cardSpecs?.schoolWebsite || student?.schoolWebsite || 'www.greenwoodhigh.edu.in',
    affiliationNumber: cardSpecs?.affiliationNumber || student?.affiliationNumber || 'CBSE/AFF/2026/089',
    tagline: cardSpecs?.tagline || student?.tagline || 'Rooted in Values, Soaring in Excellence',
    principalName: cardSpecs?.principalName || student?.principalName || 'Dr. Rajesh Verma',
    academicSession: cardSpecs?.academicSession || student?.academicSession || '2026-2027',
    validUntil: cardSpecs?.validUntil || student?.validUntil || '31-03-2027',
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Card Proof — ${previewStudent.studentName} (${previewStudent.admissionNumber})`}
      size="xl"
    >
      <div className="space-y-5">
        {/* Top Control Bar: Side Switcher & Specs Badge */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-3 border-b border-slate-200">
          <div className="inline-flex rounded-lg border border-slate-200 bg-slate-50 p-1">
            <button
              type="button"
              onClick={() => setActiveSide('front')}
              className={`flex items-center gap-1.5 px-4 py-1.5 text-xs font-semibold rounded-md transition-all ${
                activeSide === 'front'
                  ? 'bg-white text-indigo-600 shadow-sm border border-slate-200/80'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <CreditCard className="w-3.5 h-3.5" />
              Front Side
            </button>
            <button
              type="button"
              onClick={() => setActiveSide('back')}
              className={`flex items-center gap-1.5 px-4 py-1.5 text-xs font-semibold rounded-md transition-all ${
                activeSide === 'back'
                  ? 'bg-white text-indigo-600 shadow-sm border border-slate-200/80'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <CreditCard className="w-3.5 h-3.5 rotate-180" />
              Back Side
            </button>
          </div>

          <div className="flex items-center gap-2 text-xs text-slate-500">
            <span className="inline-flex items-center px-2 py-0.5 rounded bg-slate-100 text-slate-700 font-mono text-[11px]">
              {widthMm}mm × {heightMm}mm
            </span>
            <span className="inline-flex items-center px-2 py-0.5 rounded bg-amber-50 text-amber-700 border border-amber-200 font-mono text-[11px]">
              {bleedMarginMm}mm Bleed
            </span>
            {previewStudent.photoFileId ? (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 border border-emerald-200 text-[11px]">
                <CheckCircle className="w-3 h-3" /> Photo Attached
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-slate-100 text-slate-500 text-[11px]">
                <User className="w-3 h-3" /> Default Avatar
              </span>
            )}
          </div>
        </div>

        {/* Modal Main Content: Visual Canvas + Student Metadata */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Card Canvas Visualizer (Col 7) */}
          <div className="lg:col-span-7 flex flex-col items-center justify-center p-6 bg-slate-100 rounded-xl border border-slate-200/80 min-h-[340px] shadow-inner overflow-hidden">
            <div className="text-[11px] font-medium text-slate-400 uppercase tracking-wider mb-2">
              Live Preview • {activeSide === 'front' ? 'Front Side (Page 1)' : 'Back Side (Page 2)'}
            </div>

            <div className="p-2 bg-white rounded-lg shadow-md border border-slate-300/60 overflow-hidden max-w-full">
              <CardCanvas
                widthMm={widthMm}
                heightMm={heightMm}
                bleedMarginMm={bleedMarginMm}
                elements={currentElements}
                backgroundColor={currentBgColor}
                previewData={previewStudent}
                useSampleData={false}
                interactive={false}
                scale={4.0}
              />
            </div>

            <p className="text-[11px] text-slate-400 mt-3 text-center">
              Dashed red border represents the 3mm print cutting & bleed safety margin.
            </p>
          </div>

          {/* Student Profile & Metadata Sheet (Col 5) */}
          <div className="lg:col-span-5 bg-white border border-slate-200 rounded-xl p-4 shadow-sm space-y-4">
            <div className="flex items-center gap-3 pb-3 border-b border-slate-100">
              <div className="w-12 h-12 rounded-lg bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-700 font-bold text-base overflow-hidden">
                {previewStudent.photoFileId ? (
                  <img
                    src={`/api/v1/files/${previewStudent.photoFileId}${authToken ? `?token=${encodeURIComponent(authToken)}` : ''}`}
                    alt={previewStudent.studentName}
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      e.target.style.display = 'none';
                    }}
                  />
                ) : (
                  previewStudent.studentName.charAt(0)
                )}
              </div>
              <div className="min-w-0 flex-1">
                <h4 className="text-sm font-bold text-slate-900 truncate">
                  {previewStudent.studentName}
                </h4>
                <p className="text-xs font-mono text-indigo-600 font-medium">
                  {previewStudent.admissionNumber}
                </p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-x-3 gap-y-2 text-xs">
              <div>
                <span className="text-slate-400 block text-[11px]">Class & Section</span>
                <span className="font-semibold text-slate-800">
                  {previewStudent.className} - {previewStudent.section}
                </span>
              </div>
              <div>
                <span className="text-slate-400 block text-[11px]">Roll Number</span>
                <span className="font-semibold text-slate-800 font-mono">
                  {previewStudent.rollNumber || '-'}
                </span>
              </div>
              <div>
                <span className="text-slate-400 block text-[11px]">Date of Birth</span>
                <span className="font-semibold text-slate-800">
                  {previewStudent.dateOfBirth || '-'}
                </span>
              </div>
              <div>
                <span className="text-slate-400 block text-[11px]">Blood Group</span>
                <span className="font-semibold text-rose-600">
                  {previewStudent.bloodGroup || '-'}
                </span>
              </div>
              <div className="col-span-2">
                <span className="text-slate-400 block text-[11px]">Father / Guardian</span>
                <span className="font-medium text-slate-800">
                  {previewStudent.fatherName || '-'}
                </span>
              </div>
              <div className="col-span-2">
                <span className="text-slate-400 block text-[11px]">Emergency Contact</span>
                <span className="font-medium text-slate-800 font-mono">
                  {previewStudent.emergencyContact || '-'}
                </span>
              </div>
            </div>

            {cardSpecs?.schoolName && (
              <div className="pt-3 border-t border-slate-100 text-xs">
                <span className="text-slate-400 block text-[11px]">Assigned School</span>
                <span className="font-medium text-slate-700">{cardSpecs.schoolName}</span>
              </div>
            )}
          </div>
        </div>

        {/* Footer Actions */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-slate-200">
          <Button variant="outline" onClick={onClose}>
            Close
          </Button>

          <div className="flex flex-wrap items-center gap-2">
            {(onDirectPrint || jobId) && (
              <Button
                variant="outline"
                onClick={handleTriggerDirectPrint}
                disabled={isDirectPrinting}
                className="flex items-center gap-1.5 text-slate-700 hover:bg-slate-50 border-slate-300"
              >
                <Printer className="w-4 h-4 text-indigo-600" />
                {isDirectPrinting ? 'Sending to Printer...' : 'Direct Print'}
              </Button>
            )}

            {(onDownloadImage || jobId) && (
              <Button
                variant="outline"
                onClick={handleTriggerDownloadImage}
                disabled={isDownloadingImage}
                className="flex items-center gap-1.5 text-slate-700 hover:bg-slate-50 border-slate-300"
                title={`Download ${activeSide === 'front' ? 'Front' : 'Back'} Side High-Res JPEG`}
              >
                <ImageIcon className="w-4 h-4 text-emerald-600" />
                {isDownloadingImage ? 'Exporting JPEG...' : `Download ${activeSide === 'front' ? 'Front' : 'Back'} JPEG`}
              </Button>
            )}

            {onDownloadPdf && (
              <Button
                variant="primary"
                onClick={() => onDownloadPdf(student)}
                disabled={isDownloadingPdf}
                className="flex items-center gap-2"
              >
                <Download className="w-4 h-4" />
                {isDownloadingPdf ? 'Generating PDF...' : 'Download Card PDF'}
              </Button>
            )}
          </div>
        </div>
      </div>
    </Modal>
  );
}
