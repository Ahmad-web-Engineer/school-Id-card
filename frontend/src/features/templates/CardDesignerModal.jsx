import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import CardCanvas from './CardCanvas';
import ElementPalette from './ElementPalette';
import PropertyInspector from './PropertyInspector';
import JsonPreviewModal from './JsonPreviewModal';
import {
  DEFAULT_FRONT_DESIGN,
  DEFAULT_BACK_DESIGN,
  CARD_STANDARDS,
  SAMPLE_STUDENT_DATA,
} from './templatesConstants';
import {
  FileDown,
  Send,
  Lock,
  Save,
  RotateCw,
  Eye,
  Code,
  AlertTriangle,
  CheckCircle2,
  ChevronRight,
  ZoomIn,
  ZoomOut,
  Maximize2,
  X,
} from 'lucide-react';

export default function CardDesignerModal({
  isOpen,
  onClose,
  templateId,
  templateName: initialTemplateName = 'ID Card Template',
}) {
  const queryClient = useQueryClient();

  // Active side: 'front' | 'back'
  const [activeSide, setActiveSide] = useState('front');

  // Dimensions & Orientation
  const [widthMm, setWidthMm] = useState(85.6);
  const [heightMm, setHeightMm] = useState(54.0);
  const [bleedMarginMm, setBleedMarginMm] = useState(3.0);
  const [showBleed, setShowBleed] = useState(true);
  const [zoomScale, setZoomScale] = useState(4.8);

  // Design elements for both sides
  const [frontElements, setFrontElements] = useState(DEFAULT_FRONT_DESIGN.elements);
  const [backElements, setBackElements] = useState(DEFAULT_BACK_DESIGN.elements);
  const [selectedElementId, setSelectedElementId] = useState(null);

  // Preview & Inspector toggles
  const [useSampleData, setUseSampleData] = useState(true);
  const [isJsonModalOpen, setIsJsonModalOpen] = useState(false);

  // Current selected version
  const [selectedVersionId, setSelectedVersionId] = useState(null);

  // Status & Notification feedback
  const [actionMessage, setActionMessage] = useState(null);
  const [actionError, setActionError] = useState(null);

  // Fetch full template details with version history
  const { data: templateResponse, isLoading, refetch } = useQuery({
    queryKey: ['admin-template-detail', templateId],
    queryFn: () => api.get(`/admin/templates/${templateId}`),
    enabled: isOpen && !!templateId,
  });

  const template = templateResponse?.data;
  const versions = template?.versions || [];

  // Active version object
  const currentVersion =
    versions.find((v) => v.id === selectedVersionId) ||
    versions[versions.length - 1] ||
    versions[0] ||
    null;

  // On template loaded or version changed, populate design elements
  useEffect(() => {
    if (currentVersion) {
      setWidthMm(currentVersion.cardWidthMm || 85.6);
      setHeightMm(currentVersion.cardHeightMm || 54.0);
      setBleedMarginMm(currentVersion.bleedMarginMm || 3.0);

      try {
        if (currentVersion.frontDesignJson) {
          const parsed = typeof currentVersion.frontDesignJson === 'string'
            ? JSON.parse(currentVersion.frontDesignJson)
            : currentVersion.frontDesignJson;
          if (parsed && Array.isArray(parsed.elements)) {
            setFrontElements(parsed.elements);
          }
        }
      } catch (e) {
        console.error('Failed to parse front design JSON', e);
      }

      try {
        if (currentVersion.backDesignJson) {
          const parsed = typeof currentVersion.backDesignJson === 'string'
            ? JSON.parse(currentVersion.backDesignJson)
            : currentVersion.backDesignJson;
          if (parsed && Array.isArray(parsed.elements)) {
            setBackElements(parsed.elements);
          }
        }
      } catch (e) {
        console.error('Failed to parse back design JSON', e);
      }
    }
  }, [currentVersion?.id]);

  // Current elements based on active side
  const currentElements = activeSide === 'front' ? frontElements : backElements;
  const setCurrentElements = activeSide === 'front' ? setFrontElements : setBackElements;

  const selectedElement = currentElements.find((el) => el.id === selectedElementId) || null;

  // Element CRUD handlers
  const handleAddElement = (newElement) => {
    setCurrentElements((prev) => [...prev, newElement]);
    setSelectedElementId(newElement.id);
  };

  const handleUpdateElement = (id, updates) => {
    setCurrentElements((prev) =>
      prev.map((el) => (el.id === id ? { ...el, ...updates } : el))
    );
  };

  const handleDeleteElement = (id) => {
    setCurrentElements((prev) => prev.filter((el) => el.id !== id));
    if (selectedElementId === id) {
      setSelectedElementId(null);
    }
  };

  const handleDuplicateElement = (id) => {
    const el = currentElements.find((item) => item.id === id);
    if (!el) return;
    const duplicated = {
      ...el,
      id: `el-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      x: Math.min(widthMm - (el.width || 10), el.x + 2),
      y: Math.min(heightMm - (el.height || 5), el.y + 2),
    };
    setCurrentElements((prev) => [...prev, duplicated]);
    setSelectedElementId(duplicated.id);
  };

  // Orientation toggle
  const toggleOrientation = () => {
    const newWidth = heightMm;
    const newHeight = widthMm;
    setWidthMm(newWidth);
    setHeightMm(newHeight);
  };

  // Status badge styling helper
  const getApprovalBadge = (status) => {
    switch (status) {
      case 'APPROVED':
        return <Badge variant="success">APPROVED & LOCKED</Badge>;
      case 'PENDING_APPROVAL':
        return <Badge variant="warning">PENDING APPROVAL</Badge>;
      case 'ARCHIVED':
        return <Badge variant="neutral">ARCHIVED</Badge>;
      case 'DRAFT':
      default:
        return <Badge variant="info">DRAFT</Badge>;
    }
  };

  // 1. Mutation: Save New Version
  const saveVersionMutation = useMutation({
    mutationFn: (versionPayload) =>
      api.post(`/admin/templates/${templateId}/versions`, versionPayload),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['admin-template-detail', templateId] });
      queryClient.invalidateQueries({ queryKey: ['admin-templates'] });
      setActionMessage('New draft version saved successfully!');
      setTimeout(() => setActionMessage(null), 4000);
      if (res?.data?.id) {
        setSelectedVersionId(res.data.id);
      }
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to save template version.');
      setTimeout(() => setActionError(null), 5000);
    },
  });

  const handleSaveDraft = () => {
    setActionError(null);
    setActionMessage(null);
    saveVersionMutation.mutate({
      cardWidthMm: widthMm,
      cardHeightMm: heightMm,
      bleedMarginMm: bleedMarginMm,
      frontDesignJson: JSON.stringify({ elements: frontElements }),
      backDesignJson: JSON.stringify({ elements: backElements }),
      autoApprove: false,
    });
  };

  const handleSaveAndPublish = () => {
    setActionError(null);
    setActionMessage(null);
    saveVersionMutation.mutate({
      cardWidthMm: widthMm,
      cardHeightMm: heightMm,
      bleedMarginMm: bleedMarginMm,
      frontDesignJson: JSON.stringify({ elements: frontElements }),
      backDesignJson: JSON.stringify({ elements: backElements }),
      autoApprove: true,
    });
  };

  // 2. Mutation: Submit for Approval
  const submitVersionMutation = useMutation({
    mutationFn: (versionId) =>
      api.post(`/admin/templates/${templateId}/versions/${versionId}/submit`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-template-detail', templateId] });
      queryClient.invalidateQueries({ queryKey: ['admin-templates'] });
      setActionMessage('Template version submitted for review and approval!');
      setTimeout(() => setActionMessage(null), 4000);
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to submit version for approval.');
      setTimeout(() => setActionError(null), 5000);
    },
  });

  const handleSubmitForApproval = () => {
    if (!currentVersion?.id) return;
    setActionError(null);
    submitVersionMutation.mutate(currentVersion.id);
  };

  // 3. Mutation: Approve and Lock Version
  const approveVersionMutation = useMutation({
    mutationFn: (versionId) =>
      api.post(`/admin/templates/${templateId}/versions/${versionId}/approve`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-template-detail', templateId] });
      queryClient.invalidateQueries({ queryKey: ['admin-templates'] });
      setActionMessage('Template version approved & locked for production!');
      setTimeout(() => setActionMessage(null), 4000);
    },
    onError: (err) => {
      setActionError(err.message || 'Failed to approve template version.');
      setTimeout(() => setActionError(null), 5000);
    },
  });

  const handleApproveAndLock = () => {
    if (!currentVersion?.id) return;
    setActionError(null);
    approveVersionMutation.mutate(currentVersion.id);
  };

  // 4. Download PDF Preview
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);
  const handleDownloadPdf = async () => {
    if (!currentVersion?.id) return;
    setIsDownloadingPdf(true);
    setActionError(null);
    try {
      const response = await api.get(
        `/admin/templates/${templateId}/versions/${currentVersion.id}/preview`,
        { responseType: 'blob' }
      );
      const blob = new Blob([response], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute(
        'download',
        `template_${template?.templateName || 'preview'}_v${currentVersion.versionNumber || 1}.pdf`
      );
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      setActionMessage('PDF preview downloaded successfully!');
      setTimeout(() => setActionMessage(null), 3000);
    } catch (err) {
      setActionError(err.message || 'Failed to download preview PDF.');
      setTimeout(() => setActionError(null), 5000);
    } finally {
      setIsDownloadingPdf(false);
    }
  };

  if (!isOpen) return null;

  const isLocked = currentVersion?.approvalStatus === 'APPROVED' || currentVersion?.approvalStatus === 'ARCHIVED';

  return (
    <div
      className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-2 sm:p-4"
      role="dialog"
      aria-modal="true"
    >
      <div className="relative w-full max-w-7xl bg-white rounded-xl shadow-2xl flex flex-col h-[95vh] max-h-[920px] overflow-hidden border border-slate-200">
        {/* Top Header & Navigation Bar */}
        <header className="px-5 py-3 border-b border-slate-200 bg-slate-50/90 flex flex-wrap items-center justify-between gap-3 shrink-0">
          <div className="flex items-center gap-3">
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-slate-900 tracking-tight">
                  {template?.templateName || initialTemplateName}
                </h3>
                {currentVersion && getApprovalBadge(currentVersion.approvalStatus)}
              </div>
              <p className="text-xs text-slate-500">
                CR80 Card Designer • {widthMm} × {heightMm} mm ({widthMm > heightMm ? 'Landscape' : 'Portrait'})
              </p>
            </div>

            {/* Version Switcher if multiple versions exist */}
            {versions.length > 0 && (
              <div className="hidden sm:flex items-center gap-1.5 ml-4 pl-4 border-l border-slate-200">
                <span className="text-xs text-slate-500 font-medium">Version:</span>
                <select
                  value={currentVersion?.id || ''}
                  onChange={(e) => setSelectedVersionId(e.target.value)}
                  className="text-xs font-semibold bg-white border border-slate-200 rounded px-2 py-1 text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  {versions.map((ver) => (
                    <option key={ver.id} value={ver.id}>
                      v{ver.versionNumber} ({ver.approvalStatus})
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-2 flex-wrap">
            <Button
              variant="outline"
              size="sm"
              onClick={handleDownloadPdf}
              loading={isDownloadingPdf}
              className="flex items-center gap-1.5"
            >
              <FileDown className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">PDF Preview</span>
            </Button>

            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsJsonModalOpen(true)}
              className="flex items-center gap-1.5"
            >
              <Code className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">JSON</span>
            </Button>

            {currentVersion?.approvalStatus === 'DRAFT' && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleSubmitForApproval}
                loading={submitVersionMutation.isPending}
                className="flex items-center gap-1.5 text-amber-700 border-amber-300 hover:bg-amber-50"
              >
                <Send className="w-3.5 h-3.5" />
                <span>Submit for Approval</span>
              </Button>
            )}

            {currentVersion?.approvalStatus === 'PENDING_APPROVAL' && (
              <Button
                variant="primary"
                size="sm"
                onClick={handleApproveAndLock}
                loading={approveVersionMutation.isPending}
                className="flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-700 focus-visible:ring-emerald-500"
              >
                <Lock className="w-3.5 h-3.5" />
                <span>Approve & Lock</span>
              </Button>
            )}

            <Button
              variant="outline"
              size="sm"
              onClick={handleSaveDraft}
              loading={saveVersionMutation.isPending}
              className="flex items-center gap-1.5"
            >
              <Save className="w-3.5 h-3.5" />
              <span>{isLocked ? 'Save Draft' : 'Save Draft'}</span>
            </Button>

            <Button
              variant="primary"
              size="sm"
              onClick={handleSaveAndPublish}
              loading={saveVersionMutation.isPending}
              className="flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-700 focus-visible:ring-emerald-500"
              title="Save and automatically approve immediately for all schools"
            >
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Save & Publish</span>
            </Button>

            <button
              type="button"
              onClick={onClose}
              aria-label="Close designer"
              className="p-1.5 rounded-md text-slate-400 hover:text-slate-600 hover:bg-slate-200/60 ml-2"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </header>

        {/* Action feedback banner */}
        {actionMessage && (
          <div className="bg-emerald-50 border-b border-emerald-200 px-4 py-2 flex items-center justify-between text-xs text-emerald-800">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>{actionMessage}</span>
            </div>
            <button onClick={() => setActionMessage(null)} className="text-emerald-600 hover:text-emerald-900">
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {actionError && (
          <div className="bg-rose-50 border-b border-rose-200 px-4 py-2 flex items-center justify-between text-xs text-rose-800">
            <div className="flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
              <span>{actionError}</span>
            </div>
            <button onClick={() => setActionError(null)} className="text-rose-600 hover:text-rose-900">
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {isLocked && (
          <div className="bg-amber-50/90 border-b border-amber-200 px-4 py-2 flex items-center gap-2 text-xs text-amber-900">
            <Lock className="w-4 h-4 text-amber-600 shrink-0" />
            <span>
              <strong>This version is {currentVersion?.approvalStatus}.</strong> Existing student batches and orders are locked to this file. Any changes saved will automatically create a new draft version.
            </span>
          </div>
        )}

        {/* Sub-Header: Side Tabs, Orientation & Canvas Tools */}
        <div className="px-5 py-2.5 bg-white border-b border-slate-200 flex flex-wrap items-center justify-between gap-3 shrink-0">
          {/* Side Tabs (Front / Back) */}
          <div className="flex items-center bg-slate-100 p-0.5 rounded-lg border border-slate-200">
            <button
              type="button"
              onClick={() => {
                setActiveSide('front');
                setSelectedElementId(null);
              }}
              className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-all ${
                activeSide === 'front'
                  ? 'bg-white text-indigo-700 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Front Side ({frontElements.length} elements)
            </button>
            <button
              type="button"
              onClick={() => {
                setActiveSide('back');
                setSelectedElementId(null);
              }}
              className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-all ${
                activeSide === 'back'
                  ? 'bg-white text-indigo-700 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Back Side ({backElements.length} elements)
            </button>
          </div>

          {/* Canvas Tools */}
          <div className="flex items-center gap-3 text-xs">
            <button
              type="button"
              onClick={toggleOrientation}
              className="flex items-center gap-1.5 text-slate-600 hover:text-indigo-600 font-medium px-2 py-1 rounded hover:bg-slate-100"
            >
              <RotateCw className="w-3.5 h-3.5" />
              <span>Rotate Card</span>
            </button>

            <label className="flex items-center gap-1.5 cursor-pointer text-slate-600 hover:text-slate-900 select-none">
              <input
                type="checkbox"
                checked={showBleed}
                onChange={(e) => setShowBleed(e.target.checked)}
                className="rounded text-indigo-600 focus:ring-indigo-500 w-3.5 h-3.5"
              />
              <span>Bleed Guide (3mm)</span>
            </label>

            <label className="flex items-center gap-1.5 cursor-pointer text-slate-600 hover:text-slate-900 select-none">
              <input
                type="checkbox"
                checked={useSampleData}
                onChange={(e) => setUseSampleData(e.target.checked)}
                className="rounded text-indigo-600 focus:ring-indigo-500 w-3.5 h-3.5"
              />
              <span>Sample Student Preview</span>
            </label>

            {/* Zoom Controls */}
            <div className="hidden md:flex items-center gap-1 border-l border-slate-200 pl-3">
              <button
                type="button"
                onClick={() => setZoomScale((prev) => Math.max(3.0, +(prev - 0.5).toFixed(1)))}
                className="p-1 rounded text-slate-500 hover:bg-slate-100"
                title="Zoom Out"
              >
                <ZoomOut className="w-3.5 h-3.5" />
              </button>
              <span className="text-[11px] font-mono text-slate-600 min-w-[45px] text-center">
                {Math.round((zoomScale / 4.8) * 100)}%
              </span>
              <button
                type="button"
                onClick={() => setZoomScale((prev) => Math.min(7.0, +(prev + 0.5).toFixed(1)))}
                className="p-1 rounded text-slate-500 hover:bg-slate-100"
                title="Zoom In"
              >
                <ZoomIn className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        {/* 3-Column Designer Body */}
        <div className="flex-1 grid grid-cols-1 md:grid-cols-12 min-h-0 overflow-hidden bg-slate-100/50">
          {/* Left Column: Element Palette (col-span-3) */}
          <div className="md:col-span-3 border-r border-slate-200 bg-white p-4 overflow-y-auto">
            <ElementPalette
              onAddElement={handleAddElement}
              cardWidthMm={widthMm}
              cardHeightMm={heightMm}
            />
          </div>

          {/* Center Column: Interactive Canvas View (col-span-6) */}
          <div className="md:col-span-6 flex flex-col items-center justify-center p-4 sm:p-6 overflow-auto bg-slate-100/80">
            <CardCanvas
              widthMm={widthMm}
              heightMm={heightMm}
              bleedMarginMm={bleedMarginMm}
              showBleed={showBleed}
              elements={currentElements}
              selectedElementId={selectedElementId}
              onSelectElement={setSelectedElementId}
              onElementChange={handleUpdateElement}
              interactive={true}
              useSampleData={useSampleData}
              scale={zoomScale}
            />
          </div>

          {/* Right Column: Property Inspector (col-span-3) */}
          <div className="md:col-span-3 border-l border-slate-200 bg-white p-4 overflow-y-auto">
            <PropertyInspector
              selectedElement={selectedElement}
              onUpdateElement={handleUpdateElement}
              onDeleteElement={handleDeleteElement}
              onDuplicateElement={handleDuplicateElement}
              cardWidthMm={widthMm}
              cardHeightMm={heightMm}
            />
          </div>
        </div>
      </div>

      {/* JSON Viewer Modal */}
      <JsonPreviewModal
        isOpen={isJsonModalOpen}
        onClose={() => setIsJsonModalOpen(false)}
        frontElements={frontElements}
        backElements={backElements}
        widthMm={widthMm}
        heightMm={heightMm}
        bleedMarginMm={bleedMarginMm}
      />
    </div>
  );
}
