import React, { useState, useEffect } from 'react';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import { SETTLEMENT_STATUS_VARIANTS, formatCurrency } from '../../features/finance/financeConstants';
import api from '../../services/api';
import { extractArray } from '../../services/responseUtils';

export default function AgencySettlementsPage() {
  const [summary, setSummary] = useState(null);
  const [settlements, setSettlements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [sumRes, setRes] = await Promise.all([
        api.get('/agency/finance/summary'),
        api.get('/agency/finance/settlements'),
      ]);

      setSummary(sumRes?.data?.data || sumRes?.data || null);
      setSettlements(extractArray(setRes));
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load agency settlement statements');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  if (loading && !summary) {
    return (
      <div className="flex justify-center items-center py-24">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error && !summary) {
    return <ErrorState message={error} onRetry={fetchData} />;
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Agency Settlements & Payouts</h1>
        <p className="mt-1 text-sm text-gray-500">
          Review job contract payouts, defect reprint deductions, and cleared bank disbursements.
        </p>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-5">
        <Card className="p-5">
          <div className="text-sm font-medium text-gray-500">Total Agreed</div>
          <div className="mt-2 text-2xl font-bold text-gray-900">
            {formatCurrency(summary?.totalAgreedAmount)}
          </div>
          <div className="mt-1 text-xs text-gray-400">Total gross job contracts</div>
        </Card>

        <Card className="p-5">
          <div className="text-sm font-medium text-gray-500">Net Payable</div>
          <div className="mt-2 text-2xl font-bold text-indigo-600">
            {formatCurrency(summary?.totalPayableAmount)}
          </div>
          <div className="mt-1 text-xs text-gray-400">After defect deductions</div>
        </Card>

        <Card className="p-5">
          <div className="text-sm font-medium text-gray-500">Total Disbursed</div>
          <div className="mt-2 text-2xl font-bold text-emerald-600">
            {formatCurrency(summary?.totalPaidAmount)}
          </div>
          <div className="mt-1 text-xs text-gray-400">Cleared bank transfers</div>
        </Card>

        <Card className="p-5 border-l-4 border-amber-500">
          <div className="text-sm font-medium text-gray-500">Pending Payout</div>
          <div className="mt-2 text-2xl font-bold text-amber-600">
            {formatCurrency(summary?.outstandingPayable)}
          </div>
          <div className="mt-1 text-xs text-gray-400">Awaiting disbursement</div>
        </Card>
      </div>

      {/* Settlements Table */}
      {settlements.length === 0 ? (
        <EmptyState
          title="No Settlements Issued"
          description="Settlement vouchers will be generated as completed print jobs pass quality and delivery milestones."
        />
      ) : (
        <Card padding="none">
          <div className="p-4 border-b border-gray-100 flex justify-between items-center">
            <h2 className="text-lg font-semibold text-gray-900">Settlement Statements</h2>
            <span className="text-xs text-gray-500">{settlements.length} total records</span>
          </div>
          <ResponsiveTable
            columns={[
              {
                header: 'Statement #',
                accessor: (row) => (
                  <span className="font-semibold text-indigo-600">{row.settlementNumber}</span>
                ),
              },
              {
                header: 'Job #',
                accessor: (row) => row.jobNumber,
              },
              {
                header: 'Agreed (₹)',
                accessor: (row) => formatCurrency(row.agreedAmount),
              },
              {
                header: 'Reprint Deductions',
                accessor: (row) => (
                  <span className={row.deductionAmount > 0 ? 'text-red-600 font-medium' : 'text-gray-400'}>
                    {row.deductionAmount > 0 ? `-${formatCurrency(row.deductionAmount)}` : '₹0.00'}
                  </span>
                ),
              },
              {
                header: 'Penalties',
                accessor: (row) => (
                  <span className={row.penaltyAmount > 0 ? 'text-red-600 font-medium' : 'text-gray-400'}>
                    {row.penaltyAmount > 0 ? `-${formatCurrency(row.penaltyAmount)}` : '₹0.00'}
                  </span>
                ),
              },
              {
                header: 'Net Payable',
                accessor: (row) => (
                  <span className="font-semibold text-gray-900">{formatCurrency(row.payableAmount)}</span>
                ),
              },
              {
                header: 'Disbursed',
                accessor: (row) => (
                  <span className="text-emerald-600 font-medium">{formatCurrency(row.paidAmount)}</span>
                ),
              },
              {
                header: 'Outstanding',
                accessor: (row) => (
                  <span className={`font-semibold ${row.outstandingAmount > 0 ? 'text-amber-600' : 'text-gray-400'}`}>
                    {formatCurrency(row.outstandingAmount)}
                  </span>
                ),
              },
              {
                header: 'Status',
                accessor: (row) => (
                  <Badge variant={SETTLEMENT_STATUS_VARIANTS[row.settlementStatus] || 'neutral'}>
                    {row.settlementStatus?.replace('_', ' ')}
                  </Badge>
                ),
              },
              {
                header: 'UTR / Ref',
                accessor: (row) => row.paymentReference || 'Pending',
              },
              {
                header: 'Settled At',
                accessor: (row) => (row.settledAt ? new Date(row.settledAt).toLocaleDateString() : 'N/A'),
              },
            ]}
            data={settlements}
          />
        </Card>
      )}
    </div>
  );
}
