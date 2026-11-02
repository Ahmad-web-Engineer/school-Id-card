import React from 'react';
import { PLACEHOLDERS } from './templatesConstants';
import {
  Type,
  User,
  Image as ImageIcon,
  QrCode,
  Barcode,
  Layers,
  FileSignature,
  Maximize2,
  Plus,
} from 'lucide-react';
import Button from '../../components/common/Button';

export default function ElementPalette({ onAddElement, cardWidthMm = 85.6, cardHeightMm = 54.0 }) {
  const handleAddPlaceholder = (placeholder) => {
    onAddElement({
      id: `el-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      type: 'text',
      field: placeholder.key.replace(/[{}]/g, ''),
      content: placeholder.key,
      x: 10,
      y: 20,
      width: Math.min(cardWidthMm - 20, 50),
      height: 5,
      fontSize: 8.5,
      fontWeight: 'normal',
      color: '#0f172a',
      textAlign: 'left',
    });
  };

  const handleAddCustomText = () => {
    onAddElement({
      id: `el-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      type: 'text',
      content: 'Custom Text',
      x: 10,
      y: 20,
      width: 40,
      height: 5,
      fontSize: 8.5,
      fontWeight: 'semibold',
      color: '#1e293b',
      textAlign: 'left',
    });
  };

  const handleAddPhotoBox = () => {
    onAddElement({
      id: `el-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      type: 'image',
      field: 'student_photo',
      x: 6,
      y: 15,
      width: 22,
      height: 27,
      borderColor: '#cbd5e1',
      borderWidth: 1,
      borderRadius: 2,
      label: 'Student Photo',
    });
  };

  const handleAddLogoBox = () => {
    onAddElement({
      id: `el-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      type: 'image',
      field: 'school_logo',
      x: 4,
      y: 2,
      width: 10,
      height: 10,
      label: 'School Logo',
    });
  };

  const handleAddQrCode = () => {
    onAddElement({
      id: `el-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      type: 'qrcode',
      field: 'student_qr',
      x: cardWidthMm - 22,
      y: 18,
      width: 16,
      height: 16,
      label: 'QR Code',
    });
  };

  const handleAddBarcode = () => {
    onAddElement({
      id: `el-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      type: 'barcode',
      field: 'student_barcode',
      x: 6,
      y: cardHeightMm - 14,
      width: 40,
      height: 9,
      label: 'Barcode',
    });
  };

  const handleAddHeaderBanner = () => {
    onAddElement({
      id: `el-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      type: 'rect',
      x: 0,
      y: 0,
      width: cardWidthMm,
      height: 12,
      backgroundColor: '#1e3a8a',
      borderColor: '#1e3a8a',
      borderWidth: 0,
      borderRadius: 0,
    });
  };

  const handleAddFooterBanner = () => {
    onAddElement({
      id: `el-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      type: 'rect',
      x: 0,
      y: cardHeightMm - 7,
      width: cardWidthMm,
      height: 7,
      backgroundColor: '#1e3a8a',
      borderColor: '#1e3a8a',
      borderWidth: 0,
      borderRadius: 0,
    });
  };

  const handleAddDivider = () => {
    onAddElement({
      id: `el-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      type: 'rect',
      x: 5,
      y: 20,
      width: cardWidthMm - 10,
      height: 0.5,
      backgroundColor: '#cbd5e1',
      borderColor: '#cbd5e1',
      borderWidth: 0,
      borderRadius: 0,
    });
  };

  const handleAddSignature = () => {
    onAddElement({
      id: `el-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      type: 'signature',
      label: 'Principal Signature',
      x: cardWidthMm - 35,
      y: cardHeightMm - 14,
      width: 30,
      height: 10,
      borderColor: '#94a3b8',
      borderWidth: 1,
      borderRadius: 1,
    });
  };

  return (
    <div className="space-y-4 text-xs">
      {/* Category 1: Structural & Media Elements */}
      <div>
        <h4 className="font-semibold text-slate-800 uppercase tracking-wider text-[11px] mb-2 flex items-center gap-1.5">
          <Layers className="w-3.5 h-3.5 text-indigo-600" />
          <span>Card Elements</span>
        </h4>
        <div className="grid grid-cols-2 gap-1.5">
          <button
            type="button"
            onClick={handleAddPhotoBox}
            className="flex items-center gap-2 p-2 rounded-md border border-slate-200 bg-white hover:bg-slate-50 hover:border-indigo-300 text-left transition-all text-slate-700 shadow-2xs"
          >
            <User className="w-4 h-4 text-indigo-600 shrink-0" />
            <div className="truncate">
              <span className="font-medium text-slate-900 block truncate">Photo Box</span>
              <span className="text-[10px] text-slate-400 block truncate">Portrait Frame</span>
            </div>
          </button>

          <button
            type="button"
            onClick={handleAddLogoBox}
            className="flex items-center gap-2 p-2 rounded-md border border-slate-200 bg-white hover:bg-slate-50 hover:border-indigo-300 text-left transition-all text-slate-700 shadow-2xs"
          >
            <ImageIcon className="w-4 h-4 text-sky-600 shrink-0" />
            <div className="truncate">
              <span className="font-medium text-slate-900 block truncate">School Logo</span>
              <span className="text-[10px] text-slate-400 block truncate">Emblem Box</span>
            </div>
          </button>

          <button
            type="button"
            onClick={handleAddQrCode}
            className="flex items-center gap-2 p-2 rounded-md border border-slate-200 bg-white hover:bg-slate-50 hover:border-indigo-300 text-left transition-all text-slate-700 shadow-2xs"
          >
            <QrCode className="w-4 h-4 text-emerald-600 shrink-0" />
            <div className="truncate">
              <span className="font-medium text-slate-900 block truncate">QR Code</span>
              <span className="text-[10px] text-slate-400 block truncate">Student QR</span>
            </div>
          </button>

          <button
            type="button"
            onClick={handleAddBarcode}
            className="flex items-center gap-2 p-2 rounded-md border border-slate-200 bg-white hover:bg-slate-50 hover:border-indigo-300 text-left transition-all text-slate-700 shadow-2xs"
          >
            <Barcode className="w-4 h-4 text-purple-600 shrink-0" />
            <div className="truncate">
              <span className="font-medium text-slate-900 block truncate">Barcode</span>
              <span className="text-[10px] text-slate-400 block truncate">Code128 Format</span>
            </div>
          </button>

          <button
            type="button"
            onClick={handleAddHeaderBanner}
            className="flex items-center gap-2 p-2 rounded-md border border-slate-200 bg-white hover:bg-slate-50 hover:border-indigo-300 text-left transition-all text-slate-700 shadow-2xs"
          >
            <Maximize2 className="w-4 h-4 text-blue-600 shrink-0" />
            <div className="truncate">
              <span className="font-medium text-slate-900 block truncate">Header Bar</span>
              <span className="text-[10px] text-slate-400 block truncate">Full-width top</span>
            </div>
          </button>

          <button
            type="button"
            onClick={handleAddFooterBanner}
            className="flex items-center gap-2 p-2 rounded-md border border-slate-200 bg-white hover:bg-slate-50 hover:border-indigo-300 text-left transition-all text-slate-700 shadow-2xs"
          >
            <Maximize2 className="w-4 h-4 text-blue-600 shrink-0" />
            <div className="truncate">
              <span className="font-medium text-slate-900 block truncate">Footer Bar</span>
              <span className="text-[10px] text-slate-400 block truncate">Full-width btm</span>
            </div>
          </button>

          <button
            type="button"
            onClick={handleAddDivider}
            className="flex items-center gap-2 p-2 rounded-md border border-slate-200 bg-white hover:bg-slate-50 hover:border-indigo-300 text-left transition-all text-slate-700 shadow-2xs"
          >
            <Layers className="w-4 h-4 text-slate-500 shrink-0" />
            <div className="truncate">
              <span className="font-medium text-slate-900 block truncate">Divider Line</span>
              <span className="text-[10px] text-slate-400 block truncate">Thin separator</span>
            </div>
          </button>

          <button
            type="button"
            onClick={handleAddSignature}
            className="flex items-center gap-2 p-2 rounded-md border border-slate-200 bg-white hover:bg-slate-50 hover:border-indigo-300 text-left transition-all text-slate-700 shadow-2xs"
          >
            <FileSignature className="w-4 h-4 text-amber-600 shrink-0" />
            <div className="truncate">
              <span className="font-medium text-slate-900 block truncate">Signature</span>
              <span className="text-[10px] text-slate-400 block truncate">Principal Sign</span>
            </div>
          </button>
        </div>
      </div>

      {/* Category 2: Text / Student Dynamic Placeholders */}
      <div>
        <div className="flex items-center justify-between mb-2">
          <h4 className="font-semibold text-slate-800 uppercase tracking-wider text-[11px] flex items-center gap-1.5">
            <Type className="w-3.5 h-3.5 text-indigo-600" />
            <span>Placeholders & Text</span>
          </h4>
          <button
            type="button"
            onClick={handleAddCustomText}
            className="text-[10px] font-medium text-indigo-600 hover:text-indigo-800 inline-flex items-center gap-0.5"
          >
            <Plus className="w-3 h-3" /> Custom Text
          </button>
        </div>

        <div className="space-y-1 max-h-52 overflow-y-auto pr-1">
          {PLACEHOLDERS.map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => handleAddPlaceholder(item)}
              className="w-full flex items-center justify-between p-1.5 rounded border border-slate-100 bg-slate-50/70 hover:bg-indigo-50/60 hover:border-indigo-200 text-left transition-all group"
            >
              <div className="flex items-center gap-1.5 truncate">
                <span className="font-mono text-[10px] text-indigo-700 bg-indigo-100/60 px-1 py-0.5 rounded truncate">
                  {item.key}
                </span>
                <span className="text-slate-600 text-[11px] font-medium truncate">
                  {item.label}
                </span>
              </div>
              <Plus className="w-3.5 h-3.5 text-slate-400 group-hover:text-indigo-600 shrink-0" />
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
