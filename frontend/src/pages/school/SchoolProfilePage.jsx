import React from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import { School, Mail, Phone, MapPin, ShieldCheck } from 'lucide-react';

export default function SchoolProfilePage() {
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['school-own-profile'],
    queryFn: () => api.get('/school/profile'),
  });

  if (isLoading) {
    return (
      <div className="py-12 flex justify-center">
        <LoadingSpinner size="lg" className="text-indigo-600" />
      </div>
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Could not load school profile"
        message={error.message || 'Error communicating with school profile service.'}
        errorCode={error.errorCode}
        onRetry={refetch}
      />
    );
  }

  const school = data?.data;

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Institution Profile</h1>
          <p className="text-xs text-slate-500 mt-1">
            Registered institution identity and official card delivery addresses.
          </p>
        </div>
        <Badge variant="success" size="md">
          {school?.status || 'ACTIVE'}
        </Badge>
      </div>

      <Card
        title={school?.schoolName}
        subtitle={`School Code: ${school?.schoolCode}`}
        actions={<Badge variant="brand">Verified School</Badge>}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <Mail className="h-4 w-4 text-slate-400 mt-0.5" />
              <div>
                <p className="text-xs text-slate-500 font-medium">Official Contact Email</p>
                <p className="text-sm font-semibold text-slate-900">{school?.email || '—'}</p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Phone className="h-4 w-4 text-slate-400 mt-0.5" />
              <div>
                <p className="text-xs text-slate-500 font-medium">Telephone / Mobile</p>
                <p className="text-sm font-semibold text-slate-900">{school?.mobile || '—'}</p>
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <MapPin className="h-4 w-4 text-slate-400 mt-0.5" />
              <div>
                <p className="text-xs text-slate-500 font-medium">Delivery Address</p>
                <p className="text-sm font-semibold text-slate-900">{school?.address || '—'}</p>
                <p className="text-xs text-slate-600 mt-0.5">
                  {[school?.city, school?.state, school?.pincode].filter(Boolean).join(', ')}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <ShieldCheck className="h-4 w-4 text-indigo-500 mt-0.5" />
              <div>
                <p className="text-xs text-slate-500 font-medium">Platform Brokerage Protection</p>
                <p className="text-xs text-slate-600 mt-0.5">
                  All communication, billing, and logistics are mediated exclusively through Platform Administration.
                </p>
              </div>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
