import React, { useRef, useState, useCallback, useEffect } from 'react';
import { substitutePlaceholders, SAMPLE_STUDENT_DATA } from './templatesConstants';
import { User, QrCode as QrIcon, Barcode as BarcodeIcon, Image as ImageIcon, Sparkles } from 'lucide-react';

export default function CardCanvas({
  widthMm = 85.6,
  heightMm = 54.0,
  bleedMarginMm = 3.0,
  showBleed = true,
  elements = [],
  selectedElementId = null,
  onSelectElement,
  onElementChange,
  interactive = true,
  previewData = null, // If null, can show placeholders or sample data depending on useSampleData
  useSampleData = true,
  scale = 4.8, // px per mm: 85.6mm * 4.8 = 410.88px
  className = '',
}) {
  const containerRef = useRef(null);
  const [dragState, setDragState] = useState(null);

  const canvasWidthPx = widthMm * scale;
  const canvasHeightPx = heightMm * scale;
  const bleedMarginPx = bleedMarginMm * scale;

  const activeStudentData = previewData || (useSampleData ? SAMPLE_STUDENT_DATA : null);

  // Mouse drag handler for repositioning elements
  const handleMouseDown = (e, element) => {
    if (!interactive) return;
    e.stopPropagation();
    if (onSelectElement) {
      onSelectElement(element.id);
    }

    setDragState({
      elementId: element.id,
      startX: e.clientX,
      startY: e.clientY,
      origX: element.x,
      origY: element.y,
    });
  };

  const handleMouseMove = useCallback(
    (e) => {
      if (!dragState || !interactive || !onElementChange) return;

      const deltaXPx = e.clientX - dragState.startX;
      const deltaYPx = e.clientY - dragState.startY;

      const deltaXMm = deltaXPx / scale;
      const deltaYMm = deltaYPx / scale;

      const el = elements.find((item) => item.id === dragState.elementId);
      if (!el) return;

      const newX = Math.max(0, Math.min(widthMm - (el.width || 10), +(dragState.origX + deltaXMm).toFixed(1)));
      const newY = Math.max(0, Math.min(heightMm - (el.height || 5), +(dragState.origY + deltaYMm).toFixed(1)));

      onElementChange(dragState.elementId, { x: newX, y: newY });
    },
    [dragState, interactive, onElementChange, elements, scale, widthMm, heightMm]
  );

  const handleMouseUp = useCallback(() => {
    setDragState(null);
  }, []);

  useEffect(() => {
    if (dragState) {
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
    } else {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    }
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [dragState, handleMouseMove, handleMouseUp]);

  // Click on background deselects
  const handleCanvasClick = (e) => {
    if (e.target === containerRef.current && onSelectElement && interactive) {
      onSelectElement(null);
    }
  };

  return (
    <div className={`relative flex flex-col items-center justify-center p-3 sm:p-5 select-none ${className}`}>
      {/* Outer Card Wrapper with Shadow & Exact Aspect Ratio */}
      <div
        ref={containerRef}
        onClick={handleCanvasClick}
        style={{
          width: `${canvasWidthPx}px`,
          height: `${canvasHeightPx}px`,
        }}
        className="relative bg-white shadow-xl rounded-md overflow-hidden border border-slate-300 transition-all duration-75"
      >
        {/* Bleed Margin Indicator (3mm) */}
        {showBleed && (
          <div
            style={{
              top: `${bleedMarginPx}px`,
              left: `${bleedMarginPx}px`,
              right: `${bleedMarginPx}px`,
              bottom: `${bleedMarginPx}px`,
            }}
            className="absolute border border-dashed border-rose-400/50 pointer-events-none z-30"
          >
            <span className="absolute -top-3 left-1 text-[8px] font-mono text-rose-500 font-semibold px-1 bg-white/90 rounded border border-rose-200">
              {bleedMarginMm}mm safe / bleed line
            </span>
          </div>
        )}

        {/* Card Elements Render Loop */}
        {elements.map((el) => {
          const isSelected = selectedElementId === el.id;
          const leftPx = el.x * scale;
          const topPx = el.y * scale;
          const widthPx = el.width * scale;
          const heightPx = el.height * scale;

          return (
            <div
              key={el.id}
              onClick={(e) => {
                if (!interactive) return;
                e.stopPropagation();
                onSelectElement && onSelectElement(el.id);
              }}
              onMouseDown={(e) => handleMouseDown(e, el)}
              style={{
                left: `${leftPx}px`,
                top: `${topPx}px`,
                width: `${widthPx}px`,
                height: `${heightPx}px`,
                zIndex: isSelected ? 40 : el.type === 'rect' ? 10 : 20,
              }}
              className={`absolute cursor-${interactive ? 'move' : 'default'} transition-shadow ${
                isSelected
                  ? 'ring-2 ring-indigo-600 ring-offset-1 rounded-sm'
                  : interactive
                  ? 'hover:outline hover:outline-1 hover:outline-indigo-300'
                  : ''
              }`}
            >
              {/* Selected coordinate tag badge */}
              {isSelected && interactive && (
                <div className="absolute -top-5 left-0 z-50 bg-indigo-700 text-white text-[9px] font-mono px-1 py-0.2 rounded shadow-sm pointer-events-none whitespace-nowrap">
                  X:{el.x} Y:{el.y} mm
                </div>
              )}

              {/* RENDER ELEMENT BY TYPE */}
              {/* 1. Rectangle / Banner Bar */}
              {el.type === 'rect' && (
                <div
                  style={{
                    backgroundColor: el.backgroundColor || '#1e3a8a',
                    borderColor: el.borderColor || 'transparent',
                    borderWidth: el.borderWidth ? `${el.borderWidth}px` : 0,
                    borderRadius: el.borderRadius ? `${el.borderRadius * 2}px` : 0,
                  }}
                  className="w-full h-full"
                />
              )}

              {/* 2. Text / Placeholder Field */}
              {el.type === 'text' && (
                <div
                  style={{
                    fontFamily: el.fontFamily || 'Inter, sans-serif',
                    fontSize: `${(el.fontSize || 8) * (scale / 4)}px`,
                    fontWeight: el.fontWeight || (el.bold ? 'bold' : 'normal'),
                    fontStyle: el.fontStyle || (el.italic ? 'italic' : 'normal'),
                    textDecoration: el.textDecoration || (el.underline ? 'underline' : 'none'),
                    textTransform: el.textTransform || 'none',
                    letterSpacing: el.letterSpacing ? `${el.letterSpacing * (scale / 4)}px` : 'normal',
                    lineHeight: el.lineHeight || 1.25,
                    color: el.color || '#0f172a',
                    textAlign: el.textAlign || 'left',
                    backgroundColor: el.backgroundColor || 'transparent',
                    borderRadius: el.borderRadius ? `${el.borderRadius * scale}px` : 0,
                    borderColor: el.borderColor || 'transparent',
                    borderWidth: el.borderWidth ? `${el.borderWidth}px` : 0,
                    padding: el.backgroundColor && el.backgroundColor !== 'transparent' ? '2px 4px' : 0,
                  }}
                  className="w-full h-full flex flex-col justify-center overflow-hidden whitespace-pre-wrap break-words"
                >
                  {activeStudentData
                    ? substitutePlaceholders(el.content || '', activeStudentData)
                    : el.content || '{{text}}'}
                </div>
              )}

              {/* 3. Image Box (Student Photo or School Logo) */}
              {el.type === 'image' && (
                <div
                  style={{
                    borderColor: el.borderColor || '#cbd5e1',
                    borderWidth: el.borderWidth ? `${el.borderWidth}px` : 1,
                    borderRadius: el.borderRadius ? `${el.borderRadius * 2}px` : 2,
                  }}
                  className="w-full h-full bg-slate-100 overflow-hidden flex flex-col items-center justify-center relative border shadow-inner"
                >
                  {el.field === 'student_photo' ? (
                    activeStudentData?.photo_url || activeStudentData?.photoUrl ? (
                      <img
                        src={activeStudentData.photo_url || activeStudentData.photoUrl}
                        alt="Student"
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="flex flex-col items-center justify-center p-1 text-slate-400">
                        <User className="w-8 h-8 text-slate-300 stroke-[1.5]" />
                        <span className="text-[7px] text-slate-400 font-medium tracking-tight mt-0.5">
                          Photo Frame
                        </span>
                      </div>
                    )
                  ) : el.field === 'school_logo' ? (
                    activeStudentData?.school_logo_url || activeStudentData?.logoUrl ? (
                      <img
                        src={activeStudentData.school_logo_url || activeStudentData.logoUrl}
                        alt="School Logo"
                        className="w-full h-full object-contain p-0.5"
                      />
                    ) : (
                      <div className="flex flex-col items-center justify-center p-0.5 text-indigo-700 bg-indigo-50 w-full h-full">
                        <Sparkles className="w-5 h-5 text-indigo-600" />
                        <span className="text-[6px] font-bold uppercase tracking-wider text-indigo-900">
                          Logo
                        </span>
                      </div>
                    )
                  ) : (
                    <div className="flex flex-col items-center justify-center text-slate-400">
                      <ImageIcon className="w-6 h-6" />
                      <span className="text-[7px]">Image</span>
                    </div>
                  )}
                </div>
              )}

              {/* 4. QR Code Box */}
              {el.type === 'qrcode' && (
                <div className="w-full h-full bg-white p-1 border border-slate-300 rounded flex flex-col items-center justify-center shadow-sm">
                  {/* High quality simulated SVG QR Matrix */}
                  <svg
                    viewBox="0 0 33 33"
                    className="w-full h-full"
                    shapeRendering="crispEdges"
                    fill="currentColor"
                  >
                    {/* Top-Left Finder */}
                    <path d="M0,0 h7 v7 h-7 z M1,1 v5 h5 v-5 z M2,2 h3 v3 h-3 z" />
                    {/* Top-Right Finder */}
                    <path d="M26,0 h7 v7 h-7 z M27,1 v5 h5 v-5 z M28,2 h3 v3 h-3 z" />
                    {/* Bottom-Left Finder */}
                    <path d="M0,26 h7 v7 h-7 z M1,27 v5 h5 v-5 z M2,28 h3 v3 h-3 z" />
                    {/* Data modules */}
                    <rect x="9" y="1" width="2" height="2" />
                    <rect x="13" y="1" width="3" height="1" />
                    <rect x="18" y="2" width="2" height="3" />
                    <rect x="22" y="1" width="2" height="2" />
                    <rect x="9" y="5" width="4" height="2" />
                    <rect x="15" y="6" width="3" height="3" />
                    <rect x="20" y="5" width="2" height="2" />
                    <rect x="9" y="9" width="3" height="3" />
                    <rect x="14" y="11" width="5" height="2" />
                    <rect x="21" y="9" width="3" height="4" />
                    <rect x="26" y="9" width="4" height="2" />
                    <rect x="2" y="10" width="3" height="2" />
                    <rect x="1" y="14" width="4" height="3" />
                    <rect x="7" y="14" width="3" height="2" />
                    <rect x="12" y="15" width="3" height="4" />
                    <rect x="17" y="15" width="4" height="3" />
                    <rect x="23" y="15" width="2" height="3" />
                    <rect x="27" y="13" width="5" height="3" />
                    <rect x="2" y="19" width="4" height="2" />
                    <rect x="8" y="18" width="2" height="4" />
                    <rect x="12" y="21" width="5" height="2" />
                    <rect x="19" y="20" width="3" height="4" />
                    <rect x="24" y="20" width="3" height="2" />
                    <rect x="29" y="18" width="3" height="3" />
                    <rect x="9" y="25" width="3" height="3" />
                    <rect x="14" y="25" width="4" height="2" />
                    <rect x="20" y="26" width="3" height="3" />
                    <rect x="25" y="24" width="5" height="3" />
                    <rect x="9" y="30" width="5" height="2" />
                    <rect x="16" y="29" width="3" height="3" />
                    <rect x="21" y="30" width="4" height="2" />
                    <rect x="27" y="29" width="3" height="3" />
                  </svg>
                </div>
              )}

              {/* 5. Barcode Box */}
              {el.type === 'barcode' && (
                <div className="w-full h-full bg-white p-1 border border-slate-300 rounded flex flex-col items-center justify-between shadow-sm">
                  {/* Simulated Code128 Vector Bars */}
                  <div className="w-full h-4/5 flex items-stretch justify-between px-1">
                    {[3, 1, 2, 1, 4, 1, 2, 3, 1, 2, 4, 1, 3, 2, 1, 2, 1, 3, 4, 1, 2, 3, 1, 2, 3, 1, 4, 2].map(
                      (weight, idx) => (
                        <div
                          key={idx}
                          style={{
                            width: `${weight * 0.9}px`,
                            backgroundColor: idx % 2 === 0 ? '#000000' : 'transparent',
                          }}
                          className="h-full"
                        />
                      )
                    )}
                  </div>
                  <span className="text-[6.5px] font-mono tracking-widest text-slate-700 font-semibold leading-none">
                    {activeStudentData ? activeStudentData.admission_number : '*ADM-2026-001*'}
                  </span>
                </div>
              )}

              {/* 6. Signature Box */}
              {el.type === 'signature' && (
                <div
                  style={{
                    borderColor: el.borderColor || '#94a3b8',
                    borderWidth: el.borderWidth ? `${el.borderWidth}px` : 1,
                    borderRadius: el.borderRadius ? `${el.borderRadius * 2}px` : 2,
                  }}
                  className="w-full h-full border-dashed bg-slate-50/60 p-1 flex flex-col justify-end items-center relative overflow-hidden"
                >
                  {activeStudentData?.signature_url || activeStudentData?.signatureUrl ? (
                    <img
                      src={activeStudentData.signature_url || activeStudentData.signatureUrl}
                      alt="Signature"
                      className="max-h-[60%] max-w-[90%] object-contain mb-0.5"
                    />
                  ) : null}
                  <div className="w-4/5 border-t border-slate-400 mb-0.5" />
                  <span className="text-[6.5px] font-medium text-slate-500 uppercase tracking-wider text-center leading-none">
                    {el.label || 'Principal Signature'}
                  </span>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Under Canvas Meta Info: Dimensions & Scale Indicator */}
      <div className="mt-2.5 flex items-center justify-between w-full max-w-sm text-[11px] text-slate-500 font-medium">
        <span>
          Dimensions: <strong className="text-slate-700">{widthMm} × {heightMm} mm</strong>
        </span>
        <span>
          Bleed: <strong className="text-slate-700">{bleedMarginMm} mm</strong>
        </span>
        <span className="bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded text-[10px] font-mono">
          CR80 Standard
        </span>
      </div>
    </div>
  );
}
