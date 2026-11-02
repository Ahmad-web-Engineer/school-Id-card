export const INVOICE_STATUS_VARIANTS = {
  DRAFT: 'neutral',
  ISSUED: 'info',
  PARTIALLY_PAID: 'warning',
  PAID: 'success',
  CANCELLED: 'danger',
  OVERDUE: 'danger',
};

export const SETTLEMENT_STATUS_VARIANTS = {
  PENDING: 'warning',
  APPROVED: 'info',
  PARTIALLY_PAID: 'warning',
  SETTLED: 'success',
  DISPUTED: 'danger',
};

export const PAYMENT_STATUS_VARIANTS = {
  PENDING: 'warning',
  COMPLETED: 'success',
  FAILED: 'danger',
  REFUNDED: 'neutral',
};

export const PAYMENT_METHODS = [
  { value: 'BANK_TRANSFER', label: 'Direct Bank Transfer / NEFT / RTGS' },
  { value: 'UPI', label: 'UPI / QR Code' },
  { value: 'CHEQUE', label: 'Cheque / Demand Draft' },
  { value: 'CARD', label: 'Debit / Credit Card' },
  { value: 'CASH', label: 'Cash / Manual Receipt' },
  { value: 'WALLET', label: 'Corporate Wallet' },
];

export const formatCurrency = (amount) => {
  if (amount == null) return '₹0.00';
  const num = typeof amount === 'number' ? amount : parseFloat(amount);
  return '₹' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};
