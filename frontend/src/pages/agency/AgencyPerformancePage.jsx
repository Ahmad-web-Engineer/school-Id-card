import React, { useState, useEffect } from 'react';
import Card from '../../components/common/Card';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import { formatCurrency } from '../../features/finance/financeConstants';
import api from '../../services/api';

export default function AgencyPerformancePage() {
  const [perf, setPerf] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchPerformance = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.get('/agency/reports/performance');
      setPerf(res?.data !== undefined ? res.data : res);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load agency performance analytics');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPerformance();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center items-center py-24">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error) {
    return <ErrorState message={error} onRetry={fetchPerformance} />;
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Printing Throughput & Quality Metrics</h1>
        <p className="mt-1 text-sm text-gray-500">
          KPI metrics on assigned production volume, dispatch milestones, defect inspection rates, and payout status.
        </p>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <Card className="p-5">
          <div className="text-sm font-medium text-gray-500">Jobs Assigned</div>
          <div className="mt-2 text-3xl font-bold text-gray-900">{perf?.totalJobsAssigned}</div>
          <div className="mt-1 text-xs text-gray-400">{perf?.totalCardsAssigned} total cards assigned</div>
        </Card>

        <Card className="p-5">
          <div className="text-sm font-medium text-gray-500">Dispatched Output</div>
          <div className="mt-2 text-3xl font-bold text-indigo-600">{perf?.totalCardsDispatched}</div>
          <div className="mt-1 text-xs text-indigo-500 font-medium">{perf?.totalCardsProduced} printed units</div>
        </Card>

        <Card className="p-5 border-l-4 border-emerald-500">
          <div className="text-sm font-medium text-gray-500">Quality Pass Index</div>
          <div className="mt-2 text-3xl font-bold text-emerald-600">{perf?.qcPassPercentage}%</div>
          <div className="mt-1 text-xs text-emerald-700">
            {perf?.qcPassedQuantity} passed / {perf?.qcFailedQuantity} rejected
          </div>
        </Card>

        <Card className="p-5 border-l-4 border-amber-500">
          <div className="text-sm font-medium text-gray-500">Pending Payout</div>
          <div className="mt-2 text-3xl font-bold text-amber-600">
            {formatCurrency(perf?.pendingPayout)}
          </div>
          <div className="mt-1 text-xs text-amber-700">
            Total earned: {formatCurrency(perf?.totalEarnings)} | Settled: {formatCurrency(perf?.settledEarnings)}
          </div>
        </Card>
      </div>
    </div>
  );
}
