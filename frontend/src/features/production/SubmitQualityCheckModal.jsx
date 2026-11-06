import React, { useState, useEffect, useMemo } from 'react';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Textarea from '../../components/common/Textarea';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import SingleCardPreviewModal from '../templates/SingleCardPreviewModal';
import StudentPhotoAvatar from '../students/StudentPhotoAvatar';
import { DEFECT_CATEGORIES } from './productionConstants';
import {
  CheckCircle,
  XCircle,
  AlertTriangle,
  Search,
  Eye,
  CheckCheck,
  RotateCcw,
  User,
  ShieldCheck,
  FileText,
} from 'lucide-react';

export default function SubmitQualityCheckModal({
  isOpen,
  onClose,
  onSubmit,
  isSubmitting,
  maxQuantity = 100,
  assignments = [],
  selectedAssignmentId = '',
  onSelectAssignment,
  job = null,
  assignment = null,
  students: propStudents = null,
  cardSpecs: propCardSpecs = null,
  isLoadingStudents = false,
}) {
  // Derive effective active job/assignment data
  const activeData = job || assignment || assignments.find((a) => a.id === selectedAssignmentId);
  const rawStudents = propStudents || activeData?.students || [];
  const cardSpecs = propCardSpecs || activeData?.cardSpecifications || activeData?.specs || null;

  // Search filter inside modal roster
  const [searchQuery, setSearchQuery] = useState('');

  // Per-card inspection status map: { [studentId]: { status: 'PASSED' | 'DEFECT', category: '...', note: '...' } }
  const [cardChecks, setCardChecks] = useState({});

  // Fallback manual quantities (used when students array is empty)
  const [manualInspected, setManualInspected] = useState(maxQuantity > 0 ? maxQuantity : 100);
  const [manualAccepted, setManualAccepted] = useState(maxQuantity > 0 ? maxQuantity : 100);
  const [manualRejected, setManualRejected] = useState(0);

  // General notes & defects
  const [fallbackDefectCategory, setFallbackDefectCategory] = useState('PRINT_ALIGNMENT');
  const [generalNotes, setGeneralNotes] = useState('');
  const [error, setError] = useState('');

  // Active student card preview modal
  const [previewingStudent, setPreviewingStudent] = useState(null);

  // Initialize checks when modal opens or student list changes
  useEffect(() => {
    if (isOpen) {
      setError('');
      setSearchQuery('');
      setPreviewingStudent(null);
      setGeneralNotes('');

      if (rawStudents && rawStudents.length > 0) {
        // Initial state: default all students to PASSED for streamlined QC workflow
        const initialMap = {};
        rawStudents.forEach((s) => {
          const sId = s.id || s.studentId || s.admissionNumber;
          initialMap[sId] = {
            status: 'PASSED',
            category: 'PRINT_ALIGNMENT',
            note: '',
          };
        });
        setCardChecks(initialMap);
      } else {
        const total = activeData?.totalQuantity || maxQuantity || 100;
        setManualInspected(total);
        setManualAccepted(total);
        setManualRejected(0);
      }
    }
  }, [isOpen, rawStudents?.length, activeData?.id]);

  // Computed summary
  const hasStudents = rawStudents && rawStudents.length > 0;

  const { inspectedQty, acceptedQty, rejectedQty, defectList } = useMemo(() => {
    if (!hasStudents) {
      const ins = parseInt(manualInspected, 10) || 0;
      const acc = parseInt(manualAccepted, 10) || 0;
      const rej = parseInt(manualRejected, 10) || 0;
      return { inspectedQty: ins, acceptedQty: acc, rejectedQty: rej, defectList: [] };
    }

    let ins = 0;
    let acc = 0;
    let rej = 0;
    const defects = [];

    rawStudents.forEach((s) => {
      const sId = s.id || s.studentId || s.admissionNumber;
      const check = cardChecks[sId];
      if (check?.status) {
        ins += 1;
        if (check.status === 'PASSED') {
          acc += 1;
        } else if (check.status === 'DEFECT') {
          rej += 1;
          defects.push({
            student: s,
            category: check.category || 'PRINT_ALIGNMENT',
            note: check.note || 'Quality defect flagged during manual check',
          });
        }
      }
    });

    return { inspectedQty: ins, acceptedQty: acc, rejectedQty: rej, defectList: defects };
  }, [hasStudents, rawStudents, cardChecks, manualInspected, manualAccepted, manualRejected]);

  // Bulk actions
  const handleMarkAllPassed = () => {
    const updated = {};
    rawStudents.forEach((s) => {
      const sId = s.id || s.studentId || s.admissionNumber;
      updated[sId] = {
        status: 'PASSED',
        category: 'PRINT_ALIGNMENT',
        note: '',
      };
    });
    setCardChecks(updated);
  };

  const handleResetAllChecks = () => {
    setCardChecks({});
  };

  // Single card check actions
  const handleSetStudentStatus = (sId, status) => {
    setCardChecks((prev) => ({
      ...prev,
      [sId]: {
        ...(prev[sId] || { category: 'PRINT_ALIGNMENT', note: '' }),
        status,
      },
    }));
  };

  const handleUpdateStudentDefect = (sId, field, value) => {
    setCardChecks((prev) => ({
      ...prev,
      [sId]: {
        ...(prev[sId] || { status: 'DEFECT' }),
        [field]: value,
      },
    }));
  };

  // Filter students for display
  const filteredStudents = useMemo(() => {
    if (!hasStudents) return [];
    if (!searchQuery.trim()) return rawStudents;
    const q = searchQuery.toLowerCase();
    return rawStudents.filter((s) => {
      const name = (s.studentName || '').toLowerCase();
      const adm = (s.admissionNumber || '').toLowerCase();
      const cls = (s.className || '').toLowerCase();
      const roll = (s.rollNumber || '').toLowerCase();
      return name.includes(q) || adm.includes(q) || cls.includes(q) || roll.includes(q);
    });
  }, [hasStudents, rawStudents, searchQuery]);

  // Submit Handler
  const handleSubmit = (e) => {
    e.preventDefault();

    if (inspectedQty <= 0) {
      setError('At least 1 card must be inspected before submitting report.');
      return;
    }

    if (inspectedQty !== acceptedQty + rejectedQty) {
      setError(`Inspected (${inspectedQty}) must equal sum of accepted (${acceptedQty}) and rejected (${rejectedQty}).`);
      return;
    }

    let primaryDefectCategory = null;
    let defectDescription = null;

    if (rejectedQty > 0) {
      if (hasStudents) {
        primaryDefectCategory = defectList[0]?.category || fallbackDefectCategory;
        const details = defectList
          .map((d) => `${d.student.studentName} (${d.student.admissionNumber}): [${d.category}] ${d.note || 'Defective'}`)
          .join('; ');
        defectDescription = details.length > 500 ? details.slice(0, 497) + '...' : details;
      } else {
        primaryDefectCategory = fallbackDefectCategory;
        defectDescription = generalNotes.trim() || 'Print defect flagged during batch inspection';
      }
    }

    setError('');
    onSubmit({
      inspectedQuantity: inspectedQty,
      acceptedQuantity: acceptedQty,
      rejectedQuantity: rejectedQty,
      defectCategory: rejectedQty > 0 ? primaryDefectCategory : null,
      defectDescription: rejectedQty > 0 ? defectDescription : null,
      notes: generalNotes.trim() || null,
    });
  };

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={onClose}
        title="Quality Inspection & Verification Report"
        size="2xl"
      >
        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Assignment Selector (if multiple available) */}
          {assignments && assignments.length > 0 && !job && (
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                Target Production Assignment <span className="text-rose-500">*</span>
              </label>
              <select
                value={selectedAssignmentId}
                onChange={(e) => onSelectAssignment?.(e.target.value)}
                className="w-full text-xs font-medium border border-slate-300 rounded-lg py-2 px-3 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 shadow-sm"
                required
              >
                <option value="">-- Choose an Assignment --</option>
                {assignments.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.assignmentNumber || a.jobNumber || a.orderNumber} — {a.agencyName || 'Agency'} ({a.totalAssignedQuantity || a.totalQuantity || 0} cards) [{a.assignmentStatus}]
                  </option>
                ))}
              </select>
            </div>
          )}

          {error && (
            <div className="p-3 text-xs text-rose-700 bg-rose-50 border border-rose-200 rounded-lg flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 shrink-0 text-rose-600" />
              <span>{error}</span>
            </div>
          )}

          {/* Inspection Summary Stats Bar */}
          <div className="grid grid-cols-3 gap-3 p-3 bg-slate-50 border border-slate-200/80 rounded-xl text-center">
            <div className="p-2 bg-white rounded-lg border border-slate-100 shadow-xs">
              <div className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                Total Inspected
              </div>
              <div className="text-xl font-bold text-slate-900 mt-0.5">
                {inspectedQty}
                {hasStudents && <span className="text-xs font-normal text-slate-400 ml-1">/ {rawStudents.length}</span>}
              </div>
            </div>

            <div className="p-2 bg-emerald-50/60 rounded-lg border border-emerald-100 shadow-xs">
              <div className="text-[11px] font-semibold text-emerald-700 uppercase tracking-wider flex items-center justify-center gap-1">
                <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                Accepted Cards
              </div>
              <div className="text-xl font-bold text-emerald-700 mt-0.5">{acceptedQty}</div>
            </div>

            <div className="p-2 bg-rose-50/60 rounded-lg border border-rose-100 shadow-xs">
              <div className="text-[11px] font-semibold text-rose-700 uppercase tracking-wider flex items-center justify-center gap-1">
                <XCircle className="w-3.5 h-3.5 text-rose-600" />
                Rejected (Defects)
              </div>
              <div className="text-xl font-bold text-rose-700 mt-0.5">{rejectedQty}</div>
            </div>
          </div>

          {/* Interactive Student Checklist Mode (When student snapshots are present) */}
          {isLoadingStudents ? (
            <div className="py-12 flex flex-col items-center justify-center space-y-2 bg-slate-50 border border-slate-200/80 rounded-xl text-slate-500">
              <LoadingSpinner size="md" />
              <span className="text-xs font-medium">Loading student cards for quality verification...</span>
            </div>
          ) : hasStudents ? (
            <div className="space-y-3">
              {/* Toolbar: Search + Quick Pass All Button */}
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pt-1">
                <div className="relative flex-1">
                  <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
                  <input
                    type="text"
                    placeholder="Search by student name, admission number, roll no..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full pl-9 pr-3 py-1.5 text-xs border border-slate-300 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-indigo-500 shadow-xs"
                  />
                </div>

                <div className="flex items-center gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={handleMarkAllPassed}
                    className="text-xs py-1.5 px-3 flex items-center gap-1.5 border-emerald-300 text-emerald-700 hover:bg-emerald-50 bg-emerald-50/30"
                  >
                    <CheckCheck className="w-3.5 h-3.5 text-emerald-600" />
                    Pass All ({rawStudents.length})
                  </Button>

                  <Button
                    type="button"
                    variant="outline"
                    onClick={handleResetAllChecks}
                    className="text-xs py-1.5 px-2.5 text-slate-600 hover:bg-slate-100"
                    title="Reset all checkmarks"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                  </Button>
                </div>
              </div>

              {/* Student Cards Roster Table */}
              <div className="border border-slate-200 rounded-xl overflow-hidden shadow-xs max-h-[320px] overflow-y-auto">
                <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
                  <thead className="bg-slate-100/80 sticky top-0 z-10 text-slate-600 uppercase tracking-wider font-semibold text-[11px]">
                    <tr>
                      <th className="py-2.5 px-3">Student Details</th>
                      <th className="py-2.5 px-3">Class / Roll</th>
                      <th className="py-2.5 px-3 text-center">Card Proof</th>
                      <th className="py-2.5 px-3 text-right">Inspection Verdict</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {filteredStudents.length === 0 ? (
                      <tr>
                        <td colSpan={4} className="py-8 text-center text-slate-400 text-xs">
                          No students match your filter criteria.
                        </td>
                      </tr>
                    ) : (
                      filteredStudents.map((s) => {
                        const sId = s.id || s.studentId || s.admissionNumber;
                        const check = cardChecks[sId] || { status: null, category: 'PRINT_ALIGNMENT', note: '' };
                        const isPassed = check.status === 'PASSED';
                        const isDefect = check.status === 'DEFECT';

                        return (
                          <React.Fragment key={sId}>
                            <tr className={`transition-colors ${isDefect ? 'bg-rose-50/40' : isPassed ? 'bg-emerald-50/20' : 'hover:bg-slate-50'}`}>
                              <td className="py-2 px-3">
                                <div className="flex items-center gap-2.5">
                                  <StudentPhotoAvatar
                                    photoFileId={s.photoFileId || s.photo_file_id}
                                    name={s.studentName}
                                    size="sm"
                                  />
                                  <div className="min-w-0">
                                    <div className="font-semibold text-slate-900 truncate">
                                      {s.studentName}
                                    </div>
                                    <div className="text-[11px] font-mono text-slate-500">
                                      {s.admissionNumber}
                                    </div>
                                  </div>
                                </div>
                              </td>

                              <td className="py-2 px-3 whitespace-nowrap text-slate-700">
                                <div>{s.className} - {s.section}</div>
                                <div className="text-[11px] text-slate-400 font-mono">Roll: {s.rollNumber || '-'}</div>
                              </td>

                              <td className="py-2 px-3 text-center whitespace-nowrap">
                                <button
                                  type="button"
                                  onClick={() => setPreviewingStudent(s)}
                                  className="inline-flex items-center gap-1 px-2.5 py-1 text-[11px] font-medium text-indigo-700 bg-indigo-50 border border-indigo-200/80 rounded-md hover:bg-indigo-100 transition-colors"
                                >
                                  <Eye className="w-3 h-3" />
                                  Preview Card
                                </button>
                              </td>

                              <td className="py-2 px-3 text-right whitespace-nowrap">
                                <div className="inline-flex items-center gap-1.5">
                                  <button
                                    type="button"
                                    onClick={() => handleSetStudentStatus(sId, 'PASSED')}
                                    className={`px-3 py-1 text-xs font-semibold rounded-md transition-all flex items-center gap-1 ${
                                      isPassed
                                        ? 'bg-emerald-600 text-white shadow-xs'
                                        : 'bg-slate-100 text-slate-600 hover:bg-emerald-50 hover:text-emerald-700 border border-slate-200'
                                    }`}
                                  >
                                    <CheckCircle className="w-3 h-3" />
                                    Pass
                                  </button>

                                  <button
                                    type="button"
                                    onClick={() => handleSetStudentStatus(sId, 'DEFECT')}
                                    className={`px-3 py-1 text-xs font-semibold rounded-md transition-all flex items-center gap-1 ${
                                      isDefect
                                        ? 'bg-rose-600 text-white shadow-xs'
                                        : 'bg-slate-100 text-slate-600 hover:bg-rose-50 hover:text-rose-700 border border-slate-200'
                                    }`}
                                  >
                                    <XCircle className="w-3 h-3" />
                                    Defect
                                  </button>
                                </div>
                              </td>
                            </tr>

                            {/* Inline Defect Annotator */}
                            {isDefect && (
                              <tr className="bg-rose-50/70 border-b border-rose-200/60">
                                <td colSpan={4} className="py-2 px-4">
                                  <div className="flex flex-col sm:flex-row items-center gap-3">
                                    <span className="text-[11px] font-bold text-rose-800 uppercase tracking-wider shrink-0">
                                      Flag Defect:
                                    </span>
                                    <div className="w-full sm:w-48 shrink-0">
                                      <select
                                        value={check.category || 'PRINT_ALIGNMENT'}
                                        onChange={(e) => handleUpdateStudentDefect(sId, 'category', e.target.value)}
                                        className="w-full text-xs font-medium border border-rose-300 rounded-md py-1 px-2 bg-white text-slate-800 focus:outline-none focus:ring-1 focus:ring-rose-500"
                                      >
                                        {DEFECT_CATEGORIES.map((cat) => (
                                          <option key={cat.value} value={cat.value}>
                                            {cat.label}
                                          </option>
                                        ))}
                                      </select>
                                    </div>
                                    <input
                                      type="text"
                                      placeholder="Specific flaw (e.g. alignment shift, blurry photo, typo)..."
                                      value={check.note || ''}
                                      onChange={(e) => handleUpdateStudentDefect(sId, 'note', e.target.value)}
                                      className="w-full text-xs border border-rose-300 rounded-md py-1 px-2.5 bg-white text-slate-800 focus:outline-none focus:ring-1 focus:ring-rose-500"
                                    />
                                  </div>
                                </td>
                              </tr>
                            )}
                          </React.Fragment>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          ) : (
            /* Fallback Numeric Input View (If no student snapshots available) */
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                  Total Inspected <span className="text-rose-500">*</span>
                </label>
                <Input
                  type="number"
                  min="1"
                  value={manualInspected}
                  onChange={(e) => {
                    setManualInspected(e.target.value);
                    const ins = parseInt(e.target.value, 10) || 0;
                    const rej = parseInt(manualRejected, 10) || 0;
                    setManualAccepted(Math.max(0, ins - rej));
                  }}
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                  Accepted Cards <span className="text-rose-500">*</span>
                </label>
                <Input
                  type="number"
                  min="0"
                  value={manualAccepted}
                  onChange={(e) => setManualAccepted(e.target.value)}
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                  Rejected Cards <span className="text-rose-500">*</span>
                </label>
                <Input
                  type="number"
                  min="0"
                  value={manualRejected}
                  onChange={(e) => {
                    setManualRejected(e.target.value);
                    const ins = parseInt(manualInspected, 10) || 0;
                    const rej = parseInt(e.target.value, 10) || 0;
                    setManualAccepted(Math.max(0, ins - rej));
                  }}
                  required
                />
              </div>
            </div>
          )}

          {/* Automatic Reprint Alert Trigger */}
          {rejectedQty > 0 && (
            <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl space-y-2">
              <div className="flex items-center gap-2 text-rose-800 text-xs font-bold">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>Automatic Reprint Authorization Trigger</span>
              </div>
              <p className="text-xs text-rose-700">
                Submitting this inspection with <strong>{rejectedQty} rejected card(s)</strong> will automatically generate a formal Reprint Request for Super Admin authorization and liability review.
              </p>
            </div>
          )}

          {/* General Inspection Notes */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
              General Inspection Notes
            </label>
            <Textarea
              rows={2}
              value={generalNotes}
              onChange={(e) => setGeneralNotes(e.target.value)}
              placeholder="e.g. Visual colors match approved client proofs, barcodes scanned and verified successfully."
              className="text-xs"
            />
          </div>

          {/* Actions */}
          <div className="flex items-center justify-between pt-3 border-t border-slate-200">
            <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              disabled={isSubmitting || inspectedQty <= 0}
              className="flex items-center gap-2"
            >
              <ShieldCheck className="w-4 h-4" />
              {isSubmitting ? 'Submitting Inspection...' : `Submit QC Report (${inspectedQty} Cards)`}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Embedded Single Card Preview Modal */}
      {previewingStudent && (
        <SingleCardPreviewModal
          isOpen={!!previewingStudent}
          onClose={() => setPreviewingStudent(null)}
          student={previewingStudent}
          cardSpecs={cardSpecs}
          jobId={activeData?.id}
        />
      )}
    </>
  );
}
