import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';
import ErrorState from './ErrorState';

export default function ProtectedRoute({ allowedRoles = [] }) {
  const { isAuthenticated, user } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles.length > 0 && (!user || !allowedRoles.includes(user.role))) {
    return (
      <div className="p-8 max-w-xl mx-auto">
        <ErrorState
          title="Access Denied"
          message={`Your role (${user?.role || 'Guest'}) does not have permission to view this section.`}
          errorCode="ACCESS_DENIED"
        />
      </div>
    );
  }

  return <Outlet />;
}
