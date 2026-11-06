import React, { useState, useEffect, useMemo } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { directPrintUrl } from '../../utils/printHelper';
import api from '../../services/api';
import {
  Printer,
  Download,
  FileText,
  Sliders,
  Layers,
  Grid,
  CheckCircle2,
  Lock,
  Unlock,
  Eye,
  RefreshCw,
  AlertTriangle,
  CreditCard,
  FileDown
} from 'lucide-react';

export default function PrintImpositionModal({
  isOpen,
  onClose,
  role = 'SUPER_ADMIN', // 'SUPER_ADMIN' or 'PRINTING_AGENCY'
  orderId,
  jobId,
  identifierName = '',
  totalStudents = 1,
  sampleStudent = null,
  initialSettings = null,
  allowAgencyOverride = false,
  onSettingsUpdated
}) {
  // Mode: SHEET or PLASTIC_CARD
  const [printMode, setPrintMode] = useState('SHEET');
  const [paperSize, setPaperSize] = useState('A4');
  const [sides, setSides] = useState('DUPLEX');
  const [columns, setColumns] = useState(3);
  const [rows, setRows] = useState(3);
  const [hGap, setHGap] = useState(2.0);
  const [vGap, setVGap] = useState(2.0);
  const [topOffset, setTopOffset] = useState(10.0);
  const [leftOffset, setLeftOffset] = useState(10.0);
  const [autoCenter, setAutoCenter] = useState(true);
  const [showCropMarks, setShowCropMarks] = useState(true);
  const [showCutGuides, setShowCutGuides] = useState(true);
  const [agencyOverridePermitted, setAgencyOverridePermitted] = useState(allowAgencyOverride);

  // Preview state
  const [previewSide, setPreviewSide] = useState('front'); // 'front' or 'back'

  // Loading & error states
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [downloadingType, setDownloadingType] = useState(null); // 'pdf', 'image', 'plastic', 'direct'

  const isAgency = role === 'PRINTING_AGENCY';
  const isLockedForAgency = isAgency && !allowAgencyOverride;

  // Initialize from props or server settings
  useEffect(() => {
    if (isOpen) {
      setActionError(null);
      setSaveSuccess(false);

      if (initialSettings) {
        applySettings(initialSettings);
      } else {
        // Fetch saved settings from backend
        fetchSavedSettings();
      }

      setAgencyOverridePermitted(allowAgencyOverride);
    }
  }, [isOpen, initialSettings, allowAgencyOverride]);

  const fetchSavedSettings = async () => {
    try {
      const endpoint = isAgency
        ? `/agency/jobs/${jobId}/print-settings`
        : `/admin/orders/${orderId}/print-settings`;
      const res = await api.get(endpoint);
      const data = res?.data || res;
      if (data) {
        applySettings(data);
      }
    } catch (err) {
      console.warn('Could not load saved print settings, using defaults', err);
    }
  };

  const applySettings = (cfg) => {
    if (!cfg) return;
    if (cfg.printMode) setPrintMode(cfg.printMode);
    if (cfg.paperSize) setPaperSize(cfg.paperSize);
    if (cfg.sides) setSides(cfg.sides);
    if (cfg.columns) setColumns(cfg.columns);
    if (cfg.rows) setRows(cfg.rows);
    if (cfg.horizontalSpacingMm !== undefined) setHGap(cfg.horizontalSpacingMm);
    if (cfg.verticalSpacingMm !== undefined) setVGap(cfg.verticalSpacingMm);
    if (cfg.topOffsetMm !== undefined || cfg.marginTopMm !== undefined) {
      setTopOffset(cfg.topOffsetMm !== undefined ? cfg.topOffsetMm : cfg.marginTopMm);
    }
    if (cfg.leftOffsetMm !== undefined || cfg.marginLeftMm !== undefined) {
      setLeftOffset(cfg.leftOffsetMm !== undefined ? cfg.leftOffsetMm : cfg.marginLeftMm);
    }
    if (cfg.autoCenter !== undefined) setAutoCenter(cfg.autoCenter);
    if (cfg.showCropMarks !== undefined) setShowCropMarks(cfg.showCropMarks);
    if (cfg.showCutGuides !== undefined) setShowCutGuides(cfg.showCutGuides);
    if (cfg.allowAgencyOverride !== undefined) setAgencyOverridePermitted(cfg.allowAgencyOverride);
  };

  // Quick preset selections
  const applyPreset = (cols, r, hG = 2.0, vG = 2.0) => {
    if (isLockedForAgency) return;
    setColumns(cols);
    setRows(r);
    setHGap(hG);
    setVGap(vG);
  };

  // Calculations for preview & sheet stats
  const cardsPerPage = printMode === 'PLASTIC_CARD' ? 1 : columns * rows;
  const totalSheetsNeeded = Math.ceil(Math.max(1, totalStudents) / cardsPerPage);

  // Construct current query parameter string
  const queryParams = useMemo(() => {
    const params = new URLSearchParams();
    params.set('printMode', printMode);
    params.set('paperSize', paperSize);
    params.set('sides', sides);
    params.set('columns', String(columns));
    params.set('rows', String(rows));
    params.set('horizontalSpacingMm', String(hGap));
    params.set('verticalSpacingMm', String(vGap));
    params.set('autoCenter', String(autoCenter));
    params.set('showCropMarks', String(showCropMarks));
    params.set('showCutGuides', String(showCutGuides));
    return params.toString();
  }, [printMode, paperSize, sides, columns, rows, hGap, vGap, autoCenter, showCropMarks, showCutGuides]);

  // Save current settings to server
  const handleSaveSettings = async () => {
    if (isLockedForAgency) return;
    try {
      setIsSaving(true);
      setActionError(null);
      setSaveSuccess(false);

      const payload = {
        printMode,
        paperSize,
        sides,
        columns,
        rows,
        horizontalSpacingMm: parseFloat(hGap) || 0,
        verticalSpacingMm: parseFloat(vGap) || 0,
        topOffsetMm: parseFloat(topOffset) || 10,
        bottomOffsetMm: parseFloat(topOffset) || 10,
        leftOffsetMm: parseFloat(leftOffset) || 10,
        rightOffsetMm: parseFloat(leftOffset) || 10,
        autoCenter,
        showCropMarks,
        showCutGuides,
        allowAgencyOverride: agencyOverridePermitted
      };

      const endpoint = isAgency
        ? `/agency/jobs/${jobId}/print-settings`
        : `/admin/orders/${orderId}/print-settings`;

      await api.put(endpoint, payload);
      setSaveSuccess(true);
      if (onSettingsUpdated) onSettingsUpdated(payload);
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch (err) {
      setActionError(err.message || 'Failed to save print settings.');
    } finally {
      setIsSaving(false);
    }
  };

  // Direct Browser Print
  const handleDirectPrint = async () => {
    try {
      setDownloadingType('direct');
      setActionError(null);
      const baseUrl = isAgency
        ? `/agency/jobs/${jobId}/print-sheet-pdf?${queryParams}`
        : `/api/v1/admin/orders/${orderId}/print-sheet-pdf?${queryParams}`;
      await directPrintUrl(baseUrl, `Prepress ${identifierName || 'Sheet'}`);
    } catch (err) {
      setActionError('Direct print error: ' + (err.message || 'Printer failed'));
    } finally {
      setDownloadingType(null);
    }
  };

  // Download Imposition PDF
  const handleDownloadSheetPdf = async () => {
    try {
      setDownloadingType('pdf');
      setActionError(null);
      const endpoint = isAgency
        ? `/agency/jobs/${jobId}/print-sheet-pdf?${queryParams}`
        : `/admin/orders/${orderId}/print-sheet-pdf?${queryParams}`;

      const res = await api.get(endpoint, { responseType: 'blob' });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${identifierName || 'print'}_${paperSize}_${columns}x${rows}_sheet.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError('Failed to generate imposition sheet PDF.');
    } finally {
      setDownloadingType(null);
    }
  };

  // Download 300 DPI Imposition JPEG
  const handleDownloadSheetImage = async () => {
    try {
      setDownloadingType('image');
      setActionError(null);
      const endpoint = isAgency
        ? `/agency/jobs/${jobId}/print-sheet-image?side=${previewSide}&${queryParams}`
        : `/admin/orders/${orderId}/print-sheet-image?side=${previewSide}&${queryParams}`;

      const res = await api.get(endpoint, { responseType: 'blob' });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'image/jpeg' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${identifierName || 'print'}_${paperSize}_sheet_${previewSide}.jpg`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError('Failed to download imposition sheet JPEG.');
    } finally {
      setDownloadingType(null);
    }
  };

  // Download Direct Plastic Card PDF (1 card per page)
  const handleDownloadPlasticPdf = async () => {
    try {
      setDownloadingType('plastic');
      setActionError(null);
      const endpoint = isAgency
        ? `/agency/jobs/${jobId}/print-plastic-pdf?sides=${sides}`
        : `/admin/orders/${orderId}/print-plastic-pdf?sides=${sides}`;

      const res = await api.get(endpoint, { responseType: 'blob' });
      const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${identifierName || 'print'}_CR80_plastic_cards.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setActionError('Failed to generate plastic cards PDF.');
    } finally {
      setDownloadingType(null);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Prepress Imposition & Bulk Printing Studio"
      size="2xl"
    >
      <div className="flex flex-col gap-5">
        {/* Header Badges & Role Status */}
        <div className="flex flex-wrap items-center justify-between gap-3 p-3.5 bg-slate-50 border border-slate-200 rounded-lg">
          <div>
            <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              {isAgency ? 'Agency Prepress Console' : 'Administrator Prepress Console'}
            </div>
            <div className="text-base font-bold text-slate-900 flex items-center gap-2">
              <span>{identifierName || 'Production Job'}</span>
              <span className="text-xs px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-700 font-semibold">
                {totalStudents} {totalStudents === 1 ? 'Student' : 'Students'}
              </span>
            </div>
          </div>

          {/* Locked vs Override Status Badge */}
          {isAgency ? (
            isLockedForAgency ? (
              <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-amber-50 border border-amber-200 text-amber-800 text-xs font-medium">
                <Lock className="w-4 h-4 text-amber-600 flex-shrink-0" />
                <span>Layout Locked by Super Admin</span>
              </div>
            ) : (
              <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-medium">
                <Unlock className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                <span>Customization Permitted by Super Admin</span>
              </div>
            )
          ) : (
            <label className="flex items-center gap-2 cursor-pointer bg-white px-3 py-1.5 rounded-md border border-slate-300 hover:border-indigo-400 transition-colors shadow-sm text-xs font-medium text-slate-700">
              <input
                type="checkbox"
                checked={agencyOverridePermitted}
                onChange={(e) => setAgencyOverridePermitted(e.target.checked)}
                className="w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500 border-slate-300"
              />
              <span>Allow Printing Agency to Customize Layout</span>
            </label>
          )}
        </div>

        {/* Error / Success Notifications */}
        {actionError && (
          <div className="p-3 bg-rose-50 border border-rose-200 rounded-md text-sm text-rose-700 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-rose-500 flex-shrink-0" />
            <span>{actionError}</span>
          </div>
        )}
        {saveSuccess && (
          <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-md text-sm text-emerald-700 flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
            <span>Print settings saved successfully as defaults.</span>
          </div>
        )}

        {/* Printing Mode Toggle */}
        <div className="grid grid-cols-2 gap-3 p-1 bg-slate-100 rounded-lg">
          <button
            type="button"
            onClick={() => !isLockedForAgency && setPrintMode('SHEET')}
            disabled={isLockedForAgency}
            className={`flex items-center justify-center gap-2 py-2 px-3 text-sm font-semibold rounded-md transition-all ${
              printMode === 'SHEET'
                ? 'bg-white text-indigo-700 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Grid className="w-4 h-4" />
            Multi-Card Sheet Imposition (A4 / A3 / Letter)
          </button>
          <button
            type="button"
            onClick={() => !isLockedForAgency && setPrintMode('PLASTIC_CARD')}
            disabled={isLockedForAgency}
            className={`flex items-center justify-center gap-2 py-2 px-3 text-sm font-semibold rounded-md transition-all ${
              printMode === 'PLASTIC_CARD'
                ? 'bg-white text-indigo-700 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <CreditCard className="w-4 h-4" />
            Direct Plastic Card (CR80 PVC 1-by-1)
          </button>
        </div>

        {/* Main Configuration Grid & Live Preview */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
          {/* Controls Column */}
          <div className="lg:col-span-7 flex flex-col gap-4">
            {printMode === 'SHEET' ? (
              <>
                {/* Paper Standard & Print Sides */}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Paper Standard
                    </label>
                    <select
                      value={paperSize}
                      onChange={(e) => setPaperSize(e.target.value)}
                      disabled={isLockedForAgency}
                      className="w-full text-sm border-slate-300 rounded-md shadow-sm focus:border-indigo-500 focus:ring-indigo-500 disabled:bg-slate-100"
                    >
                      <option value="A4">A4 (210 × 297 mm)</option>
                      <option value="A3">A3 (297 × 420 mm)</option>
                      <option value="LETTER">Letter (216 × 279 mm)</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Print Sides
                    </label>
                    <select
                      value={sides}
                      onChange={(e) => setSides(e.target.value)}
                      disabled={isLockedForAgency}
                      className="w-full text-sm border-slate-300 rounded-md shadow-sm focus:border-indigo-500 focus:ring-indigo-500 disabled:bg-slate-100"
                    >
                      <option value="DUPLEX">Duplex (Front & Back Mirrored)</option>
                      <option value="FRONT_ONLY">Front Side Only</option>
                      <option value="BACK_ONLY">Back Side Only</option>
                    </select>
                  </div>
                </div>

                {/* Quick Presets */}
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-xs font-semibold text-slate-700">Quick Grid Presets</span>
                    <span className="text-xs text-slate-500">{cardsPerPage} cards per page</span>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {[
                      { cols: 2, r: 2, label: '2×2 (4 cards)' },
                      { cols: 2, r: 3, label: '2×3 (6 cards)' },
                      { cols: 3, r: 3, label: '3×3 (9 cards - A4 standard)' },
                      { cols: 2, r: 4, label: '2×4 (8 cards)' },
                      { cols: 2, r: 5, label: '2×5 (10 cards)' },
                    ].map((p) => (
                      <button
                        key={p.label}
                        type="button"
                        onClick={() => applyPreset(p.cols, p.r)}
                        disabled={isLockedForAgency}
                        className={`text-xs px-2.5 py-1 rounded border transition-colors ${
                          columns === p.cols && rows === p.r
                            ? 'bg-indigo-600 text-white border-indigo-600 font-semibold'
                            : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-50 disabled:opacity-60'
                        }`}
                      >
                        {p.label}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Custom Columns & Rows Steppers */}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Columns (Horizontal)
                    </label>
                    <input
                      type="number"
                      min={1}
                      max={6}
                      value={columns}
                      onChange={(e) => setColumns(Math.max(1, Math.min(6, parseInt(e.target.value) || 1)))}
                      disabled={isLockedForAgency}
                      className="w-full text-sm border-slate-300 rounded-md shadow-sm focus:border-indigo-500 focus:ring-indigo-500 disabled:bg-slate-100"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Rows (Vertical)
                    </label>
                    <input
                      type="number"
                      min={1}
                      max={8}
                      value={rows}
                      onChange={(e) => setRows(Math.max(1, Math.min(8, parseInt(e.target.value) || 1)))}
                      disabled={isLockedForAgency}
                      className="w-full text-sm border-slate-300 rounded-md shadow-sm focus:border-indigo-500 focus:ring-indigo-500 disabled:bg-slate-100"
                    />
                  </div>
                </div>

                {/* Spacing / Gaps Controls */}
                <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg flex flex-col gap-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                      <Sliders className="w-3.5 h-3.5 text-indigo-600" />
                      Card Spacing / Gutters (mm)
                    </span>
                    <span className="text-[11px] text-slate-500">Fine prepress registration</span>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <div className="flex justify-between text-xs text-slate-600 mb-1">
                        <span>Horizontal Gap:</span>
                        <span className="font-semibold text-slate-900">{hGap} mm</span>
                      </div>
                      <input
                        type="range"
                        min={0}
                        max={20}
                        step={0.5}
                        value={hGap}
                        onChange={(e) => setHGap(parseFloat(e.target.value) || 0)}
                        disabled={isLockedForAgency}
                        className="w-full accent-indigo-600 disabled:opacity-50"
                      />
                    </div>

                    <div>
                      <div className="flex justify-between text-xs text-slate-600 mb-1">
                        <span>Vertical Gap:</span>
                        <span className="font-semibold text-slate-900">{vGap} mm</span>
                      </div>
                      <input
                        type="range"
                        min={0}
                        max={20}
                        step={0.5}
                        value={vGap}
                        onChange={(e) => setVGap(parseFloat(e.target.value) || 0)}
                        disabled={isLockedForAgency}
                        className="w-full accent-indigo-600 disabled:opacity-50"
                      />
                    </div>
                  </div>
                </div>

                {/* Margins & Prepress Marks Toggles */}
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                  <label className="flex items-center gap-2 p-2 bg-white rounded border border-slate-200 text-xs text-slate-700 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={autoCenter}
                      onChange={(e) => setAutoCenter(e.target.checked)}
                      disabled={isLockedForAgency}
                      className="rounded text-indigo-600 focus:ring-indigo-500"
                    />
                    <span>Auto-Center Grid</span>
                  </label>

                  <label className="flex items-center gap-2 p-2 bg-white rounded border border-slate-200 text-xs text-slate-700 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={showCropMarks}
                      onChange={(e) => setShowCropMarks(e.target.checked)}
                      disabled={isLockedForAgency}
                      className="rounded text-indigo-600 focus:ring-indigo-500"
                    />
                    <span>Crop / Cut Marks</span>
                  </label>

                  <label className="flex items-center gap-2 p-2 bg-white rounded border border-slate-200 text-xs text-slate-700 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={showCutGuides}
                      onChange={(e) => setShowCutGuides(e.target.checked)}
                      disabled={isLockedForAgency}
                      className="rounded text-indigo-600 focus:ring-indigo-500"
                    />
                    <span>Cutting Guides</span>
                  </label>
                </div>
              </>
            ) : (
              /* Plastic Card Mode Panel */
              <div className="p-4 bg-indigo-50/50 border border-indigo-100 rounded-lg flex flex-col gap-3">
                <div className="flex items-start gap-3">
                  <CreditCard className="w-6 h-6 text-indigo-600 flex-shrink-0 mt-0.5" />
                  <div>
                    <h4 className="text-sm font-bold text-slate-900">Direct CR80 Plastic PVC Mode</h4>
                    <p className="text-xs text-slate-600 mt-1">
                      Pre-formatted for dedicated direct-to-card PVC thermal printers (Evolis Primacy, Fargo DTC,
                      Zebra ZC300). Generates 1 exact card per page (54 × 85.6 mm) with sequential student records.
                    </p>
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Print Sides Sequence
                  </label>
                  <select
                    value={sides}
                    onChange={(e) => setSides(e.target.value)}
                    disabled={isLockedForAgency}
                    className="w-full text-sm border-slate-300 rounded-md shadow-sm focus:border-indigo-500 focus:ring-indigo-500 disabled:bg-slate-100"
                  >
                    <option value="DUPLEX">Duplex (Page 1: Front, Page 2: Back sequentially)</option>
                    <option value="FRONT_ONLY">Front Side Only</option>
                    <option value="BACK_ONLY">Back Side Only</option>
                  </select>
                </div>

                <div className="text-xs text-slate-500 bg-white p-2.5 rounded border border-slate-200">
                  Total PVC Cards to Print: <strong className="text-slate-900">{totalStudents}</strong> cards
                  ({sides === 'DUPLEX' ? totalStudents * 2 : totalStudents} total page passes)
                </div>
              </div>
            )}

            {/* Save Settings Button */}
            {!isLockedForAgency && (
              <div className="pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleSaveSettings}
                  disabled={isSaving}
                  className="w-full flex items-center justify-center gap-1.5 text-indigo-700 border-indigo-300 hover:bg-indigo-50"
                >
                  {isSaving ? <LoadingSpinner size="xs" /> : <RefreshCw className="w-3.5 h-3.5" />}
                  <span>Save Specifications as Default</span>
                </Button>
              </div>
            )}
          </div>

          {/* Interactive Live Sheet Preview Column */}
          <div className="lg:col-span-5 flex flex-col gap-3">
            <div className="flex items-center justify-between text-xs font-semibold text-slate-700">
              <span className="flex items-center gap-1.5">
                <Eye className="w-4 h-4 text-slate-500" />
                Live Prepress Layout Preview
              </span>

              {/* Front / Back Toggle for Preview */}
              {sides === 'DUPLEX' && (
                <div className="flex items-center rounded-md border border-slate-200 bg-slate-100 p-0.5 text-[11px]">
                  <button
                    type="button"
                    onClick={() => setPreviewSide('front')}
                    className={`px-2 py-0.5 rounded font-medium ${
                      previewSide === 'front' ? 'bg-white text-indigo-700 shadow-sm' : 'text-slate-600'
                    }`}
                  >
                    Front Sheet
                  </button>
                  <button
                    type="button"
                    onClick={() => setPreviewSide('back')}
                    className={`px-2 py-0.5 rounded font-medium ${
                      previewSide === 'back' ? 'bg-white text-indigo-700 shadow-sm' : 'text-slate-600'
                    }`}
                  >
                    Back (Mirrored)
                  </button>
                </div>
              )}
            </div>

            {/* Visual Sheet Canvas Container */}
            <div className="bg-slate-200/80 p-3 rounded-lg flex items-center justify-center min-h-[340px] border border-slate-300/80 shadow-inner">
              <div
                className="bg-white rounded shadow-md border border-slate-300 p-3 transition-all relative"
                style={{
                  width: printMode === 'PLASTIC_CARD' ? '180px' : '230px',
                  height: printMode === 'PLASTIC_CARD' ? '280px' : '325px',
                  aspectRatio: printMode === 'PLASTIC_CARD' ? '54 / 85.6' : paperSize === 'A3' ? '297 / 420' : '210 / 297'
                }}
              >
                {/* Header margin tag */}
                <div className="text-[8px] font-mono text-slate-400 border-b border-slate-100 pb-1 mb-2 truncate">
                  {paperSize} • {previewSide.toUpperCase()} {sides === 'DUPLEX' && previewSide === 'back' ? '(MIRRORED)' : ''}
                </div>

                {/* Card Slots Dynamic Grid */}
                <div
                  className="w-full h-[calc(100%-20px)] grid items-center justify-center"
                  style={{
                    gridTemplateColumns: `repeat(${printMode === 'PLASTIC_CARD' ? 1 : columns}, minmax(0, 1fr))`,
                    gridTemplateRows: `repeat(${printMode === 'PLASTIC_CARD' ? 1 : rows}, minmax(0, 1fr))`,
                    gap: `${Math.min(10, Math.max(1, hGap * 0.8))}px ${Math.min(10, Math.max(1, vGap * 0.8))}px`
                  }}
                >
                  {Array.from({ length: cardsPerPage }).map((_, idx) => {
                    const colIdx = idx % columns;
                    const displayCol = (sides === 'DUPLEX' && previewSide === 'back') ? (columns - 1 - colIdx) : colIdx;
                    return (
                      <div
                        key={idx}
                        className={`w-full h-full rounded border flex flex-col items-center justify-center p-1 relative overflow-hidden transition-all ${
                          showCropMarks ? 'border-dashed border-indigo-300/80 bg-slate-50' : 'border-slate-200 bg-white'
                        }`}
                      >
                        {/* Crop marks visual tick marks */}
                        {showCropMarks && (
                          <>
                            <div className="absolute top-0 left-0 w-1.5 h-1.5 border-t border-l border-slate-400" />
                            <div className="absolute top-0 right-0 w-1.5 h-1.5 border-t border-r border-slate-400" />
                            <div className="absolute bottom-0 left-0 w-1.5 h-1.5 border-b border-l border-slate-400" />
                            <div className="absolute bottom-0 right-0 w-1.5 h-1.5 border-b border-r border-slate-400" />
                          </>
                        )}

                        <div className="w-4 h-4 rounded-full bg-slate-300 flex items-center justify-center text-[7px] text-slate-600 font-bold mb-0.5">
                          {idx + 1}
                        </div>
                        <div className="w-full h-1 bg-indigo-200 rounded mb-0.5" />
                        <div className="w-2/3 h-0.5 bg-slate-200 rounded" />
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Calculations Summary */}
            <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-md text-xs text-slate-600 flex justify-between items-center">
              <span>Roster: <strong>{totalStudents} cards</strong></span>
              <span>Layout: <strong>{columns}×{rows} ({cardsPerPage}/pg)</strong></span>
              <span>Sheets: <strong className="text-indigo-600">{totalSheetsNeeded}</strong></span>
            </div>
          </div>
        </div>

        {/* Action Buttons Bar */}
        <div className="pt-3 border-t border-slate-200 flex flex-wrap items-center justify-between gap-2.5">
          <Button variant="outline" size="sm" onClick={onClose} disabled={!!downloadingType}>
            Cancel
          </Button>

          <div className="flex flex-wrap items-center gap-2">
            {/* Download JPEG Button */}
            {printMode === 'SHEET' && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleDownloadSheetImage}
                disabled={!!downloadingType}
                className="text-slate-700 hover:bg-slate-100 flex items-center gap-1.5"
                title="Download 300 DPI high-resolution JPEG of current imposition sheet"
              >
                {downloadingType === 'image' ? <LoadingSpinner size="xs" /> : <Download className="w-4 h-4 text-emerald-600" />}
                <span>Download JPEG</span>
              </Button>
            )}

            {/* Download Plastic PDF */}
            {printMode === 'PLASTIC_CARD' ? (
              <Button
                variant="primary"
                size="sm"
                onClick={handleDownloadPlasticPdf}
                disabled={!!downloadingType}
                className="bg-indigo-600 hover:bg-indigo-700 text-white flex items-center gap-1.5"
              >
                {downloadingType === 'plastic' ? <LoadingSpinner size="xs" /> : <CreditCard className="w-4 h-4" />}
                <span>Download CR80 PVC Direct PDF</span>
              </Button>
            ) : (
              <Button
                variant="outline"
                size="sm"
                onClick={handleDownloadSheetPdf}
                disabled={!!downloadingType}
                className="border-indigo-300 text-indigo-700 hover:bg-indigo-50 flex items-center gap-1.5"
                title="Download Imposition Sheet PDF with crop marks and duplex mirroring"
              >
                {downloadingType === 'pdf' ? <LoadingSpinner size="xs" /> : <FileDown className="w-4 h-4 text-indigo-600" />}
                <span>Download Imposition PDF</span>
              </Button>
            )}

            {/* Direct Native Print Button */}
            <Button
              variant="primary"
              size="sm"
              onClick={handleDirectPrint}
              disabled={!!downloadingType}
              className="bg-indigo-600 hover:bg-indigo-700 text-white flex items-center gap-1.5 shadow-sm"
              title="Open browser print dialog with generated imposition sheets"
            >
              {downloadingType === 'direct' ? <LoadingSpinner size="xs" /> : <Printer className="w-4 h-4" />}
              <span>Direct Print</span>
            </Button>
          </div>
        </div>
      </div>
    </Modal>
  );
}
