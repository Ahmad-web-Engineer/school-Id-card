import React, { useState, useEffect } from 'react';
import Card from '../../components/common/Card';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import { formatCurrency } from '../../features/finance/financeConstants';
import api from '../../services/api';

export default function SchoolReportsPage() {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchReport = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.get('/school/reports/summary');
      setReport(res?.data !== undefined ? res.data : res);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load school summary report');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center items-center py-24">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error) {
    return <ErrorState message={error} onRetry={fetchReport} />;
  }

  const deliveryRate =
    report?.cardsOrdered > 0
      ? ((report.cardsDelivered / report.cardsOrdered) * 100).toFixed(1)
      : '100.0';

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">School Summary & Enrollment Report</h1>
        <p className="mt-1 text-sm text-gray-500">
          Overview of registered students, card order volume, fulfilled deliveries, and financial balances.
        </p>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <Card className="p-5">
          <div className="text-sm font-medium text-gray-500">Total Enrolled Students</div>
          <div className="mt-2 text-3xl font-bold text-gray-900">{report?.totalStudents}</div>
          <div className="mt-1 text-xs text-gray-400">Roster in database</div>
        </Card>

        <Card className="p-5">
          <div className="text-sm font-medium text-gray-500">Orders Placed</div>
          <div className="mt-2 text-3xl font-bold text-indigo-600">{report?.totalOrders}</div>
          <div className="mt-1 text-xs text-indigo-500">{report?.cardsOrdered} cards ordered</div>
        </Card>

        <Card className="p-5 border-l-4 border-emerald-500">
          <div className="text-sm font-medium text-gray-500">Cards Delivered</div>
          <div className="mt-2 text-3xl font-bold text-emerald-600">{report?.cardsDelivered}</div>
          <div className="mt-1 text-xs text-emerald-700">{deliveryRate}% fulfillment rate</div>
        </Card>

        <Card className="p-5 border-l-4 border-amber-500">
          <div className="text-sm font-medium text-gray-500">Outstanding Balance</div>
          <div className="mt-2 text-3xl font-bold text-amber-600">
            {formatCurrency(report?.outstandingDues)}
          </div>
          <div className="mt-1 text-xs text-amber-700">
            Billed: {formatCurrency(report?.totalBilled)} | Paid: {formatCurrency(report?.totalPaid)}
          </div>
        </Card>
      </div>
    </div>
  );
}
