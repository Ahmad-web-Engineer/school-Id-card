import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Select from '../../components/common/Select';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import CardCanvas from '../../features/templates/CardCanvas';
import {
  DEFAULT_FRONT_DESIGN,
  DEFAULT_BACK_DESIGN,
  SAMPLE_STUDENT_DATA,
} from '../../features/templates/templatesConstants';
import {
  Palette,
  FileDown,
  UserCheck,
  RotateCw,
  Building,
  CheckCircle2,
  AlertTriangle,
  Layers,
  Sparkles,
  Eye,
} from 'lucide-react';

export default function SchoolTemplatesPage() {
  const [selectedTemplateId, setSelectedTemplateId] = useState(null);
  const [selectedStudentId, setSelectedStudentId] = useState('');
  const [activeSide, setActiveSide] = useState('front'); // 'front' | 'back'
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);
  const [pdfError, setPdfError] = useState(null);
  const [pdfSuccess, setPdfSuccess] = useState(null);

  // 1. Fetch assigned approved templates for this school
  const {
    data: templatesResponse,
    isLoading: isLoadingTemplates,
    isError: isTemplatesError,
    error: templatesError,
    refetch: refetchTemplates,
  } = useQuery({
    queryKey: ['school-templates'],
    queryFn: () => api.get('/school/templates'),
  });

  // 2. Fetch enrolled students for live roster preview
  const { data: studentsResponse, isLoading: isLoadingStudents } = useQuery({
    queryKey: ['school-students-dropdown'],
    queryFn: () => api.get('/school/students', { params: { page: 0, size: 100 } }),
  });

  // 3. Fetch school profile for school name & address
  const { data: schoolProfileResponse } = useQuery({
    queryKey: ['school-profile'],
    queryFn: () => api.get('/school/profile'),
  });

  const templates = templatesResponse?.data || [];
  const students = studentsResponse?.data || [];
  const schoolProfile = schoolProfileResponse?.data;

  // Auto-select first template when loaded
  useEffect(() => {
    if (templates.length > 0 && !selectedTemplateId) {
      setSelectedTemplateId(templates[0].id);
    }
  }, [templates, selectedTemplateId]);

  // Selected template object
  const activeTemplate =
    templates.find((t) => t.id === selectedTemplateId) || templates[0] || null;

  // Parse design elements
  let frontElements = DEFAULT_FRONT_DESIGN.elements;
  let backElements = DEFAULT_BACK_DESIGN.elements;

  if (activeTemplate?.frontDesignJson) {
    try {
      const parsed = typeof activeTemplate.frontDesignJson === 'string'
        ? JSON.parse(activeTemplate.frontDesignJson)
        : activeTemplate.frontDesignJson;
      if (parsed?.elements) frontElements = parsed.elements;
    } catch (e) {
      console.error('Failed to parse front JSON', e);
    }
  }

  if (activeTemplate?.backDesignJson) {
    try {
      const parsed = typeof activeTemplate.backDesignJson === 'string'
        ? JSON.parse(activeTemplate.backDesignJson)
        : activeTemplate.backDesignJson;
      if (parsed?.elements) backElements = parsed.elements;
    } catch (e) {
      console.error('Failed to parse back JSON', e);
    }
  }

  // Find chosen student
  const chosenStudent = students.find((s) => s.id === selectedStudentId);

  // Construct real student preview payload
  const previewStudentData = chosenStudent
    ? {
        student_name: chosenStudent.studentName,
        admission_number: chosenStudent.admissionNumber,
        class: chosenStudent.className,
        section: chosenStudent.section,
        roll_number: chosenStudent.rollNumber,
        father_name: chosenStudent.fatherName || 'Guardian',
        mother_name: chosenStudent.motherName || '',
        blood_group: chosenStudent.bloodGroup || 'O+',
        date_of_birth: chosenStudent.dateOfBirth || '',
        gender: chosenStudent.gender || 'MALE',
        mobile: chosenStudent.mobile || '',
        emergency_contact: chosenStudent.emergencyContact || chosenStudent.mobile || '',
        address: chosenStudent.address || schoolProfile?.address || '',
        school_name: schoolProfile?.schoolName || 'High School',
        school_address: `${schoolProfile?.address || ''}, ${schoolProfile?.city || ''}`,
        academic_session: '2026-2027',
        photoFileId: chosenStudent.photoFileId || chosenStudent.photo_file_id || null,
        photo_url: (chosenStudent.photoFileId || chosenStudent.photo_file_id)
          ? `/api/v1/files/${chosenStudent.photoFileId || chosenStudent.photo_file_id}${localStorage.getItem('auth_token') ? `?token=${encodeURIComponent(localStorage.getItem('auth_token'))}` : ''}`
          : null,
      }
    : SAMPLE_STUDENT_DATA;

  // Download PDF Preview Handler
  const handleDownloadPdf = async () => {
    if (!activeTemplate?.id) return;
    setIsDownloadingPdf(true);
    setPdfError(null);
    setPdfSuccess(null);

    try {
      const params = {};
      if (selectedStudentId) {
        params.studentId = selectedStudentId;
      }

      const response = await api.get(`/school/templates/${activeTemplate.id}/preview`, {
        params,
        responseType: 'blob',
      });

      const blob = new Blob([response], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      const fileName = `card_preview_${
        chosenStudent ? chosenStudent.admissionNumber : 'sample'
      }.pdf`;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      setPdfSuccess('PDF preview downloaded successfully!');
      setTimeout(() => setPdfSuccess(null), 3500);
    } catch (err) {
      setPdfError(err.message || 'Failed to download card PDF preview.');
      setTimeout(() => setPdfError(null), 5000);
    } finally {
      setIsDownloadingPdf(false);
    }
  };

  // Loading State
  if (isLoadingTemplates) {
    return (
      <div className="py-20 flex flex-col items-center justify-center">
        <LoadingSpinner size="lg" className="text-indigo-600 mb-3" />
        <p className="text-sm text-slate-500 font-medium">
          Loading assigned ID card templates...
        </p>
      </div>
    );
  }

  // Error State
  if (isTemplatesError) {
    return (
      <ErrorState
        title="Failed to load templates"
        message={templatesError?.message || 'Unable to fetch assigned templates for this school.'}
        onRetry={refetchTemplates}
      />
    );
  }

  // Empty State
  if (!templates || templates.length === 0) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            Assigned Card Templates
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Preview your institution's approved card designs with live enrolled student profiles.
          </p>
        </div>
        <EmptyState
          title="No Card Templates Assigned Yet"
          description="Your school does not have any approved ID card templates assigned for this academic session. Please contact the platform administrator to assign an approved template."
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            ID Card Templates & Proofing
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1 max-w-3xl">
            Review approved identity card layouts assigned to your school. Select any enrolled student
            from your roster to verify real-time layout alignment, typography, photos, and barcodes before placing production orders.
          </p>
        </div>

        {/* PDF Download Preview Action */}
        <Button
          variant="primary"
          onClick={handleDownloadPdf}
          loading={isDownloadingPdf}
          className="flex items-center gap-2 self-start sm:self-auto shadow-sm"
        >
          <FileDown className="w-4 h-4" />
          <span>Download PDF Proof</span>
        </Button>
      </div>

      {/* Notifications */}
      {pdfSuccess && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-md text-xs text-emerald-800 flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
          <span>{pdfSuccess}</span>
        </div>
      )}
      {pdfError && (
        <div className="p-3 bg-rose-50 border border-rose-200 rounded-md text-xs text-rose-800 flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
          <span>{pdfError}</span>
        </div>
      )}

      {/* Main Grid: Left Catalog & Right Interactive Proof Canvas */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Template Cards Catalog (col-span-4) */}
        <div className="lg:col-span-4 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-slate-800 uppercase tracking-wider text-xs">
              Available Templates ({templates.length})
            </h3>
            <span className="text-[11px] text-slate-400">Approved for Production</span>
          </div>

          <div className="space-y-3">
            {templates.map((tpl) => {
              const isSelected = tpl.id === activeTemplate?.id;
              return (
                <div
                  key={tpl.id}
                  onClick={() => setSelectedTemplateId(tpl.id)}
                  className={`p-4 rounded-lg border cursor-pointer transition-all ${
                    isSelected
                      ? 'border-indigo-600 bg-indigo-50/40 ring-2 ring-indigo-500/20 shadow-sm'
                      : 'border-slate-200 bg-white hover:border-indigo-300 hover:bg-slate-50/70'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2.5">
                      <div
                        className={`w-9 h-9 rounded-md flex items-center justify-center shrink-0 border ${
                          isSelected
                            ? 'bg-indigo-600 text-white border-indigo-700'
                            : 'bg-slate-100 text-slate-600 border-slate-200'
                        }`}
                      >
                        <Palette className="w-5 h-5" />
                      </div>
                      <div>
                        <h4 className="text-sm font-bold text-slate-900 leading-snug">
                          {tpl.templateName}
                        </h4>
                        <span className="text-xs text-slate-500">
                          {tpl.templateCategory || 'STUDENT'} Card
                        </span>
                      </div>
                    </div>
                    <Badge variant="success" size="sm">
                      APPROVED
                    </Badge>
                  </div>

                  <div className="mt-3 pt-3 border-t border-slate-200/60 flex items-center justify-between text-xs text-slate-500">
                    <span>
                      Size: {tpl.cardWidthMm || 85.6} × {tpl.cardHeightMm || 54.0} mm
                    </span>
                    <span className="font-mono font-medium text-slate-600 bg-slate-100 px-1.5 py-0.5 rounded">
                      v{tpl.versionNumber || 1}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Student Selector Card */}
          <Card className="p-4 bg-white border border-slate-200">
            <h4 className="text-xs font-semibold text-slate-800 uppercase tracking-wider mb-2.5 flex items-center gap-1.5">
              <UserCheck className="w-4 h-4 text-indigo-600" />
              <span>Live Student Proofing</span>
            </h4>
            <p className="text-xs text-slate-500 mb-3">
              Switch between enrolled students to see how real photos, long names, and roll numbers render on this template.
            </p>

            <select
              value={selectedStudentId}
              onChange={(e) => setSelectedStudentId(e.target.value)}
              className="w-full text-xs rounded-md border border-slate-300 py-2 px-3 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white text-slate-800"
            >
              <option value="">Standard Sample Data (Aarav Sharma)</option>
              {students.map((stu) => (
                <option key={stu.id} value={stu.id}>
                  {stu.studentName} ({stu.admissionNumber} - Class {stu.className}{stu.section ? `-${stu.section}` : ''})
                </option>
              ))}
            </select>

            {chosenStudent && (
              <div className="mt-3 p-2.5 bg-slate-50 rounded-md border border-slate-100 text-[11px] text-slate-600 space-y-1">
                <div className="flex justify-between">
                  <span className="text-slate-400">Admission No:</span>
                  <strong className="text-slate-700 font-mono">{chosenStudent.admissionNumber}</strong>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Class & Section:</span>
                  <span className="text-slate-700 font-medium">
                    {chosenStudent.className} - {chosenStudent.section || '—'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Photo Attached:</span>
                  <span className={chosenStudent.photoFileId ? 'text-emerald-600 font-medium' : 'text-amber-600 font-medium'}>
                    {chosenStudent.photoFileId ? 'Yes (Live Portrait)' : 'Placeholder Avatar'}
                  </span>
                </div>
              </div>
            )}
          </Card>
        </div>

        {/* Right Column: Visual Card Canvas Preview (col-span-8) */}
        <div className="lg:col-span-8">
          <Card className="p-4 sm:p-6 bg-white border border-slate-200">
            {/* Action Bar: Front / Back Switcher & Full Info */}
            <div className="flex flex-wrap items-center justify-between gap-3 pb-4 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="flex bg-slate-100 p-0.5 rounded-lg border border-slate-200">
                  <button
                    type="button"
                    onClick={() => setActiveSide('front')}
                    className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-all ${
                      activeSide === 'front'
                        ? 'bg-white text-indigo-700 shadow-xs'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    Front View
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveSide('back')}
                    className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-all ${
                      activeSide === 'back'
                        ? 'bg-white text-indigo-700 shadow-xs'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    Back View
                  </button>
                </div>

                <Badge variant="brand" size="sm">
                  {activeSide === 'front' ? 'Front Side Design' : 'Back Side Design'}
                </Badge>
              </div>

              <div className="text-xs text-slate-500 flex items-center gap-2">
                <span className="hidden sm:inline">CR80 Physical Dimensions:</span>
                <strong className="text-slate-800">
                  {activeTemplate?.cardWidthMm || 85.6} × {activeTemplate?.cardHeightMm || 54.0} mm
                </strong>
              </div>
            </div>

            {/* Visual Canvas Display */}
            <div className="my-6 bg-slate-50/70 rounded-xl p-4 sm:p-8 flex items-center justify-center border border-slate-200/60 overflow-x-auto min-h-[360px]">
              <CardCanvas
                widthMm={activeTemplate?.cardWidthMm || 85.6}
                heightMm={activeTemplate?.cardHeightMm || 54.0}
                bleedMarginMm={3.0}
                showBleed={false}
                elements={activeSide === 'front' ? frontElements : backElements}
                interactive={false}
                previewData={previewStudentData}
                useSampleData={true}
                scale={5.2}
              />
            </div>

            {/* Proofing Guidance Banner */}
            <div className="p-3 bg-indigo-50/70 border border-indigo-100 rounded-lg text-xs text-indigo-900 flex items-start gap-2.5">
              <Sparkles className="w-4 h-4 text-indigo-600 shrink-0 mt-0.5" />
              <div>
                <strong className="block font-semibold">Print-Ready Quality Guarantee</strong>
                <span>
                  The card shown above precisely simulates final thermal dye-sublimation / UV inkjet
                  printing specs. When placing print orders in Step 5, cards will be rendered at 300 DPI vector resolution.
                </span>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
