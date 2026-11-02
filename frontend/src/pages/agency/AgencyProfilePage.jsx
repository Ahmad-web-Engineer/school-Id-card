import React from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import { Building2, User, Mail, Phone, Gauge, Sparkles } from 'lucide-react';

export default function AgencyProfilePage() {
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['agency-own-profile'],
    queryFn: () => api.get('/agency/profile'),
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
        title="Could not load agency profile"
        message={error.message || 'Error communicating with agency profile service.'}
        errorCode={error.errorCode}
        onRetry={refetch}
      />
    );
  }

  const agency = data?.data;

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Printing Agency Profile</h1>
          <p className="text-xs text-slate-500 mt-1">
            Registered printing workshop capacity, production capabilities, and contact points.
          </p>
        </div>
        <Badge variant="warning" size="md">
          {agency?.status || 'ACTIVE'}
        </Badge>
      </div>

      <Card
        title={agency?.agencyName}
        subtitle="Certified ID Card Production Partner"
        actions={<Badge variant="neutral">Fulfillment Hub</Badge>}
      >
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <User className="h-4 w-4 text-slate-400 mt-0.5" />
              <div>
                <p className="text-xs text-slate-500 font-medium">Contact Person</p>
                <p className="text-sm font-semibold text-slate-900">{agency?.contactPerson || '—'}</p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Mail className="h-4 w-4 text-slate-400 mt-0.5" />
              <div>
                <p className="text-xs text-slate-500 font-medium">Business Email</p>
                <p className="text-sm font-semibold text-slate-900">{agency?.email || '—'}</p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Phone className="h-4 w-4 text-slate-400 mt-0.5" />
              <div>
                <p className="text-xs text-slate-500 font-medium">Telephone / Dispatch Phone</p>
                <p className="text-sm font-semibold text-slate-900">{agency?.mobile || '—'}</p>
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <Gauge className="h-4 w-4 text-indigo-500 mt-0.5" />
              <div>
                <p className="text-xs text-slate-500 font-medium">Daily Production Capacity</p>
                <p className="text-sm font-bold text-slate-900">
                  {Number(agency?.productionCapacity || 0).toLocaleString()} cards / day
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Sparkles className="h-4 w-4 text-amber-500 mt-0.5" />
              <div>
                <p className="text-xs text-slate-500 font-medium">Certified Print Capabilities</p>
                <p className="text-sm font-semibold text-slate-900">{agency?.capabilities || 'Thermal PVC'}</p>
              </div>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
