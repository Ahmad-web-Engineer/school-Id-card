import React from 'react';
import { Trash2, Copy, AlignLeft, AlignCenter, AlignRight, Sliders, Hash } from 'lucide-react';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import { PLACEHOLDERS } from './templatesConstants';

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
  if (!selectedElement) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-center p-6 bg-slate-50/50 rounded-lg border border-dashed border-slate-200">
        <Sliders className="w-8 h-8 text-slate-300 mb-2 stroke-[1.5]" />
        <h4 className="text-xs font-semibold text-slate-700">No Element Selected</h4>
        <p className="text-[11px] text-slate-400 mt-1 max-w-[200px]">
          Click any element on the card canvas to inspect and adjust its mm coordinates, typography, and styling.
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
            <strong className="text-slate-700">300 DPI Vector</strong>
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

  return (
    <div className="space-y-4 text-xs">
      {/* Header with Type & Delete / Duplicate */}
      <div className="flex items-center justify-between pb-2 border-b border-slate-100">
        <div className="flex items-center gap-1.5">
          <span className="font-semibold text-slate-900 capitalize text-sm">
            {selectedElement.type === 'rect'
              ? 'Shape / Banner'
              : selectedElement.type === 'qrcode'
              ? 'QR Code'
              : selectedElement.type === 'barcode'
              ? 'Barcode'
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

      {/* Coordinate & Dimensions Grid (mm) */}
      <div>
        <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[10px] mb-2 flex items-center gap-1">
          <Hash className="w-3 h-3 text-indigo-500" />
          <span>Coordinates & Dimensions (mm)</span>
        </h5>
        <div className="grid grid-cols-2 gap-2">
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">X Pos (mm)</label>
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
            <label className="text-[10px] text-slate-500 font-medium block mb-1">Y Pos (mm)</label>
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

      {/* Typography Controls for Text Elements */}
      {selectedElement.type === 'text' && (
        <div className="space-y-3 pt-2 border-t border-slate-100">
          <h5 className="font-semibold text-slate-700 uppercase tracking-wider text-[10px]">
            Typography & Content
          </h5>

          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">
              Text Template / Placeholder
            </label>
            <textarea
              rows={2}
              value={selectedElement.content || ''}
              onChange={(e) => handleChange('content', e.target.value)}
              className="w-full px-2 py-1.5 border border-slate-200 rounded text-xs font-mono focus:ring-1 focus:ring-indigo-500 focus:outline-none resize-none"
              placeholder="Enter text or placeholders like {{student_name}}"
            />
            {/* Quick placeholder insert pills */}
            <div className="mt-1 flex flex-wrap gap-1">
              {['{{student_name}}', '{{admission_number}}', '{{class}}', '{{section}}'].map((tag) => (
                <button
                  key={tag}
                  type="button"
                  onClick={() => handleChange('content', (selectedElement.content || '') + ' ' + tag)}
                  className="text-[9px] bg-indigo-50 text-indigo-700 hover:bg-indigo-100 px-1 py-0.5 rounded font-mono"
                >
                  +{tag}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="text-[10px] text-slate-500 font-medium block mb-1">Font Size (pt)</label>
              <input
                type="number"
                step="0.5"
                min="4"
                max="32"
                value={selectedElement.fontSize || 8}
                onChange={(e) => handleNumberChange('fontSize', e.target.value)}
                className="w-full px-2 py-1 border border-slate-200 rounded text-xs focus:ring-1 focus:ring-indigo-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="text-[10px] text-slate-500 font-medium block mb-1">Weight</label>
              <select
                value={selectedElement.fontWeight || 'normal'}
                onChange={(e) => handleChange('fontWeight', e.target.value)}
                className="w-full px-2 py-1 border border-slate-200 rounded text-xs bg-white focus:ring-1 focus:ring-indigo-500 focus:outline-none"
              >
                <option value="normal">Normal</option>
                <option value="medium">Medium</option>
                <option value="semibold">Semibold</option>
                <option value="bold">Bold</option>
              </select>
            </div>
          </div>

          {/* Alignment */}
          <div>
            <label className="text-[10px] text-slate-500 font-medium block mb-1">Alignment</label>
            <div className="flex rounded border border-slate-200 overflow-hidden">
              {[
                { val: 'left', icon: AlignLeft },
                { val: 'center', icon: AlignCenter },
                { val: 'right', icon: AlignRight },
              ].map(({ val, icon: Icon }) => (
                <button
                  key={val}
                  type="button"
                  onClick={() => handleChange('textAlign', val)}
                  className={`flex-1 py-1 flex items-center justify-center transition-colors ${
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
        </div>
      )}

      {/* Styling Controls for Shapes & Banners */}
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
                Border Radius (mm)
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
                Border Width
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
                Border Radius
              </label>
              <input
                type="number"
                min="0"
                max="6"
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
