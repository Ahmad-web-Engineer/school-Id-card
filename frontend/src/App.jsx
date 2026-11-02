import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import AuthLayout from './components/layouts/AuthLayout';
import AdminLayout from './components/layouts/AdminLayout';
import SchoolLayout from './components/layouts/SchoolLayout';
import AgencyLayout from './components/layouts/AgencyLayout';
import ProtectedRoute from './components/common/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import DesignSystemShowcasePage from './pages/DesignSystemShowcasePage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import SchoolsPage from './pages/admin/SchoolsPage';
import AgenciesPage from './pages/admin/AgenciesPage';
import AdminSchoolStudentsPage from './pages/admin/AdminSchoolStudentsPage';
import SchoolDashboardPage from './pages/school/SchoolDashboardPage';
import SchoolProfilePage from './pages/school/SchoolProfilePage';
import StudentsPage from './pages/school/StudentsPage';
import SchoolTemplatesPage from './pages/school/SchoolTemplatesPage';
import OrdersPage from './pages/school/OrdersPage';
import OrderDetailPage from './pages/school/OrderDetailPage';
import TemplatesPage from './pages/admin/TemplatesPage';
import AdminOrdersPage from './pages/admin/AdminOrdersPage';
import AdminOrderDetailPage from './pages/admin/AdminOrderDetailPage';
import AdminAssignmentsPage from './pages/admin/AdminAssignmentsPage';
import AdminProductionPage from './pages/admin/AdminProductionPage';
import AdminQualityControlPage from './pages/admin/AdminQualityControlPage';
import AdminDeliveryPage from './pages/admin/AdminDeliveryPage';
import AgencyJobsPage from './pages/agency/AgencyJobsPage';
import AgencyJobDetailPage from './pages/agency/AgencyJobDetailPage';
import AgencyDashboardPage from './pages/agency/AgencyDashboardPage';
import AgencyProfilePage from './pages/agency/AgencyProfilePage';
import AgencyProductionPage from './pages/agency/AgencyProductionPage';
import AgencyQualityPage from './pages/agency/AgencyQualityPage';
import AgencyDispatchPage from './pages/agency/AgencyDispatchPage';
import AdminFinancePage from './pages/admin/AdminFinancePage';
import SchoolFinancePage from './pages/school/SchoolFinancePage';
import AgencySettlementsPage from './pages/agency/AgencySettlementsPage';
import AdminAuditLogsPage from './pages/admin/AdminAuditLogsPage';
import AdminReportsPage from './pages/admin/AdminReportsPage';
import SchoolReportsPage from './pages/school/SchoolReportsPage';
import AgencyPerformancePage from './pages/agency/AgencyPerformancePage';
import { useAuthStore } from './store/useAuthStore';

function RootRedirect() {
  const { isAuthenticated, user } = useAuthStore();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (user.role === 'SUPER_ADMIN') {
    return <Navigate to="/admin" replace />;
  } else if (user.role === 'SCHOOL') {
    return <Navigate to="/school" replace />;
  } else if (user.role === 'PRINTING_AGENCY') {
    return <Navigate to="/agency" replace />;
  }

  return <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      {/* Root redirect to portal or login */}
      <Route path="/" element={<RootRedirect />} />

      {/* Component Library & Design Tokens Verification */}
      <Route
        path="/design-system"
        element={
          <div className="min-h-full bg-slate-50 p-4 sm:p-8 max-w-7xl mx-auto">
            <DesignSystemShowcasePage />
          </div>
        }
      />

      {/* Auth Shell */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      {/* Super Admin Portal (Guarded by SUPER_ADMIN role) */}
      <Route element={<ProtectedRoute allowedRoles={['SUPER_ADMIN']} />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<AdminDashboardPage />} />
          <Route path="schools" element={<SchoolsPage />} />
          <Route path="schools/:schoolId/students" element={<AdminSchoolStudentsPage />} />
          <Route path="agencies" element={<AgenciesPage />} />
          <Route path="students" element={<AdminSchoolStudentsPage />} />
          <Route path="templates" element={<TemplatesPage />} />
          <Route path="orders" element={<AdminOrdersPage />} />
          <Route path="orders/:id" element={<AdminOrderDetailPage />} />
          <Route path="assignments" element={<AdminAssignmentsPage />} />
          <Route path="production" element={<AdminProductionPage />} />
          <Route path="quality-control" element={<AdminQualityControlPage />} />
          <Route path="delivery" element={<AdminDeliveryPage />} />
          <Route path="finance" element={<AdminFinancePage />} />
          <Route path="reports" element={<AdminReportsPage />} />
          <Route path="audit-logs" element={<AdminAuditLogsPage />} />
          <Route path="*" element={<AdminDashboardPage />} />
        </Route>
      </Route>

      {/* School Portal (Guarded by SCHOOL role) */}
      <Route element={<ProtectedRoute allowedRoles={['SCHOOL']} />}>
        <Route path="/school" element={<SchoolLayout />}>
          <Route index element={<SchoolDashboardPage />} />
          <Route path="profile" element={<SchoolProfilePage />} />
          <Route path="students" element={<StudentsPage />} />
          <Route path="templates" element={<SchoolTemplatesPage />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route path="orders/:id" element={<OrderDetailPage />} />
          <Route path="finance" element={<SchoolFinancePage />} />
          <Route path="payments" element={<SchoolFinancePage />} />
          <Route path="invoices" element={<SchoolFinancePage />} />
          <Route path="reports" element={<SchoolReportsPage />} />
          <Route path="new-order" element={<OrdersPage />} />
          <Route path="*" element={<SchoolDashboardPage />} />
        </Route>
      </Route>

      {/* Agency Portal (Guarded by PRINTING_AGENCY role) */}
      <Route element={<ProtectedRoute allowedRoles={['PRINTING_AGENCY']} />}>
        <Route path="/agency" element={<AgencyLayout />}>
          <Route index element={<AgencyDashboardPage />} />
          <Route path="profile" element={<AgencyProfilePage />} />
          <Route path="jobs" element={<AgencyJobsPage />} />
          <Route path="jobs/:id" element={<AgencyJobDetailPage />} />
          <Route path="assigned-jobs" element={<AgencyJobsPage />} />
          <Route path="assigned-jobs/:id" element={<AgencyJobDetailPage />} />
          <Route path="production" element={<AgencyProductionPage />} />
          <Route path="quality-control" element={<AgencyQualityPage />} />
          <Route path="quality" element={<AgencyQualityPage />} />
          <Route path="dispatch" element={<AgencyDispatchPage />} />
          <Route path="settlements" element={<AgencySettlementsPage />} />
          <Route path="finance" element={<AgencySettlementsPage />} />
          <Route path="performance" element={<AgencyPerformancePage />} />
          <Route path="reports" element={<AgencyPerformancePage />} />
          <Route path="*" element={<AgencyDashboardPage />} />
        </Route>
      </Route>

      {/* 404 Catch-all */}
      <Route path="*" element={<RootRedirect />} />
    </Routes>
  );
}
