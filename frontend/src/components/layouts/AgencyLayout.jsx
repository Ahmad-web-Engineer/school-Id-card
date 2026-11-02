import React from 'react';
import { Outlet } from 'react-router-dom';
import { LayoutDashboard, Building2, Briefcase, Printer, CheckCircle, Truck, DollarSign } from 'lucide-react';
import PortalLayout from './PortalLayout';

const agencyNavItems = [
  { label: 'Dashboard', path: '/agency', icon: LayoutDashboard },
  { label: 'Agency Profile', path: '/agency/profile', icon: Building2 },
  { label: 'Assigned Jobs', path: '/agency/assigned-jobs', icon: Briefcase },
  { label: 'Production', path: '/agency/production', icon: Printer },
  { label: 'Quality Control', path: '/agency/quality-control', icon: CheckCircle },
  { label: 'Dispatch', path: '/agency/dispatch', icon: Truck },
  { label: 'Settlements', path: '/agency/settlements', icon: DollarSign },
];

export default function AgencyLayout() {
  return (
    <PortalLayout role="PRINTING_AGENCY" navItems={agencyNavItems}>
      <Outlet />
    </PortalLayout>
  );
}
