import React from 'react';
import { Outlet } from 'react-router-dom';
import { LayoutDashboard, School, Building2, Users, Palette, ShoppingBag, Briefcase, Printer, CheckCircle, Truck, DollarSign, BarChart3, History } from 'lucide-react';
import PortalLayout from './PortalLayout';

const adminNavItems = [
  { label: 'Dashboard', path: '/admin', icon: LayoutDashboard },
  { label: 'Schools', path: '/admin/schools', icon: School },
  { label: 'Agencies', path: '/admin/agencies', icon: Building2 },
  { label: 'Students', path: '/admin/students', icon: Users },
  { label: 'Templates', path: '/admin/templates', icon: Palette },
  { label: 'Orders', path: '/admin/orders', icon: ShoppingBag },
  { label: 'Assignments', path: '/admin/assignments', icon: Briefcase },
  { label: 'Production', path: '/admin/production', icon: Printer },
  { label: 'Quality Control', path: '/admin/quality-control', icon: CheckCircle },
  { label: 'Delivery', path: '/admin/delivery', icon: Truck },
  { label: 'Finance', path: '/admin/finance', icon: DollarSign },
  { label: 'Reports', path: '/admin/reports', icon: BarChart3 },
  { label: 'Audit Logs', path: '/admin/audit-logs', icon: History },
];

export default function AdminLayout() {
  return (
    <PortalLayout role="SUPER_ADMIN" navItems={adminNavItems}>
      <Outlet />
    </PortalLayout>
  );
}
