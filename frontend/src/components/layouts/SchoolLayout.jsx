import React from 'react';
import { Outlet } from 'react-router-dom';
import { LayoutDashboard, School, Users, UserCheck, Palette, PlusCircle, ShoppingBag, CreditCard, FileText, BarChart3 } from 'lucide-react';
import PortalLayout from './PortalLayout';

const schoolNavItems = [
  { label: 'Dashboard', path: '/school', icon: LayoutDashboard },
  { label: 'School Profile & Logo', path: '/school/profile', icon: School },
  { label: 'Students', path: '/school/students', icon: Users },
  { label: 'Teachers & Staff', path: '/school/teachers', icon: UserCheck },
  { label: 'Card Templates', path: '/school/templates', icon: Palette },
  { label: 'New Order', path: '/school/new-order', icon: PlusCircle },
  { label: 'Order History', path: '/school/orders', icon: ShoppingBag },
  { label: 'Payments', path: '/school/payments', icon: CreditCard },
  { label: 'Invoices', path: '/school/invoices', icon: FileText },
  { label: 'Reports', path: '/school/reports', icon: BarChart3 },
];

export default function SchoolLayout() {
  return (
    <PortalLayout role="SCHOOL" navItems={schoolNavItems}>
      <Outlet />
    </PortalLayout>
  );
}
