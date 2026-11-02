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
} from 'lucide-react';
import { PLACEHOLDERS, FONT_FAMILIES } from './templatesConstants';

const COLOR_SWATCHES = [
  '#000000',
  '#0f172a',
  '#1e3a8a',
  '#1d4ed8',
  '#0284c7',
  '#059669',
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
}) {
  const [activePlaceholderCategory, setActivePlaceholderCategory] = useState('All');

  if (!selectedElement) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-center p-6 bg-slate-50/50 rounded-lg border border-dashed border-slate-200">
        <Sliders className="w-8 h-8 text-slate-300 mb-2 stroke-[1.5]" />
        <h4 className="text-xs font-semibold text-slate-700">No Element Selected</h4>
        <p className="text-[11px] text-slate-400 mt-1 max-w-[200px]">
          Click any text, photo, shape, or barcode on the card canvas to customize its coordinates, font family, size, and styling.
        </p>
        <div className="mt-4 pt-3 border-t border-slate-200 w-full text-[11px] text-slate-500 space-y-1">
          <div className="flex justify-between">
            <span>Card Width:</span>
            <strong className="text-slate-700">{cardWidthMm} mm</strong>
          </div>
          <div className="flex justify-between">
            <span>Card Height:</span>
            <strong className="text-slate-700">{cardHeightMm} mm</strong>
          </div>
          <div className="flex justify-between">
            <span>Resolution:</span>
            <strong className="text-slate-700">300 DPI Vector Output</strong>
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

  const categories = ['All', 'Student', 'School', 'Academic', 'Contact'];
  const filteredPlaceholders =
    activePlaceholderCategory === 'All'
      ? PLACEHOLDERS
      : PLACEHOLDERS.filter((p) => p.category === activePlaceholderCategory);

  return (
    <div className="space-y-4 text-xs">
      {/* Header with Type & Actions */}
      <div className="flex items-center justify-between pb-2 border-b border-slate-100">
        <div className="flex items-center gap-1.5">
          <span className="font-semibold text-slate-900 capitalize text-sm">
            {selectedElement.type === 'rect'
              ? 'Shape / Banner'
              : selectedElement.type === 'qrcode'
              ? 'QR Code'
              : selectedElement.type === 'barcode'
              ? 'Barcode'
              : selectedElement.type === 'signature'
              ? 'Principal Signature'
              : selectedElement.type === 'image'
              ? selectedElement.label || 'Image Box'
              : 'Text Element'}
          </span>
          <span className="text-[10px] bg-slate-100 text-slate-500 px-1.5 py-0.5 rounded font-mono">
            {selectedElement.id.slice(-6)}
          </span>
        </div>
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => onDuplicateElement(selectedElement.id)}
            title="Duplicate element"
            className="p-1 rounded text-slate-500 hover:text-indigo-600 hover:bg-slate-100"
          >
            <Copy className="w-3.5 h-3.5" />
          </button>
          <button
            type="button"
            onClick={() => onDeleteElement(selectedElement.id)}
            title="Delete element"
            className="p-1 rounded text-rose-500 hover:text-rose-700 hover:bg-rose-50"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Coordinates & Dimensions (mm) */}
      <div>
        <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[10px] mb-2 flex items-center gap-1">
          <Hash className="w-3 h-3 text-indigo-500" />
          <span>Position & Size (mm)</span>
        </h5>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">X Position</label>
            <input
              type="number"
              step="0.5"
              min="0"
              max={cardWidthMm}
              value={selectedElement.x ?? 0}
              onChange={(e) => handleNumberChange('x', e.target.value)}
              className="w-full px-2 py-1 border border-slate-200 rounded text-xs focus:ring-1 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">Y Position</label>
            <input
              type="number"
              step="0.5"
              min="0"
              max={cardHeightMm}
              value={selectedElement.y ?? 0}
              onChange={(e) => handleNumberChange('y', e.target.value)}
              className="w-full px-2 py-1 border border-slate-200 rounded text-xs focus:ring-1 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">Width (mm)</label>
            <input
              type="number"
              step="0.5"
              min="1"
              max={cardWidthMm}
              value={selectedElement.width ?? 10}
              onChange={(e) => handleNumberChange('width', e.target.value)}
              className="w-full px-2 py-1 border border-slate-200 rounded text-xs focus:ring-1 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">Height (mm)</label>
            <input
              type="number"
              step="0.5"
              min="1"
              max={cardHeightMm}
              value={selectedElement.height ?? 5}
              onChange={(e) => handleNumberChange('height', e.target.value)}
              className="w-full px-2 py-1 border border-slate-200 rounded text-xs focus:ring-1 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
        </div>
      </div>

      {/* RICH TYPOGRAPHY CONTROLS FOR TEXT ELEMENTS */}
      {selectedElement.type === 'text' && (
        <div className="space-y-3 pt-2 border-t border-slate-100">
          <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[10px] flex items-center gap-1">
            <Type className="w-3 h-3 text-indigo-500" />
            <span>Typography & Styling</span>
          </h5>

          {/* Font Family Selection */}
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

          {/* Font Style Toggles (Bold, Italic, Underline, Uppercase) */}
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">Format & Transform</label>
            <div className="grid grid-cols-4 gap-1 bg-slate-100 p-1 rounded-md">
              <button
                type="button"
                onClick={toggleBold}
                title="Bold (Toggle)"
                className={`py-1 rounded flex items-center justify-center font-bold text-xs transition-colors ${
                  isBold ? 'bg-indigo-600 text-white shadow-xs' : 'text-slate-700 hover:bg-slate-200'
                }`}
              >
                <Bold className="w-3.5 h-3.5" />
              </button>

              <button
                type="button"
                onClick={toggleItalic}
                title="Italic (Toggle)"
                className={`py-1 rounded flex items-center justify-center italic text-xs transition-colors ${
                  isItalic ? 'bg-indigo-600 text-white shadow-xs' : 'text-slate-700 hover:bg-slate-200'
                }`}
              >
                <Italic className="w-3.5 h-3.5" />
              </button>

              <button
                type="button"
                onClick={toggleUnderline}
                title="Underline (Toggle)"
                className={`py-1 rounded flex items-center justify-center text-xs transition-colors ${
                  isUnderline ? 'bg-indigo-600 text-white shadow-xs' : 'text-slate-700 hover:bg-slate-200'
                }`}
              >
                <Underline className="w-3.5 h-3.5" />
              </button>

              <button
                type="button"
                onClick={toggleUppercase}
                title="Uppercase / All Caps"
                className={`py-1 rounded flex items-center justify-center font-mono font-bold text-[10px] transition-colors ${
                  isUppercase ? 'bg-indigo-600 text-white shadow-xs' : 'text-slate-700 hover:bg-slate-200'
                }`}
              >
                TT
              </button>
            </div>
          </div>

          {/* Font Weight Dropdown */}
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="text-[10px] text-slate-500 font-medium block mb-1">Weight</label>
              <select
                value={selectedElement.fontWeight || (isBold ? 'bold' : 'normal')}
                onChange={(e) => handleChange('fontWeight', e.target.value)}
                className="w-full px-2 py-1 border border-slate-200 rounded text-xs bg-white focus:ring-1 focus:ring-indigo-500 focus:outline-none"
              >
                <option value="normal">Normal (400)</option>
                <option value="medium">Medium (500)</option>
                <option value="semibold">Semibold (600)</option>
                <option value="bold">Bold (700)</option>
                <option value="800">Extra Bold (800)</option>
              </select>
            </div>

            <div>
              <label className="text-[10px] text-slate-500 font-medium block mb-1">Letter Spacing</label>
              <select
                value={selectedElement.letterSpacing ?? 0}
                onChange={(e) => handleNumberChange('letterSpacing', e.target.value)}
                className="w-full px-2 py-1 border border-slate-200 rounded text-xs bg-white focus:ring-1 focus:ring-indigo-500 focus:outline-none"
              >
                <option value="-0.5">Tight (-0.5px)</option>
                <option value="0">Normal (0)</option>
                <option value="0.5">Wide (+0.5px)</option>
                <option value="1">Wider (+1.0px)</option>
                <option value="2">Expanded (+2.0px)</option>
              </select>
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
            <div className="mt-1.5 flex gap-1">
              {COLOR_SWATCHES.map((color) => (
                <button
                  key={color}
                  type="button"
                  onClick={() => handleChange('color', color)}
                  style={{ backgroundColor: color }}
                  className="w-4 h-4 rounded-full border border-slate-300 hover:scale-110 transition-transform"
                />
              ))}
            </div>
          </div>

          {/* Optional Text Background & Pill Badge Styling */}
          <div className="pt-2 border-t border-slate-100 space-y-2">
            <h6 className="font-semibold text-slate-600 text-[10px]">Text Background / Pill Badge</h6>
            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="text-[10px] text-slate-500 font-medium block mb-1">Fill Color</label>
                <div className="flex items-center gap-1.5">
                  <input
                    type="color"
                    value={selectedElement.backgroundColor || '#ffffff'}
                    onChange={(e) => handleChange('backgroundColor', e.target.value)}
                    className="w-6 h-6 p-0 border border-slate-300 rounded cursor-pointer"
                  />
                  <button
                    type="button"
                    onClick={() => handleChange('backgroundColor', 'transparent')}
                    className="text-[9px] px-1.5 py-0.5 border border-slate-200 rounded bg-slate-50 hover:bg-slate-100"
                  >
                    Clear
                  </button>
                </div>
              </div>

              <div>
                <label className="text-[10px] text-slate-500 font-medium block mb-1">Corner Radius (mm)</label>
                <input
                  type="number"
                  step="0.5"
                  min="0"
                  max="10"
                  value={selectedElement.borderRadius || 0}
                  onChange={(e) => handleNumberChange('borderRadius', e.target.value)}
                  className="w-full px-2 py-1 border border-slate-200 rounded text-xs"
                />
              </div>
            </div>
          </div>

          {/* Content & Dynamic Data Binding */}
          <div className="pt-2 border-t border-slate-100 space-y-2">
            <label className="text-[10px] text-slate-700 font-semibold uppercase tracking-wider block">
              Content / Placeholders
            </label>
            <textarea
              rows={2}
              value={selectedElement.content || ''}
              onChange={(e) => handleChange('content', e.target.value)}
              className="w-full px-2 py-1.5 border border-slate-200 rounded text-xs font-mono focus:ring-1 focus:ring-indigo-500 focus:outline-none resize-none"
              placeholder="Enter text or placeholders like {{student_name}}"
            />

            {/* Categorized placeholder chips */}
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
                    onClick={() => handleChange('content', (selectedElement.content || '') + ' ' + p.key)}
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
                max="10"
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

      {/* Frame Styling for Images & Signature */}
      {(selectedElement.type === 'image' || selectedElement.type === 'signature') && (
        <div className="space-y-3 pt-2 border-t border-slate-100">
          <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[10px]">
            Frame & Border
          </h5>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="text-[10px] text-slate-500 font-medium block mb-1">
                Border Width (px)
              </label>
              <input
                type="number"
                min="0"
                max="4"
                value={selectedElement.borderWidth || 1}
                onChange={(e) => handleNumberChange('borderWidth', e.target.value)}
                className="w-full px-2 py-1 border border-slate-200 rounded text-xs"
              />
            </div>
            <div>
              <label className="text-[10px] text-slate-500 font-medium block mb-1">
                Border Radius (mm)
              </label>
              <input
                type="number"
                min="0"
                max="10"
                value={selectedElement.borderRadius || 2}
                onChange={(e) => handleNumberChange('borderRadius', e.target.value)}
                className="w-full px-2 py-1 border border-slate-200 rounded text-xs"
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
