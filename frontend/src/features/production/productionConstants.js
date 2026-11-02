export const BATCH_STATUSES = {
  PLANNED: { label: 'Planned', color: 'bg-slate-100 text-slate-700 border-slate-300' },
  IN_PROGRESS: { label: 'In Progress', color: 'bg-blue-100 text-blue-700 border-blue-300' },
  PAUSED: { label: 'Paused', color: 'bg-amber-100 text-amber-700 border-amber-300' },
  PRINTING_COMPLETED: { label: 'Printing Completed', color: 'bg-emerald-100 text-emerald-700 border-emerald-300' },
  CANCELLED: { label: 'Cancelled', color: 'bg-rose-100 text-rose-700 border-rose-300' },
};

export const QC_STATUSES = {
  PASSED: { label: 'Passed', color: 'bg-emerald-100 text-emerald-700 border-emerald-300' },
  CONDITIONALLY_PASSED: { label: 'Conditionally Passed', color: 'bg-amber-100 text-amber-700 border-amber-300' },
  FAILED: { label: 'Failed', color: 'bg-rose-100 text-rose-700 border-rose-300' },
};

export const DEFECT_CATEGORIES = [
  { value: 'PRINT_ALIGNMENT', label: 'Print Alignment / Margin Shift' },
  { value: 'COLOR_MISMATCH', label: 'Color Inconsistency / Fading' },
  { value: 'BLEED_ERROR', label: 'Bleed Cut / Edge Clipping' },
  { value: 'DAMAGE', label: 'Physical Scratch / Lamination Damage' },
  { value: 'MISSING_DATA', label: 'Barcode / QR / Text Unreadable' },
  { value: 'OTHER', label: 'Other Manufacturing Defect' },
];

export const REPRINT_STATUSES = {
  PENDING: { label: 'Pending Review', color: 'bg-amber-100 text-amber-700 border-amber-300' },
  APPROVED: { label: 'Reprint Approved', color: 'bg-emerald-100 text-emerald-700 border-emerald-300' },
  REJECTED: { label: 'Reprint Rejected', color: 'bg-rose-100 text-rose-700 border-rose-300' },
};

export const COST_BEARERS = [
  { value: 'AGENCY', label: 'Printing Agency (Defect Responsibility)' },
  { value: 'PLATFORM', label: 'Platform Goodwill / Internal' },
  { value: 'SCHOOL', label: 'School (Client Data Revision)' },
];
