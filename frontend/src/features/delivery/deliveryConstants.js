export const DISPATCH_STATUSES = {
  IN_TRANSIT: { label: 'In Transit', variant: 'primary' },
  DELIVERED_TO_HUB: { label: 'Delivered to Hub', variant: 'warning' },
  DELIVERED_TO_SCHOOL: { label: 'Delivered to School', variant: 'success' },
  RETURNED: { label: 'Returned', variant: 'danger' },
  CANCELLED: { label: 'Cancelled', variant: 'neutral' },
};

export const DELIVERY_STATUSES = {
  CONFIRMED: { label: 'Confirmed Receipt', variant: 'success' },
  DISPUTED: { label: 'Disputed', variant: 'danger' },
  CLOSED: { label: 'Delivery Closed', variant: 'neutral' },
};

export const DISPATCH_TYPES = {
  AGENCY_TO_SCHOOL: { label: 'Direct to School (Model A)', variant: 'primary' },
  AGENCY_TO_HUB: { label: 'To Sorting Hub (Model B)', variant: 'warning' },
  HUB_TO_SCHOOL: { label: 'Hub to School (Model B)', variant: 'secondary' },
};

export const POPULAR_COURIERS = [
  'BlueDart Express',
  'Delhivery Logistics',
  'DTDC Express',
  'Speed Post (India Post)',
  'Trackon Couriers',
  'Professional Couriers',
  'Self / Local Handover',
  'Other',
];
