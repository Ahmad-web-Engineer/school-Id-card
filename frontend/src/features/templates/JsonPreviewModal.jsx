import React, { useState } from 'react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import { Copy, Check, Code } from 'lucide-react';

export default function JsonPreviewModal({ isOpen, onClose, frontElements, backElements, widthMm, heightMm, bleedMarginMm }) {
  const [copied, setCopied] = useState(false);

  const payload = {
    cardWidthMm: widthMm,
    cardHeightMm: heightMm,
    bleedMarginMm: bleedMarginMm,
    frontDesignJson: JSON.stringify({ elements: frontElements }, null, 2),
    backDesignJson: JSON.stringify({ elements: backElements }, null, 2),
  };

  const formattedJson = JSON.stringify(payload, null, 2);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(formattedJson);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Template Design JSON Inspector"
      maxWidth="max-w-2xl"
      footer={
        <div className="flex items-center justify-between w-full">
          <Button variant="outline" size="sm" onClick={handleCopy} className="flex items-center gap-1.5">
            {copied ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
            <span>{copied ? 'Copied to Clipboard!' : 'Copy JSON'}</span>
          </Button>
          <Button variant="primary" size="sm" onClick={onClose}>
            Done
          </Button>
        </div>
      }
    >
      <div className="space-y-3">
        <p className="text-xs text-slate-500">
          This is the exact JSON structure serialized according to Section 11 of the design spec
          for database persistence and PDFBox print-engine rendering.
        </p>
        <div className="relative">
          <pre className="bg-slate-900 text-slate-100 p-4 rounded-lg text-[11px] font-mono overflow-x-auto max-h-[380px] leading-relaxed">
            {formattedJson}
          </pre>
        </div>
      </div>
    </Modal>
  );
}
