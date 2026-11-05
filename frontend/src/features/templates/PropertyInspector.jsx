import React, { useState } from 'react';
import {
  Trash2,
  Copy,
  AlignLeft,
  AlignCenter,
  AlignRight,
  AlignJustify,
  Bold,
  Italic,
  Underline,
  Type,
  Sliders,
  Hash,
  Palette,
  Sparkles,
  QrCode,
  Image,
} from 'lucide-react';
import {
  PLACEHOLDERS,
  FONT_FAMILIES,
  PHOTO_SHAPES,
  QR_DATA_PRESETS,
} from './templatesConstants';

const COLOR_SWATCHES = [
  '#000000',
  '#0f172a',
  '#1e3a8a',
  '#1d4ed8',
  '#0284c7',
  '#059669',
  '#881337',
  '#b91c1c',
  '#d97706',
  '#ffffff',
  '#f1f5f9',
];

export default function PropertyInspector({
  selectedElement,
  onUpdateElement,
  onDeleteElement,
  onDuplicateElement,
  cardWidthMm = 85.6,
  cardHeightMm = 54.0,
  canvasBgColor = '#ffffff',
  onUpdateCanvasBgColor,
}) {
  const [activePlaceholderCategory, setActivePlaceholderCategory] = useState('All');

  if (!selectedElement) {
    return (
      <div className="h-full flex flex-col justify-between p-4 bg-slate-50/50 rounded-lg border border-dashed border-slate-200 text-xs">
        <div className="flex flex-col items-center justify-center text-center pt-6">
          <Sliders className="w-8 h-8 text-slate-300 mb-2 stroke-[1.5]" />
          <h4 className="text-xs font-semibold text-slate-700">Canvas Settings</h4>
          <p className="text-[11px] text-slate-400 mt-1 max-w-[220px]">
            Click any element on the card to edit its properties, or customize the background color and canvas dimensions below.
          </p>
        </div>

        {/* Canvas Background Color Adjustment */}
        {onUpdateCanvasBgColor && (
          <div className="p-3 bg-white rounded-lg border border-slate-200/80 shadow-xs space-y-2.5 my-4">
            <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[10px] flex items-center gap-1.5">
              <Palette className="w-3.5 h-3.5 text-indigo-600" />
              Card Background Color
            </h5>
            <div className="flex items-center gap-2">
              <input
                type="color"
                value={canvasBgColor || '#ffffff'}
                onChange={(e) => onUpdateCanvasBgColor(e.target.value)}
                className="w-7 h-7 p-0 border border-slate-300 rounded cursor-pointer shrink-0"
              />
              <input
                type="text"
                value={canvasBgColor || '#ffffff'}
                onChange={(e) => onUpdateCanvasBgColor(e.target.value)}
                className="flex-1 px-2 py-1 border border-slate-200 rounded text-xs font-mono"
              />
            </div>
            <div className="flex flex-wrap gap-1 pt-1">
              {['#ffffff', '#f8fafc', '#f1f5f9', '#f0fdf4', '#eff6ff', '#fffbeb', '#fef2f2', '#1e3a8a', '#0f172a'].map((col) => (
                <button
                  key={col}
                  type="button"
                  onClick={() => onUpdateCanvasBgColor(col)}
                  style={{ backgroundColor: col }}
                  className={`w-4 h-4 rounded-full border ${canvasBgColor === col ? 'ring-2 ring-indigo-500 ring-offset-1 border-white' : 'border-slate-300'}`}
                  title={col}
                />
              ))}
            </div>
          </div>
        )}

        <div className="pt-3 border-t border-slate-200 w-full text-[11px] text-slate-500 space-y-1">
          <div className="flex justify-between">
            <span>Standard:</span>
            <strong className="text-slate-700">CR-80 Format</strong>
          </div>
          <div className="flex justify-between">
            <span>Dimensions:</span>
            <strong className="text-slate-700">{cardWidthMm} × {cardHeightMm} mm</strong>
          </div>
          <div className="flex justify-between">
            <span>Bleed Margin:</span>
            <strong className="text-slate-700">3.0 mm Safe Margin</strong>
          </div>
          <div className="flex justify-between">
            <span>Print Resolution:</span>
            <strong className="text-slate-700">300 DPI Vector High-Res</strong>
          </div>
        </div>
      </div>
    );
  }

  const handleChange = (field, value) => {
    onUpdateElement(selectedElement.id, { [field]: value });
  };

  const handleNumberChange = (field, value) => {
    const num = parseFloat(value);
    if (!isNaN(num)) {
      handleChange(field, num);
    }
  };

  const isBold =
    selectedElement.fontWeight === 'bold' ||
    selectedElement.fontWeight === '700' ||
    selectedElement.fontWeight === '800' ||
    selectedElement.bold === true;

  const isItalic =
    selectedElement.fontStyle === 'italic' || selectedElement.italic === true;

  const isUnderline =
    selectedElement.textDecoration === 'underline' || selectedElement.underline === true;

  const isUppercase = selectedElement.textTransform === 'uppercase';

  const toggleBold = () => {
    handleChange('fontWeight', isBold ? 'normal' : 'bold');
    handleChange('bold', !isBold);
  };

  const toggleItalic = () => {
    handleChange('fontStyle', isItalic ? 'normal' : 'italic');
    handleChange('italic', !isItalic);
  };

  const toggleUnderline = () => {
    handleChange('textDecoration', isUnderline ? 'none' : 'underline');
    handleChange('underline', !isUnderline);
  };

  const toggleUppercase = () => {
    handleChange('textTransform', isUppercase ? 'none' : 'uppercase');
  };

  const categories = ['All', 'Student', 'Academic', 'Medical', 'Guardian', 'Contact', 'School'];
  const filteredPlaceholders =
    activePlaceholderCategory === 'All'
      ? PLACEHOLDERS
      : PLACEHOLDERS.filter((p) => p.category === activePlaceholderCategory);

  return (
    <div className="space-y-4 text-xs">
      {/* Top Header: Element Identification & Quick Action Buttons */}
      <div className="flex items-center justify-between pb-2 border-b border-slate-200">
        <div>
          <span className="font-bold uppercase tracking-wider text-[11px] text-slate-800 flex items-center gap-1">
            <Sliders className="w-3.5 h-3.5 text-indigo-600" />
            {selectedElement.type.toUpperCase()} ELEMENT
          </span>
          <span className="text-[10px] text-slate-400 font-mono block">
            ID: {selectedElement.id}
          </span>
        </div>

        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => onDuplicateElement(selectedElement.id)}
            className="p-1 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded transition-colors"
            title="Duplicate Element"
          >
            <Copy className="w-4 h-4" />
          </button>
          <button
            type="button"
            onClick={() => onDeleteElement(selectedElement.id)}
            className="p-1 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded transition-colors"
            title="Delete Element"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Geometry & Coordinates Panel (mm) */}
      <div className="space-y-2">
        <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[10px] flex items-center gap-1">
          <Hash className="w-3 h-3" /> Position & Size (mm)
        </h5>

        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-0.5">X Position</label>
            <input
              type="number"
              step="0.5"
              min="0"
              max={cardWidthMm}
              value={selectedElement.x ?? 0}
              onChange={(e) => handleNumberChange('x', e.target.value)}
              className="w-full px-2 py-1 border border-slate-200 rounded text-xs font-mono focus:ring-1 focus:ring-indigo-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-0.5">Y Position</label>
            <input
              type="number"
              step="0.5"
              min="0"
              max={cardHeightMm}
              value={selectedElement.y ?? 0}
              onChange={(e) => handleNumberChange('y', e.target.value)}
              className="w-full px-2 py-1 border border-slate-200 rounded text-xs font-mono focus:ring-1 focus:ring-indigo-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-0.5">Width</label>
            <input
              type="number"
              step="0.5"
              min="2"
              max={cardWidthMm}
              value={selectedElement.width ?? 20}
              onChange={(e) => handleNumberChange('width', e.target.value)}
              className="w-full px-2 py-1 border border-slate-200 rounded text-xs font-mono focus:ring-1 focus:ring-indigo-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-0.5">Height</label>
            <input
              type="number"
              step="0.5"
              min="2"
              max={cardHeightMm}
              value={selectedElement.height ?? 10}
              onChange={(e) => handleNumberChange('height', e.target.value)}
              className="w-full px-2 py-1 border border-slate-200 rounded text-xs font-mono focus:ring-1 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
        </div>
      </div>

      {/* Typography Styling Panel (Text Elements) */}
      {selectedElement.type === 'text' && (
        <div className="space-y-3 pt-2 border-t border-slate-100">
          <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[10px] flex items-center gap-1">
            <Type className="w-3 h-3" /> Typography & Colors
          </h5>

          {/* Font Family */}
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">Font Family</label>
            <select
              value={selectedElement.fontFamily || 'Inter, sans-serif'}
              onChange={(e) => handleChange('fontFamily', e.target.value)}
              className="w-full px-2 py-1.5 border border-slate-200 rounded text-xs bg-white focus:ring-1 focus:ring-indigo-500 focus:outline-none"
            >
              {FONT_FAMILIES.map((f) => (
                <option key={f.value} value={f.value} style={{ fontFamily: f.value }}>
                  {f.label}
                </option>
              ))}
            </select>
          </div>

          {/* Font Size (Slider + Number) */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-[10px] text-slate-500 font-medium">Font Size</label>
              <span className="text-[10px] font-mono font-bold text-slate-700">
                {selectedElement.fontSize || 8} pt
              </span>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="range"
                min="4"
                max="32"
                step="0.5"
                value={selectedElement.fontSize || 8}
                onChange={(e) => handleNumberChange('fontSize', e.target.value)}
                className="flex-1 accent-indigo-600"
              />
              <input
                type="number"
                min="4"
                max="36"
                step="0.5"
                value={selectedElement.fontSize || 8}
                onChange={(e) => handleNumberChange('fontSize', e.target.value)}
                className="w-14 px-1.5 py-1 border border-slate-200 rounded text-xs text-center font-mono"
              />
            </div>
          </div>

          {/* Font Style Toggles */}
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">Format & Transform</label>
            <div className="grid grid-cols-4 gap-1 bg-slate-100 p-1 rounded-md">
              <button
                type="button"
                onClick={toggleBold}
                className={`py-1 rounded flex items-center justify-center font-bold text-xs transition-colors ${
                  isBold ? 'bg-indigo-600 text-white shadow-xs' : 'text-slate-700 hover:bg-slate-200'
                }`}
              >
                <Bold className="w-3.5 h-3.5" />
              </button>
              <button
                type="button"
                onClick={toggleItalic}
                className={`py-1 rounded flex items-center justify-center italic text-xs transition-colors ${
                  isItalic ? 'bg-indigo-600 text-white shadow-xs' : 'text-slate-700 hover:bg-slate-200'
                }`}
              >
                <Italic className="w-3.5 h-3.5" />
              </button>
              <button
                type="button"
                onClick={toggleUnderline}
                className={`py-1 rounded flex items-center justify-center text-xs transition-colors ${
                  isUnderline ? 'bg-indigo-600 text-white shadow-xs' : 'text-slate-700 hover:bg-slate-200'
                }`}
              >
                <Underline className="w-3.5 h-3.5" />
              </button>
              <button
                type="button"
                onClick={toggleUppercase}
                className={`py-1 rounded flex items-center justify-center font-mono font-bold text-[10px] transition-colors ${
                  isUppercase ? 'bg-indigo-600 text-white shadow-xs' : 'text-slate-700 hover:bg-slate-200'
                }`}
              >
                TT
              </button>
            </div>
          </div>

          {/* Text Alignment */}
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">Alignment</label>
            <div className="flex rounded border border-slate-200 overflow-hidden">
              {[
                { val: 'left', icon: AlignLeft },
                { val: 'center', icon: AlignCenter },
                { val: 'right', icon: AlignRight },
                { val: 'justify', icon: AlignJustify },
              ].map(({ val, icon: Icon }) => (
                <button
                  key={val}
                  type="button"
                  onClick={() => handleChange('textAlign', val)}
                  className={`flex-1 py-1.5 flex items-center justify-center transition-colors ${
                    (selectedElement.textAlign || 'left') === val
                      ? 'bg-indigo-600 text-white'
                      : 'bg-white text-slate-600 hover:bg-slate-50'
                  }`}
                >
                  <Icon className="w-3.5 h-3.5" />
                </button>
              ))}
            </div>
          </div>

          {/* Text Color */}
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">Text Color</label>
            <div className="flex items-center gap-2">
              <input
                type="color"
                value={selectedElement.color || '#000000'}
                onChange={(e) => handleChange('color', e.target.value)}
                className="w-7 h-7 p-0 border border-slate-300 rounded cursor-pointer"
              />
              <input
                type="text"
                value={selectedElement.color || '#000000'}
                onChange={(e) => handleChange('color', e.target.value)}
                className="flex-1 px-2 py-1 border border-slate-200 rounded text-xs font-mono"
              />
            </div>
          </div>

          {/* Content & Dynamic Data Binding */}
          <div className="pt-2 border-t border-slate-100 space-y-2">
            <label className="text-[10px] text-slate-700 font-semibold uppercase tracking-wider block">
              Content / Placeholders
            </label>
            <textarea
              rows={2}
              value={selectedElement.content || selectedElement.text || ''}
              onChange={(e) => {
                handleChange('content', e.target.value);
                handleChange('text', e.target.value);
              }}
              className="w-full px-2 py-1.5 border border-slate-200 rounded text-xs font-mono focus:ring-1 focus:ring-indigo-500 focus:outline-none resize-none"
              placeholder="Enter text or placeholders like {{student_name}}"
            />

            <div>
              <div className="flex gap-1 mb-1.5 overflow-x-auto pb-0.5">
                {categories.map((cat) => (
                  <button
                    key={cat}
                    type="button"
                    onClick={() => setActivePlaceholderCategory(cat)}
                    className={`text-[9px] px-1.5 py-0.5 rounded font-medium transition-colors ${
                      activePlaceholderCategory === cat
                        ? 'bg-indigo-600 text-white'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    {cat}
                  </button>
                ))}
              </div>

              <div className="flex flex-wrap gap-1 max-h-24 overflow-y-auto p-1 bg-slate-50 rounded border border-slate-200">
                {filteredPlaceholders.map((p) => (
                  <button
                    key={p.key}
                    type="button"
                    onClick={() => {
                      const cur = selectedElement.content || selectedElement.text || '';
                      handleChange('content', (cur ? cur + ' ' : '') + p.key);
                      handleChange('text', (cur ? cur + ' ' : '') + p.key);
                    }}
                    className="text-[9px] bg-white border border-indigo-200 text-indigo-700 hover:bg-indigo-50 px-1.5 py-0.5 rounded font-mono shadow-2xs"
                    title={p.label}
                  >
                    +{p.key}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Shapes & Banners Styling */}
      {selectedElement.type === 'rect' && (
        <div className="space-y-3 pt-2 border-t border-slate-100">
          <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[10px]">
            Shape Appearance
          </h5>

          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">
              Background Color
            </label>
            <div className="flex items-center gap-2">
              <input
                type="color"
                value={selectedElement.backgroundColor || '#1e3a8a'}
                onChange={(e) => handleChange('backgroundColor', e.target.value)}
                className="w-7 h-7 p-0 border border-slate-300 rounded cursor-pointer"
              />
              <input
                type="text"
                value={selectedElement.backgroundColor || '#1e3a8a'}
                onChange={(e) => handleChange('backgroundColor', e.target.value)}
                className="flex-1 px-2 py-1 border border-slate-200 rounded text-xs font-mono"
              />
            </div>
            <div className="mt-1.5 flex gap-1">
              {COLOR_SWATCHES.map((color) => (
                <button
                  key={color}
                  type="button"
                  onClick={() => handleChange('backgroundColor', color)}
                  style={{ backgroundColor: color }}
                  className="w-4 h-4 rounded-full border border-slate-300 hover:scale-110 transition-transform"
                />
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="text-[10px] text-slate-500 font-medium block mb-1">
                Corner Radius (mm)
              </label>
              <input
                type="number"
                step="0.5"
                min="0"
                max="20"
                value={selectedElement.borderRadius || 0}
                onChange={(e) => handleNumberChange('borderRadius', e.target.value)}
                className="w-full px-2 py-1 border border-slate-200 rounded text-xs focus:ring-1 focus:ring-indigo-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="text-[10px] text-slate-500 font-medium block mb-1">
                Border Width (px)
              </label>
              <input
                type="number"
                min="0"
                max="5"
                value={selectedElement.borderWidth || 0}
                onChange={(e) => handleNumberChange('borderWidth', e.target.value)}
                className="w-full px-2 py-1 border border-slate-200 rounded text-xs focus:ring-1 focus:ring-indigo-500 focus:outline-none"
              />
            </div>
          </div>
        </div>
      )}

      {/* Frame Styling for Images (Photo, Logo) with Photo Shapes */}
      {selectedElement.type === 'image' && (
        <div className="space-y-3 pt-2 border-t border-slate-100">
          <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[10px] flex items-center gap-1.5">
            <Image className="w-3.5 h-3.5 text-indigo-600" />
            Photo Frame & Shapes
          </h5>

          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1.5">
              Photo Shape
            </label>
            <div className="grid grid-cols-2 gap-1.5">
              {PHOTO_SHAPES.map((ps) => {
                const currentShape = selectedElement.shape || selectedElement.photoShape || 'rounded';
                const isSelected = currentShape === ps.value;
                return (
                  <button
                    key={ps.value}
                    type="button"
                    onClick={() => {
                      handleChange('shape', ps.value);
                      handleChange('photoShape', ps.value);
                      if (ps.value === 'square') {
                        handleChange('borderRadius', 0);
                      } else if (ps.value === 'circle' || ps.value === 'pill') {
                        handleChange('borderRadius', 20);
                      } else {
                        handleChange('borderRadius', 2.5);
                      }
                    }}
                    className={`px-2.5 py-1.5 rounded-md text-xs font-medium border text-left transition-all ${
                      isSelected
                        ? 'bg-indigo-50 border-indigo-500 text-indigo-700 font-semibold shadow-2xs'
                        : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    {ps.label}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="text-[10px] text-slate-500 font-medium block mb-1">
                Border Width (px)
              </label>
              <input
                type="number"
                min="0"
                max="6"
                value={selectedElement.borderWidth !== undefined ? selectedElement.borderWidth : 1}
                onChange={(e) => handleNumberChange('borderWidth', e.target.value)}
                className="w-full px-2 py-1 border border-slate-200 rounded text-xs"
              />
            </div>
            <div>
              <label className="text-[10px] text-slate-500 font-medium block mb-1">
                Border Color
              </label>
              <div className="flex items-center gap-1.5">
                <input
                  type="color"
                  value={selectedElement.borderColor || '#cbd5e1'}
                  onChange={(e) => handleChange('borderColor', e.target.value)}
                  className="w-7 h-7 p-0 border border-slate-300 rounded cursor-pointer"
                />
                <input
                  type="text"
                  value={selectedElement.borderColor || '#cbd5e1'}
                  onChange={(e) => handleChange('borderColor', e.target.value)}
                  className="w-full px-1.5 py-1 border border-slate-200 rounded text-xs font-mono"
                />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* QR Code Data Source Configuration */}
      {(selectedElement.type === 'qrcode' || selectedElement.type === 'qr') && (
        <div className="space-y-3 pt-2 border-t border-slate-100">
          <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[10px] flex items-center gap-1.5">
            <QrCode className="w-3.5 h-3.5 text-indigo-600" />
            QR Code Real Data Feed
          </h5>

          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">
              Select Data Template
            </label>
            <select
              value={selectedElement.dataFeed || 'vcard'}
              onChange={(e) => {
                const presetId = e.target.value;
                handleChange('dataFeed', presetId);
                const p = QR_DATA_PRESETS.find((x) => x.id === presetId);
                if (p && p.template) {
                  handleChange('content', p.template);
                  handleChange('data', p.template);
                }
              }}
              className="w-full px-2 py-1.5 border border-slate-200 rounded text-xs bg-white focus:ring-1 focus:ring-indigo-500"
            >
              {QR_DATA_PRESETS.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">
              Encoded Content (Supports Dynamic Placeholders)
            </label>
            <textarea
              rows={4}
              value={selectedElement.content || selectedElement.data || ''}
              onChange={(e) => {
                handleChange('content', e.target.value);
                handleChange('data', e.target.value);
              }}
              className="w-full px-2 py-1.5 border border-slate-200 rounded text-xs font-mono focus:ring-1 focus:ring-indigo-500 resize-none"
              placeholder="e.g. BEGIN:VCARD... or {{admission_number}}"
            />
          </div>

          <div>
            <span className="text-[10px] text-slate-500 font-medium block mb-1">
              Quick Add Detail Tags:
            </span>
            <div className="flex flex-wrap gap-1">
              {[
                '{{student_name}}',
                '{{admission_number}}',
                '{{class}}',
                '{{section}}',
                '{{roll_number}}',
                '{{blood_group}}',
                '{{date_of_birth}}',
                '{{emergency_contact}}',
                '{{school_name}}',
              ].map((tag) => (
                <button
                  key={tag}
                  type="button"
                  onClick={() => {
                    const cur = selectedElement.content || selectedElement.data || '';
                    handleChange('content', (cur ? cur + ' ' : '') + tag);
                    handleChange('data', (cur ? cur + ' ' : '') + tag);
                  }}
                  className="text-[9px] bg-indigo-50 border border-indigo-200 text-indigo-700 px-1.5 py-0.5 rounded font-mono hover:bg-indigo-100"
                >
                  +{tag}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Signature Box Settings */}
      {selectedElement.type === 'signature' && (
        <div className="space-y-3 pt-2 border-t border-slate-100">
          <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[10px]">
            Signature Label
          </h5>
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">
              Signatory Title / Name
            </label>
            <input
              type="text"
              value={selectedElement.label || selectedElement.content || 'Principal Signature'}
              onChange={(e) => {
                handleChange('label', e.target.value);
                handleChange('content', e.target.value);
              }}
              className="w-full px-2 py-1 border border-slate-200 rounded text-xs"
            />
          </div>
        </div>
      )}
    </div>
  );
}
