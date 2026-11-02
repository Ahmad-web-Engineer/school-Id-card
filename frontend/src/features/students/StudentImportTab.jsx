import React, { useState, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import {
  FileSpreadsheet,
  FileText,
  UploadCloud,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Download,
  Check,
  AlertCircle,
  ArrowRight,
  RefreshCw,
  FileCheck,
} from 'lucide-react';

export default function StudentImportTab({ onNavigateToRoster }) {
  const queryClient = useQueryClient();
  const fileInputRef = useRef(null);

  // Upload & File State
  const [selectedFile, setSelectedFile] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [isConfirming, setIsConfirming] = useState(false);
  const [downloadingFormat, setDownloadingFormat] = useState(null);
  const [isDownloadingErrors, setIsDownloadingErrors] = useState(false);

  // Batch Result State
  const [batchData, setBatchData] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const [previewFilter, setPreviewFilter] = useState('ALL'); // ALL, VALID, ERROR
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Download template helper
  const handleDownloadTemplate = async (format) => {
    setErrorMessage('');
    setDownloadingFormat(format);
    try {
      const blob = await api.get('/school/students/import-template', {
        params: { format },
        responseType: 'blob',
        headers: { Accept: '*/*' },
      });

      const blobUrl = window.URL.createObjectURL(
        new Blob([blob], {
          type:
            format === 'xlsx'
              ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
              : 'text/csv; charset=UTF-8',
        })
      );
      const link = document.createElement('a');
      link.href = blobUrl;
      link.setAttribute('download', `student_import_template.${format}`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(blobUrl);
    } catch (err) {
      let msg = err.message || 'Failed to download template.';
      if (err instanceof Blob) {
        try {
          const text = await err.text();
          const parsed = JSON.parse(text);
          msg = parsed.message || msg;
        } catch (_) {}
      }
      setErrorMessage(msg);
    } finally {
      setDownloadingFormat(null);
    }
  };

  // Drag and Drop handlers
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
    if (!['xlsx', 'xls', 'csv'].includes(ext)) {
      setErrorMessage('Unsupported file format. Please upload a .xlsx or .csv file.');
      return;
    }

    const MAX_SIZE = 10 * 1024 * 1024; // 10MB
    if (file.size > MAX_SIZE) {
      setErrorMessage('File exceeds the 10 MB maximum upload limit.');
      return;
    }

    setSelectedFile(file);
    setBatchData(null);
    setImportResult(null);
  };

  // Upload & Validate File
  const handleUploadFile = async () => {
    if (!selectedFile) return;
    setErrorMessage('');
    setIsUploading(true);

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);

      const response = await api.post('/school/students/import', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      const data = response?.data || response;
      setBatchData(data);
      setPreviewFilter('ALL');
    } catch (err) {
      setErrorMessage(err.message || 'Failed to upload and validate file.');
    } finally {
      setIsUploading(false);
    }
  };

  // Confirm Import of Valid Rows
  const handleConfirmImport = async () => {
    if (!batchData?.batchId) return;
    setErrorMessage('');
    setIsConfirming(true);

    try {
      const response = await api.post(`/school/students/import/${batchData.batchId}/confirm`);
      const result = response?.data || response;
      setImportResult(result);
      queryClient.invalidateQueries({ queryKey: ['school-students'] });
      setSuccessMessage(
        result.message ||
          `Successfully imported ${result.importedCount || 0} students into your roster.`
      );
    } catch (err) {
      setErrorMessage(err.message || 'Failed to confirm student import.');
    } finally {
      setIsConfirming(false);
    }
  };

  // Download Error Report
  const handleDownloadErrors = async () => {
    const batchId = batchData?.batchId || importResult?.batchId;
    if (!batchId) return;

    setErrorMessage('');
    setIsDownloadingErrors(true);
    try {
      const blob = await api.get(`/school/students/import/${batchId}/errors`, {
        responseType: 'blob',
        headers: { Accept: '*/*' },
      });

      const blobUrl = window.URL.createObjectURL(
        new Blob([blob], { type: 'text/csv; charset=UTF-8' })
      );
      const link = document.createElement('a');
      link.href = blobUrl;
      link.setAttribute('download', `import_errors_${batchId}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(blobUrl);
    } catch (err) {
      let msg = err.message || 'Failed to download error report.';
      if (err instanceof Blob) {
        try {
          const text = await err.text();
          const parsed = JSON.parse(text);
          msg = parsed.message || msg;
        } catch (_) {}
      }
      setErrorMessage(msg);
    } finally {
      setIsDownloadingErrors(false);
    }
  };

  const handleReset = () => {
    setSelectedFile(null);
    setBatchData(null);
    setImportResult(null);
    setErrorMessage('');
    setSuccessMessage('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  // Filter preview rows
  const allRows = batchData?.previewRows || batchData?.rows || [];
  const filteredRows = allRows.filter((r) => {
    if (previewFilter === 'VALID') return r.validationStatus === 'VALID';
    if (previewFilter === 'ERROR') return r.validationStatus === 'ERROR';
    return true;
  });

  const previewColumns = [
    {
      key: 'rowNumber',
      label: 'Row',
      cellClassName: 'font-mono text-xs text-slate-500 font-semibold',
    },
    {
      key: 'admissionNumber',
      label: 'Admission No',
      cellClassName: 'font-mono text-xs font-bold text-indigo-700',
    },
    {
      key: 'studentName',
      label: 'Student Name',
      cellClassName: 'font-semibold text-slate-900',
      render: (val) => val || <span className="text-slate-400 italic">Empty</span>,
    },
    {
      key: 'className',
      label: 'Class & Sec',
      render: (_, r) => (
        <span className="font-medium text-slate-800">
          {r.className || '—'} {r.section ? `(${r.section})` : ''}
        </span>
      ),
    },
    {
      key: 'rollNumber',
      label: 'Roll No',
      render: (val) => val || '—',
    },
    {
      key: 'validationStatus',
      label: 'Status',
      render: (val) => (
        <Badge variant={val === 'VALID' ? 'success' : 'danger'} size="sm">
          {val}
        </Badge>
      ),
    },
    {
      key: 'errorMessage',
      label: 'Validation Remarks',
      render: (val, r) => {
        if (r.validationStatus === 'VALID') {
          return (
            <span className="text-xs text-emerald-700 flex items-center gap-1 font-medium">
              <Check className="w-3.5 h-3.5 text-emerald-600" />
              Valid row
            </span>
          );
        }
        return (
          <div className="flex items-start gap-1.5 text-xs text-rose-700 font-medium max-w-sm">
            <AlertCircle className="w-3.5 h-3.5 shrink-0 text-rose-500 mt-0.5" />
            <span>{val || 'Validation failed'}</span>
          </div>
        );
      },
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header and Templates Card */}
      <Card bodyClassName="p-5">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div className="max-w-xl">
            <div className="flex items-center gap-2 mb-1">
              <FileSpreadsheet className="h-5 w-5 text-indigo-600" />
              <h2 className="text-base font-bold text-slate-900">Bulk Import Students</h2>
            </div>
            <p className="text-xs text-slate-500 leading-relaxed">
              Upload existing student data in bulk using our standard spreadsheet template.
              The validator checks admission numbers, class rosters, and required fields before importing.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2 sm:gap-3">
            <Button
              variant="outline"
              size="sm"
              loading={downloadingFormat === 'xlsx'}
              onClick={() => handleDownloadTemplate('xlsx')}
              className="text-emerald-700 border-emerald-200 hover:bg-emerald-50 hover:border-emerald-300"
            >
              <FileSpreadsheet className="h-4 w-4 mr-1.5 text-emerald-600" />
              Download Excel Template
            </Button>
            <Button
              variant="outline"
              size="sm"
              loading={downloadingFormat === 'csv'}
              onClick={() => handleDownloadTemplate('csv')}
              className="text-slate-700 hover:bg-slate-50"
            >
              <FileText className="h-4 w-4 mr-1.5 text-slate-600" />
              Download CSV Template
            </Button>
          </div>
        </div>
      </Card>

      {/* Global Alerts */}
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

      {successMessage && (
        <div className="p-3.5 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2">
          <Check className="h-4 w-4 text-emerald-600 shrink-0" />
          <span>{successMessage}</span>
        </div>
      )}

      {/* STEP 1: Upload Zone (Visible if no preview and not finished) */}
      {!batchData && !importResult && (
        <Card bodyClassName="p-8">
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
              accept=".xlsx,.xls,.csv"
              onChange={handleFileInputChange}
              className="hidden"
              id="student-spreadsheet-file-input"
              aria-label="Upload student spreadsheet"
            />

            <div className="flex flex-col items-center justify-center space-y-3">
              <div
                className={`p-3.5 rounded-full ${
                  selectedFile
                    ? 'bg-emerald-100 text-emerald-700'
                    : 'bg-indigo-100 text-indigo-700'
                }`}
              >
                {selectedFile ? (
                  <FileCheck className="h-7 w-7" />
                ) : (
                  <UploadCloud className="h-7 w-7" />
                )}
              </div>

              {selectedFile ? (
                <div>
                  <div className="font-semibold text-sm text-slate-900">{selectedFile.name}</div>
                  <div className="text-xs text-slate-500 mt-0.5">
                    {(selectedFile.size / 1024).toFixed(1)} KB • Click or drag to replace
                  </div>
                </div>
              ) : (
                <div>
                  <p className="text-sm font-semibold text-slate-800">
                    Click to browse or drag & drop student spreadsheet
                  </p>
                  <p className="text-xs text-slate-500 mt-1">
                    Accepts standard Excel (.xlsx) or comma-separated (.csv) files up to 10 MB.
                  </p>
                </div>
              )}
            </div>
          </div>

          {selectedFile && (
            <div className="mt-5 flex items-center justify-end gap-2.5">
              <Button
                variant="outline"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  setSelectedFile(null);
                }}
                disabled={isUploading}
              >
                Remove
              </Button>
              <Button
                size="sm"
                loading={isUploading}
                onClick={handleUploadFile}
              >
                <UploadCloud className="h-4 w-4 mr-1.5" />
                Validate & Preview File
              </Button>
            </div>
          )}
        </Card>
      )}

      {/* STEP 2: Validation Preview Screen */}
      {batchData && !importResult && (
        <div className="space-y-6">
          {/* Summary Metric Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <Card bodyClassName="p-4 flex items-center justify-between border-slate-200">
              <div>
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Total Rows
                </span>
                <div className="text-2xl font-bold text-slate-900 mt-0.5">
                  {batchData.totalRows || 0}
                </div>
                <div className="text-[11px] text-slate-500 mt-1 truncate">
                  File: {batchData.fileName}
                </div>
              </div>
              <div className="p-3 bg-slate-100 rounded-lg text-slate-700">
                <FileSpreadsheet className="h-5 w-5" />
              </div>
            </Card>

            <Card bodyClassName="p-4 flex items-center justify-between border-emerald-200 bg-emerald-50/20">
              <div>
                <span className="text-xs font-semibold uppercase tracking-wider text-emerald-700">
                  Valid Rows
                </span>
                <div className="text-2xl font-bold text-emerald-800 mt-0.5">
                  {batchData.validRows || 0}
                </div>
                <div className="text-[11px] text-emerald-600 mt-1 font-medium">
                  Ready to be imported
                </div>
              </div>
              <div className="p-3 bg-emerald-100 rounded-lg text-emerald-700">
                <CheckCircle2 className="h-5 w-5" />
              </div>
            </Card>

            <Card bodyClassName="p-4 flex items-center justify-between border-rose-200 bg-rose-50/20">
              <div>
                <span className="text-xs font-semibold uppercase tracking-wider text-rose-700">
                  Error Rows
                </span>
                <div className="text-2xl font-bold text-rose-800 mt-0.5">
                  {batchData.errorRows || 0}
                </div>
                <div className="text-[11px] text-rose-600 mt-1 font-medium">
                  {batchData.errorRows > 0 ? 'Requires attention / skipped' : 'No errors found'}
                </div>
              </div>
              <div className="p-3 bg-rose-100 rounded-lg text-rose-700">
                <AlertTriangle className="h-5 w-5" />
              </div>
            </Card>
          </div>

          {/* Action and Filter Bar */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 bg-white border border-slate-200 rounded-lg">
            {/* Filter Pills */}
            <div className="flex items-center gap-1.5" role="group" aria-label="Row filter tabs">
              <button
                type="button"
                onClick={() => setPreviewFilter('ALL')}
                className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                  previewFilter === 'ALL'
                    ? 'bg-indigo-600 text-white'
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                All Rows ({batchData.totalRows || 0})
              </button>
              <button
                type="button"
                onClick={() => setPreviewFilter('VALID')}
                className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                  previewFilter === 'VALID'
                    ? 'bg-emerald-600 text-white'
                    : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100'
                }`}
              >
                Valid Only ({batchData.validRows || 0})
              </button>
              <button
                type="button"
                onClick={() => setPreviewFilter('ERROR')}
                className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-colors ${
                  previewFilter === 'ERROR'
                    ? 'bg-rose-600 text-white'
                    : 'bg-rose-50 text-rose-800 hover:bg-rose-100'
                }`}
              >
                Errors Only ({batchData.errorRows || 0})
              </button>
            </div>

            {/* Actions */}
            <div className="flex flex-wrap items-center gap-2">
              {batchData.errorRows > 0 && (
                <Button
                  variant="outline"
                  size="sm"
                  loading={isDownloadingErrors}
                  onClick={handleDownloadErrors}
                  className="text-rose-700 border-rose-200 hover:bg-rose-50"
                  title="Download CSV containing row-by-row error reasons"
                >
                  <Download className="h-3.5 w-3.5 mr-1.5" />
                  Download Error Report
                </Button>
              )}

              <Button
                variant="outline"
                size="sm"
                onClick={handleReset}
                disabled={isConfirming}
              >
                Upload Different File
              </Button>

              <Button
                size="sm"
                loading={isConfirming}
                disabled={!batchData.validRows || batchData.validRows === 0}
                onClick={handleConfirmImport}
                className="bg-emerald-600 hover:bg-emerald-700 text-white focus-visible:ring-emerald-500"
              >
                <Check className="h-4 w-4 mr-1.5" />
                Confirm & Import Valid Students ({batchData.validRows || 0})
              </Button>
            </div>
          </div>

          {/* Validation Preview Table */}
          <ResponsiveTable
            columns={previewColumns}
            data={filteredRows}
            keyField="rowNumber"
            emptyTitle="No rows to display"
            emptyMessage={
              previewFilter === 'ERROR'
                ? 'No error rows found in this batch! All records passed validation.'
                : 'No student records found in file.'
            }
          />
        </div>
      )}

      {/* STEP 3: Post-Confirmation Success View */}
      {importResult && (
        <Card bodyClassName="p-8 sm:p-12 text-center max-w-2xl mx-auto">
          <div className="inline-flex p-4 rounded-full bg-emerald-100 text-emerald-600 mb-4 shadow-xs">
            <CheckCircle2 className="h-10 w-10" />
          </div>

          <h3 className="text-xl font-bold text-slate-900">
            Import Completed Successfully!
          </h3>

          <p className="text-xs sm:text-sm text-slate-600 mt-2 max-w-lg mx-auto">
            {importResult.message ||
              `Enrolled ${importResult.importedCount || 0} valid students into the school directory.`}
          </p>

          <div className="my-6 grid grid-cols-2 gap-3 max-w-xs mx-auto">
            <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg">
              <span className="text-[11px] font-semibold text-emerald-700 uppercase tracking-wide">
                Imported
              </span>
              <div className="text-xl font-bold text-emerald-800 mt-0.5">
                {importResult.importedCount ?? 0}
              </div>
            </div>
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg">
              <span className="text-[11px] font-semibold text-slate-600 uppercase tracking-wide">
                Skipped
              </span>
              <div className="text-xl font-bold text-slate-800 mt-0.5">
                {importResult.errorCount ?? 0}
              </div>
            </div>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-3">
            <Button onClick={onNavigateToRoster}>
              View in Student Roster
              <ArrowRight className="h-4 w-4 ml-1.5" />
            </Button>

            {importResult.errorCount > 0 && (
              <Button
                variant="outline"
                loading={isDownloadingErrors}
                onClick={handleDownloadErrors}
                className="text-rose-700 border-rose-200 hover:bg-rose-50"
              >
                <Download className="h-4 w-4 mr-1.5" />
                Download Error Report
              </Button>
            )}

            <Button variant="outline" onClick={handleReset}>
              Import Another File
            </Button>
          </div>
        </Card>
      )}
    </div>
  );
}
