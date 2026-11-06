import React, { useState, useEffect } from 'react';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import { formatCurrency } from '../../features/finance/financeConstants';
import api from '../../services/api';

export default function AdminReportsPage() {
  const [activeTab, setActiveTab] = useState('operational');
  const [opReport, setOpReport] = useState(null);
  const [finReport, setFinReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchReports = async () => {
    try {
      setLoading(true);
      setError(null);
      const [opRes, finRes] = await Promise.all([
        api.get('/admin/reports/operational'),
        api.get('/admin/reports/financial'),
      ]);
      setOpReport(opRes?.data !== undefined ? opRes.data : opRes);
      setFinReport(finRes?.data !== undefined ? finRes.data : finRes);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load executive reports');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReports();
  }, []);

  if (loading && !opReport) {
    return (
      <div className="flex justify-center items-center py-24">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error && !opReport) {
    return <ErrorState message={error} onRetry={fetchReports} />;
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Executive & Operational Reports</h1>
        <p className="mt-1 text-sm text-gray-500">
          Global operational efficiency, quality control pass rates, and platform financial health.
        </p>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab('operational')}
            className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
              activeTab === 'operational'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Operational Analytics
          </button>
          <button
            onClick={() => setActiveTab('financial')}
            className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
              activeTab === 'financial'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Financial Performance
          </button>
        </nav>
      </div>

      {/* Operational Analytics Tab */}
      {activeTab === 'operational' && opReport && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            <Card className="p-5">
              <div className="text-sm font-medium text-gray-500">Total Orders</div>
              <div className="mt-2 text-3xl font-bold text-gray-900">{opReport.totalOrders}</div>
              <div className="mt-1 text-xs text-gray-400">Total orders submitted</div>
            </Card>

            <Card className="p-5">
              <div className="text-sm font-medium text-gray-500">Cards Produced</div>
              <div className="mt-2 text-3xl font-bold text-indigo-600">{opReport.totalCardsPrinted?.toLocaleString()}</div>
              <div className="mt-1 text-xs text-indigo-500 font-medium">{opReport.totalCardsDelivered?.toLocaleString()} cards delivered</div>
            </Card>

            <Card className="p-5 border-l-4 border-emerald-500">
              <div className="text-sm font-medium text-gray-500">Quality Pass Rate</div>
              <div className="mt-2 text-3xl font-bold text-emerald-600">{opReport.averageQualityPassRate}%</div>
              <div className="mt-1 text-xs text-emerald-700">QC acceptance index</div>
            </Card>

            <Card className="p-5">
              <div className="text-sm font-medium text-gray-500">Active Ecosystem</div>
              <div className="mt-2 text-3xl font-bold text-gray-900">{opReport.activeSchools}</div>
              <div className="mt-1 text-xs text-gray-500">{opReport.activeAgencies} active print agencies</div>
            </Card>
          </div>

          {/* Orders by Status Breakdown */}
          <Card className="p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Orders by Pipeline Status</h2>
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
              {Object.entries(opReport.ordersByStatus || {}).map(([st, count]) => (
                <div key={st} className="p-4 bg-slate-50 border border-slate-200 rounded-lg">
                  <div className="text-xs font-semibold text-slate-500 uppercase">{st.replace('_', ' ')}</div>
                  <div className="text-2xl font-bold text-slate-900 mt-1">{count}</div>
                </div>
              ))}
            </div>
          </Card>
        </div>
      )}

      {/* Financial Analytics Tab */}
      {activeTab === 'financial' && finReport && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            <Card className="p-5">
              <div className="text-sm font-medium text-gray-500">Gross Revenue</div>
              <div className="mt-2 text-2xl font-bold text-gray-900">
                {formatCurrency(finReport.grossRevenue)}
              </div>
              <div className="mt-1 text-xs text-emerald-600 font-medium">
                {formatCurrency(finReport.collectedRevenue)} collected
              </div>
            </Card>

            <Card className="p-5">
              <div className="text-sm font-medium text-gray-500">Direct Agency Costs</div>
              <div className="mt-2 text-2xl font-bold text-gray-900">
                {formatCurrency(finReport.agencyDirectCosts)}
              </div>
              <div className="mt-1 text-xs text-gray-500">
                {formatCurrency(finReport.agencySettledPayouts)} settled
              </div>
            </Card>

            <Card className="p-5 border-l-4 border-emerald-500">
              <div className="text-sm font-medium text-gray-500">Gross Contribution</div>
              <div className="mt-2 text-2xl font-bold text-emerald-600">
                {formatCurrency(finReport.grossContribution)}
              </div>
              <div className="mt-1 text-xs text-emerald-700 font-medium">
                {finReport.grossMarginPercentage}% margin rate
              </div>
            </Card>

            <Card className="p-5 border-l-4 border-amber-500">
              <div className="text-sm font-medium text-gray-500">Receivables vs Payables</div>
              <div className="mt-2 text-2xl font-bold text-amber-600">
                {formatCurrency(finReport.outstandingReceivables)}
              </div>
              <div className="mt-1 text-xs text-amber-700">
                {formatCurrency(finReport.agencyPendingPayables)} pending agency payables
              </div>
            </Card>
          </div>
        </div>
      )}
    </div>
  );
}
