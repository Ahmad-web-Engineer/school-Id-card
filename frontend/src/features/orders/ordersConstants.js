export const ORDER_STATUSES = {
  DRAFT: { label: 'Draft', variant: 'neutral', description: 'Order created, not yet submitted.' },
  SUBMITTED: { label: 'Submitted', variant: 'info', description: 'Submitted for verification.' },
  UNDER_REVIEW: { label: 'Under Review', variant: 'brand', description: 'Under review by printing team.' },
  CORRECTION_REQUIRED: { label: 'Correction Required', variant: 'danger', description: 'Changes requested by admin.' },
  VERIFIED: { label: 'Verified', variant: 'info', description: 'Verified and ready for quotation.' },
  QUOTATION_GENERATED: { label: 'Quotation Ready', variant: 'warning', description: 'Quotation prepared for school approval.' },
  APPROVED: { label: 'Approved', variant: 'success', description: 'Approved and queued for production.' },
  CANCELLED: { label: 'Cancelled', variant: 'neutral', description: 'Order cancelled.' },
};

export const ACCESSORY_OPTIONS = [
  { type: 'LANYARD', label: 'Premium Polyester Lanyard', defaultPrice: 25.00 },
  { type: 'CARD_HOLDER', label: 'Rigid PVC Transparent Card Holder', defaultPrice: 15.00 },
  { type: 'CLIP', label: 'Alligator Clip with Vinyl Strap', defaultPrice: 10.00 },
  { type: 'YOYO', label: 'Retractable Badge Reel / Yo-Yo', defaultPrice: 35.00 },
];
