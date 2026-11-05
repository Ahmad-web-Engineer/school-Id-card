import React, { useState } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import { PRESET_TEMPLATES } from './templatesConstants';
import { Sparkles, UploadCloud, FileCode2, Check, AlertCircle, Palette } from 'lucide-react';

export default function ImportDesignModal({
  isOpen,
  onClose,
  onApplyPreset,
  onImportCustomJson,
}) {
  const [activeTab, setActiveTab] = useState('presets'); // 'presets' | 'json'
  const [jsonText, setJsonText] = useState('');
  const [jsonError, setJsonError] = useState(null);
  const [selectedPresetId, setSelectedPresetId] = useState('modern_blue');

  const handleApplySelectedPreset = (preset) => {
    onApplyPreset(preset);
    onClose();
  };

  const handleJsonUpload = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const text = event.target?.result;
        setJsonText(text);
        setJsonError(null);
      } catch (err) {
        setJsonError('Failed to read file: ' + err.message);
      }
    };
    reader.readAsText(file);
  };

  const handleApplyCustomJson = () => {
    setJsonError(null);
    if (!jsonText.trim()) {
      setJsonError('Please paste or upload valid design JSON.');
      return;
    }
    try {
      const parsed = JSON.parse(jsonText);
      onImportCustomJson(parsed);
      onClose();
    } catch (err) {
      setJsonError('Invalid JSON format: ' + err.message);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Import ID Card Design & Presets"
      size="xl"
    >
      <div className="space-y-5">
        {/* Tab Switcher */}
        <div className="flex border-b border-slate-200">
          <button
            type="button"
            onClick={() => setActiveTab('presets')}
            className={`flex items-center gap-2 pb-3 px-4 text-sm font-semibold border-b-2 transition-all ${
              activeTab === 'presets'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            <Sparkles className="w-4 h-4 text-amber-500" />
            <span>Professional Presets ({PRESET_TEMPLATES.length})</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('json')}
            className={`flex items-center gap-2 pb-3 px-4 text-sm font-semibold border-b-2 transition-all ${
              activeTab === 'json'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            <FileCode2 className="w-4 h-4 text-indigo-500" />
            <span>Upload or Paste JSON</span>
          </button>
        </div>

        {/* Tab 1: Professional Presets */}
        {activeTab === 'presets' && (
          <div className="space-y-4">
            <p className="text-xs text-slate-500">
              Select one of our meticulously crafted high-contrast ID card designs. Each preset includes complete double-sided layouts, matching typography, and scannable QR verification configurations.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-h-[460px] overflow-y-auto pr-1">
              {PRESET_TEMPLATES.map((preset) => {
                const isSelected = selectedPresetId === preset.id;
                const headerColor =
                  preset.front?.elements?.find((el) => el.type === 'rect')?.backgroundColor ||
                  '#1e3a8a';

                return (
                  <div
                    key={preset.id}
                    onClick={() => setSelectedPresetId(preset.id)}
                    className={`relative p-4 rounded-xl border-2 cursor-pointer transition-all flex flex-col justify-between ${
                      isSelected
                        ? 'border-indigo-600 bg-indigo-50/40 shadow-md ring-2 ring-indigo-500/20'
                        : 'border-slate-200 bg-white hover:border-slate-300 hover:shadow-xs'
                    }`}
                  >
                    <div>
                      {/* Mini Card Preview Bar */}
                      <div
                        className="h-10 rounded-lg mb-3 flex items-center px-3 justify-between shadow-inner"
                        style={{ backgroundColor: headerColor }}
                      >
                        <span className="text-xs font-bold text-white tracking-wide truncate">
                          {preset.name}
                        </span>
                        <div className="w-5 h-5 rounded-full bg-white/20 border border-white/40 flex items-center justify-center text-[10px] text-white font-mono">
                          ID
                        </div>
                      </div>

                      <h4 className="text-sm font-bold text-slate-900 mb-1 flex items-center justify-between">
                        <span>{preset.name}</span>
                        {isSelected && (
                          <span className="flex items-center text-xs font-semibold text-indigo-600 gap-1">
                            <Check className="w-3.5 h-3.5" /> Selected
                          </span>
                        )}
                      </h4>
                      <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed">
                        {preset.description}
                      </p>
                    </div>

                    <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between">
                      <div className="flex items-center gap-1.5">
                        <span
                          className="w-3.5 h-3.5 rounded-full border border-slate-300 shadow-xs shrink-0"
                          style={{ backgroundColor: preset.front?.backgroundColor || '#ffffff' }}
                          title="Card Canvas Base Color"
                        />
                        <span className="text-[11px] text-slate-500">
                          {preset.front?.elements?.length || 0} Front / {preset.back?.elements?.length || 0} Back
                        </span>
                      </div>

                      <Button
                        size="xs"
                        variant={isSelected ? 'primary' : 'outline'}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleApplySelectedPreset(preset);
                        }}
                      >
                        Apply Preset
                      </Button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Tab 2: Upload or Paste JSON */}
        {activeTab === 'json' && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <p className="text-xs text-slate-500">
                Paste raw design JSON or upload a exported card template file. Supports full double-sided configurations or element arrays.
              </p>
              <label className="cursor-pointer inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium bg-slate-100 text-slate-700 hover:bg-slate-200 rounded-md border border-slate-300 transition-colors">
                <UploadCloud className="w-3.5 h-3.5 text-slate-600" />
                <span>Upload JSON File</span>
                <input
                  type="file"
                  accept=".json,application/json"
                  onChange={handleJsonUpload}
                  className="hidden"
                />
              </label>
            </div>

            {jsonError && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg flex items-start gap-2 text-xs text-rose-800">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{jsonError}</span>
              </div>
            )}

            <div>
              <textarea
                value={jsonText}
                onChange={(e) => {
                  setJsonText(e.target.value);
                  setJsonError(null);
                }}
                rows={12}
                placeholder={`{\n  "backgroundColor": "#ffffff",\n  "elements": [\n    {\n      "id": "el-1",\n      "type": "text",\n      "content": "{{student_name}}",\n      "x": 20,\n      "y": 15,\n      "fontSize": 12\n    }\n  ]\n}`}
                className="w-full font-mono text-xs bg-slate-900 text-emerald-400 p-3 rounded-lg border border-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 placeholder-slate-600 leading-relaxed"
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-200">
              <Button variant="outline" size="sm" onClick={onClose}>
                Cancel
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleApplyCustomJson}
                disabled={!jsonText.trim()}
                className="flex items-center gap-1.5"
              >
                <FileCode2 className="w-4 h-4" />
                <span>Import Design JSON</span>
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
