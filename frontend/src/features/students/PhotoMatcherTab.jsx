import React, { useState, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Select from '../../components/common/Select';
import Badge from '../../components/common/Badge';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import {
  Camera,
  UploadCloud,
  CheckCircle2,
  AlertTriangle,
  FolderArchive,
  ArrowRight,
  FileArchive,
  Check,
  AlertCircle,
  HelpCircle,
  RefreshCw,
  Users,
} from 'lucide-react';

const CLASS_OPTIONS = [
  ...['Nursery', 'LKG', 'UKG', '1st', '2nd', '3rd', '4th', '5th', '6th', '7th', '8th', '9th', '10th', '11th', '12th'].map(
    (c) => ({ value: c, label: `Class ${c}` })
  ),
];

const SECTION_OPTIONS = [
  ...['A', 'B', 'C', 'D', 'E'].map((s) => ({ value: s, label: `Section ${s}` })),
];

export default function PhotoMatcherTab({ onNavigateToRoster }) {
  const queryClient = useQueryClient();
  const fileInputRef = useRef(null);

  // Mode and Option Selection
  // 'auto' = auto detect from filename; 'target' = specific class and section
  const [matchMode, setMatchMode] = useState('auto');
  const [targetClass, setTargetClass] = useState('10th');
  const [targetSection, setTargetSection] = useState('A');

  // File & Upload State
  const [selectedFile, setSelectedFile] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Results State
  const [batchData, setBatchData] = useState(null);
  const [statusFilter, setStatusFilter] = useState('ALL'); // ALL, MATCHED, UNMATCHED, DUPLICATE

  // Drag and drop handlers
  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    const files = e.dataTransfer?.files;
    if (files && files.length > 0) {
      validateAndSetFile(files[0]);
    }
  };

  const handleFileInputChange = (e) => {
    const files = e.target?.files;
    if (files && files.length > 0) {
      validateAndSetFile(files[0]);
    }
  };

  const validateAndSetFile = (file) => {
    setErrorMessage('');
    const ext = file.name.split('.').pop()?.toLowerCase();
    if (ext !== 'zip') {
      setErrorMessage('Only ZIP archives (.zip) containing student portrait photos are supported.');
      return;
    }

    const MAX_SIZE = 200 * 1024 * 1024; // 200MB
    if (file.size > MAX_SIZE) {
      setErrorMessage('ZIP archive exceeds the 200 MB maximum size limit.');
      return;
    }

    setSelectedFile(file);
    setBatchData(null);
  };

  const handleUploadAndMatch = async () => {
    if (!selectedFile) return;
    setErrorMessage('');
    setIsUploading(true);

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);

      if (matchMode === 'target') {
        if (targetClass) formData.append('className', targetClass);
        if (targetSection) formData.append('section', targetSection);
      }

      const response = await api.post('/school/photos/bulk-upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      const data = response?.data || response;
      setBatchData(data);
      setStatusFilter('ALL');
      queryClient.invalidateQueries({ queryKey: ['school-students'] });
    } catch (err) {
      setErrorMessage(err.message || 'Failed to process photo ZIP archive.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleReset = () => {
    setSelectedFile(null);
    setBatchData(null);
    setErrorMessage('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  // Filter match results
  const allResults = batchData?.results || [];
  const filteredResults = allResults.filter((item) => {
    if (statusFilter === 'MATCHED') return item.matchStatus === 'MATCHED';
    if (statusFilter === 'UNMATCHED') return item.matchStatus === 'NO_STUDENT_FOUND';
    if (statusFilter === 'DUPLICATE') return item.matchStatus === 'DUPLICATE_MATCH';
    return true;
  });

  const matchColumns = [
    {
      key: 'fileName',
      label: 'Photo Filename',
      cellClassName: 'font-mono text-xs font-semibold text-slate-900',
    },
    {
      key: 'parsedIdentifiers',
      label: 'Parsed Info',
      render: (_, r) => {
        const parts = [];
        if (r.parsedClass) parts.push(`Class ${r.parsedClass}`);
        if (r.parsedSection) parts.push(`Sec ${r.parsedSection}`);
        if (r.parsedRollNumber) parts.push(`Roll #${r.parsedRollNumber}`);
        return (
          <span className="text-xs text-slate-700 font-medium">
            {parts.length > 0 ? parts.join(' • ') : '—'}
          </span>
        );
      },
    },
    {
      key: 'matchedStudent',
      label: 'Matched Student',
      render: (_, r) => {
        if (r.studentName) {
          return (
            <div>
              <div className="font-semibold text-slate-900 text-xs sm:text-sm">
                {r.studentName}
              </div>
              <div className="text-[11px] font-mono text-indigo-700">
                {r.admissionNumber || 'No Adm No'}
              </div>
            </div>
          );
        }
        return <span className="text-xs text-slate-400 italic">No matching student</span>;
      },
    },
    {
      key: 'matchStatus',
      label: 'Status',
      render: (val) => {
        let variant = 'neutral';
        let label = val;
        if (val === 'MATCHED') {
          variant = 'success';
          label = 'Matched';
        } else if (val === 'NO_STUDENT_FOUND') {
          variant = 'warning';
          label = 'No Student Found';
        } else if (val === 'DUPLICATE_MATCH') {
          variant = 'neutral';
          label = 'Duplicate Match';
        } else {
          variant = 'danger';
        }
        return (
          <Badge variant={variant} size="sm">
            {label}
          </Badge>
        );
      },
    },
    {
      key: 'message',
      label: 'Details & Note',
      render: (val, r) => (
        <span
          className={`text-xs ${
            r.matchStatus === 'MATCHED'
              ? 'text-emerald-700'
              : r.matchStatus === 'NO_STUDENT_FOUND'
              ? 'text-amber-700'
              : 'text-slate-600'
          }`}
        >
          {val || '—'}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Intro Guidance Card */}
      <Card bodyClassName="p-5">
        <div className="flex items-start gap-3">
          <div className="p-2.5 bg-indigo-50 text-indigo-600 rounded-lg shrink-0 mt-0.5">
            <Camera className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900">Automatic Photo Matcher</h2>
            <p className="text-xs text-slate-500 mt-1 leading-relaxed">
              Upload a ZIP archive containing student portrait images (.jpg, .jpeg, or .png).
              The system automatically parses roll numbers or admission numbers from the file names
              and assigns each photo directly to the matching student profile.
            </p>
          </div>
        </div>
      </Card>

      {/* Global Error Banner */}
      {errorMessage && (
        <div className="p-3.5 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-4 w-4 text-rose-600 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button
            type="button"
            onClick={() => setErrorMessage('')}
            className="text-rose-500 hover:text-rose-800 font-bold"
          >
            ✕
          </button>
        </div>
      )}

      {/* STEP 1: Upload and Configuration Wizard (visible when no batch loaded) */}
      {!batchData && (
        <div className="space-y-5">
          {/* Class & Section Option Selection */}
          <Card
            title="1. Configure Photo Matching Strategy"
            subtitle="Tell the matcher how photo filenames are structured inside your ZIP archive"
            bodyClassName="p-5 space-y-4"
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Option A: Auto-detect */}
              <label
                className={`flex items-start gap-3 p-4 rounded-lg border cursor-pointer transition-colors ${
                  matchMode === 'auto'
                    ? 'border-indigo-600 bg-indigo-50/30 ring-1 ring-indigo-600'
                    : 'border-slate-200 hover:border-slate-300 bg-white'
                }`}
              >
                <input
                  type="radio"
                  name="matchMode"
                  value="auto"
                  checked={matchMode === 'auto'}
                  onChange={() => setMatchMode('auto')}
                  className="mt-0.5 text-indigo-600 focus:ring-indigo-500"
                />
                <div className="space-y-1">
                  <div className="text-xs font-bold text-slate-900">
                    Auto-detect from file names (Recommended)
                  </div>
                  <p className="text-[11px] text-slate-500 leading-relaxed">
                    Photos include class and section or admission numbers in their name, e.g.:
                    <code className="text-indigo-700 bg-indigo-50 px-1 py-0.5 rounded mx-1">
                      10_A_01.jpg
                    </code>
                    or
                    <code className="text-indigo-700 bg-indigo-50 px-1 py-0.5 rounded mx-1">
                      ADM-2026-001.jpg
                    </code>
                  </p>
                </div>
              </label>

              {/* Option B: Specific Class & Section */}
              <label
                className={`flex items-start gap-3 p-4 rounded-lg border cursor-pointer transition-colors ${
                  matchMode === 'target'
                    ? 'border-indigo-600 bg-indigo-50/30 ring-1 ring-indigo-600'
                    : 'border-slate-200 hover:border-slate-300 bg-white'
                }`}
              >
                <input
                  type="radio"
                  name="matchMode"
                  value="target"
                  checked={matchMode === 'target'}
                  onChange={() => setMatchMode('target')}
                  className="mt-0.5 text-indigo-600 focus:ring-indigo-500"
                />
                <div className="space-y-1">
                  <div className="text-xs font-bold text-slate-900">
                    Target specific Class and Section (Option A)
                  </div>
                  <p className="text-[11px] text-slate-500 leading-relaxed">
                    Photos in this ZIP belong to one specific cohort and are named by roll number only, e.g.:
                    <code className="text-indigo-700 bg-indigo-50 px-1 py-0.5 rounded mx-1">
                      01.jpg
                    </code>
                    or
                    <code className="text-indigo-700 bg-indigo-50 px-1 py-0.5 rounded mx-1">
                      12.png
                    </code>
                  </p>
                </div>
              </label>
            </div>

            {/* Target Selectors dropdowns */}
            {matchMode === 'target' && (
              <div className="p-4 bg-slate-50 rounded-lg border border-slate-200 grid grid-cols-1 sm:grid-cols-2 gap-3 pt-3">
                <Select
                  label="Target Class *"
                  options={CLASS_OPTIONS}
                  value={targetClass}
                  onChange={(e) => setTargetClass(e.target.value)}
                />
                <Select
                  label="Target Section *"
                  options={SECTION_OPTIONS}
                  value={targetSection}
                  onChange={(e) => setTargetSection(e.target.value)}
                />
              </div>
            )}
          </Card>

          {/* ZIP Dropzone */}
          <Card
            title="2. Upload ZIP Archive"
            subtitle="Archive must contain portrait images (JPG or PNG, max 200 MB total)"
            bodyClassName="p-5"
          >
            <div
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`cursor-pointer rounded-xl border-2 border-dashed transition-all p-8 sm:p-12 text-center ${
                isDragging
                  ? 'border-indigo-600 bg-indigo-50/50 scale-[0.99]'
                  : selectedFile
                  ? 'border-emerald-400 bg-emerald-50/20'
                  : 'border-slate-300 hover:border-indigo-400 bg-slate-50/50'
              }`}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".zip,application/zip"
                onChange={handleFileInputChange}
                className="hidden"
                id="photo-zip-file-input"
                aria-label="Upload photo ZIP archive"
              />

              <div className="flex flex-col items-center justify-center space-y-3">
                <div
                  className={`p-3.5 rounded-full ${
                    selectedFile
                      ? 'bg-emerald-100 text-emerald-700'
                      : 'bg-indigo-100 text-indigo-700'
                  }`}
                >
                  <FolderArchive className="h-7 w-7" />
                </div>

                {selectedFile ? (
                  <div>
                    <div className="font-semibold text-sm text-slate-900">{selectedFile.name}</div>
                    <div className="text-xs text-slate-500 mt-0.5">
                      {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB • Ready for processing
                    </div>
                  </div>
                ) : (
                  <div>
                    <p className="text-sm font-semibold text-slate-800">
                      Click to choose or drag & drop ZIP file
                    </p>
                    <p className="text-xs text-slate-500 mt-1">
                      Archives up to 200 MB containing student photos (.jpg, .png).
                    </p>
                  </div>
                )}
              </div>
            </div>

            {selectedFile && (
              <div className="mt-4 flex items-center justify-end gap-2.5">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    setSelectedFile(null);
                  }}
                  disabled={isUploading}
                >
                  Cancel
                </Button>
                <Button
                  size="sm"
                  loading={isUploading}
                  onClick={handleUploadAndMatch}
                >
                  <Camera className="h-4 w-4 mr-1.5" />
                  Upload & Match Photos
                </Button>
              </div>
            )}
          </Card>
        </div>
      )}

      {/* STEP 2: Match Results Dashboard */}
      {batchData && (
        <div className="space-y-6">
          {/* Summary Metric Cards */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <Card bodyClassName="p-4 border-slate-200">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                Total Photos
              </span>
              <div className="text-2xl font-bold text-slate-900 mt-0.5">
                {batchData.totalFiles || 0}
              </div>
              <div className="text-[11px] text-slate-400 mt-1 truncate">
                Processed in ZIP
              </div>
            </Card>

            <Card bodyClassName="p-4 border-emerald-200 bg-emerald-50/20">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-emerald-700">
                Matched
              </span>
              <div className="text-2xl font-bold text-emerald-800 mt-0.5">
                {batchData.matchedCount || 0}
              </div>
              <div className="text-[11px] text-emerald-600 mt-1 font-medium">
                Assigned to students
              </div>
            </Card>

            <Card bodyClassName="p-4 border-amber-200 bg-amber-50/20">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-amber-700">
                Unmatched
              </span>
              <div className="text-2xl font-bold text-amber-800 mt-0.5">
                {batchData.unmatchedCount || 0}
              </div>
              <div className="text-[11px] text-amber-600 mt-1 font-medium">
                No student record found
              </div>
            </Card>

            <Card bodyClassName="p-4 border-slate-200">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                Duplicates / Others
              </span>
              <div className="text-2xl font-bold text-slate-800 mt-0.5">
                {batchData.duplicateCount || 0}
              </div>
              <div className="text-[11px] text-slate-500 mt-1">
                Multiple files per student
              </div>
            </Card>
          </div>

          {/* Action and Filter Bar */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 bg-white border border-slate-200 rounded-lg">
            {/* Filter Pills */}
            <div className="flex flex-wrap items-center gap-1.5" role="group" aria-label="Photo match filter">
              <button
                type="button"
                onClick={() => setStatusFilter('ALL')}
                className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                  statusFilter === 'ALL'
                    ? 'bg-indigo-600 text-white'
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                All Photos ({batchData.totalFiles || 0})
              </button>
              <button
                type="button"
                onClick={() => setStatusFilter('MATCHED')}
                className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                  statusFilter === 'MATCHED'
                    ? 'bg-emerald-600 text-white'
                    : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100'
                }`}
              >
                Matched ({batchData.matchedCount || 0})
              </button>
              <button
                type="button"
                onClick={() => setStatusFilter('UNMATCHED')}
                className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                  statusFilter === 'UNMATCHED'
                    ? 'bg-amber-600 text-white'
                    : 'bg-amber-50 text-amber-800 hover:bg-amber-100'
                }`}
              >
                Unmatched ({batchData.unmatchedCount || 0})
              </button>
              {batchData.duplicateCount > 0 && (
                <button
                  type="button"
                  onClick={() => setStatusFilter('DUPLICATE')}
                  className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                    statusFilter === 'DUPLICATE'
                      ? 'bg-slate-700 text-white'
                      : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                  }`}
                >
                  Duplicates ({batchData.duplicateCount || 0})
                </button>
              )}
            </div>

            {/* Actions */}
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" onClick={handleReset}>
                Upload Another ZIP
              </Button>
              <Button size="sm" onClick={onNavigateToRoster}>
                <Users className="h-4 w-4 mr-1.5" />
                View in Student Roster
              </Button>
            </div>
          </div>

          {/* Results Table */}
          <ResponsiveTable
            columns={matchColumns}
            data={filteredResults}
            keyField="fileName"
            emptyTitle="No photos to display"
            emptyMessage={
              statusFilter === 'UNMATCHED'
                ? 'All photos in this batch successfully matched with enrolled students!'
                : 'No photo records match the current filter.'
            }
          />
        </div>
      )}
    </div>
  );
}
