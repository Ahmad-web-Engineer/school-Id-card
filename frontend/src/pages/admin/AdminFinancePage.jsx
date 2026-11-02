import React, { useState, useEffect } from 'react';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import PaySettlementModal from '../../features/finance/PaySettlementModal';
import {
  INVOICE_STATUS_VARIANTS,
  SETTLEMENT_STATUS_VARIANTS,
  PAYMENT_STATUS_VARIANTS,
  formatCurrency,
} from '../../features/finance/financeConstants';
import api from '../../services/api';

export default function AdminFinancePage() {
  const [activeTab, setActiveTab] = useState('overview');
  const [summary, setSummary] = useState(null);
  const [invoices, setInvoices] = useState([]);
  const [payments, setPayments] = useState([]);
  const [settlements, setSettlements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Pay Settlement Modal State
  const [payModalOpen, setPayModalOpen] = useState(false);
  const [selectedSettlement, setSelectedSettlement] = useState(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [sumRes, invRes, payRes, setRes] = await Promise.all([
        api.get('/admin/finance/summary'),
        api.get('/admin/finance/invoices'),
        api.get('/admin/finance/payments'),
        api.get('/admin/finance/settlements'),
      ]);

      setSummary(sumRes.data?.data || null);
      setInvoices(invRes.data?.data?.data || []);
      setPayments(payRes.data?.data?.data || []);
      setSettlements(setRes.data?.data?.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load global financial records');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handlePaySettlement = (settlement) => {
    setSelectedSettlement(settlement);
    setPayModalOpen(true);
  };

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

  const marginPercentage =
    summary?.totalRevenue > 0
      ? ((summary.grossMargin / summary.totalRevenue) * 100).toFixed(1)
      : '0.0';

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Finance & P&L Accounting</h1>
        <p className="mt-1 text-sm text-gray-500">
          Global revenue ledger, agency cost settlements, receivables, and platform margins.
        </p>
      </div>

      {/* P&L Metric Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <Card className="p-5">
          <div className="text-sm font-medium text-gray-500">Gross Billed Revenue</div>
          <div className="mt-2 text-2xl font-bold text-gray-900">
            {formatCurrency(summary?.grossRevenue)}
          </div>
          <div className="mt-1 text-xs text-emerald-600 font-medium">
            {formatCurrency(summary?.collectedRevenue)} collected
          </div>
        </Card>

        <Card className="p-5">
          <div className="text-sm font-medium text-gray-500">Agency Cost of Goods</div>
          <div className="mt-2 text-2xl font-bold text-gray-900">
            {formatCurrency(summary?.agencyDirectCost)}
          </div>
          <div className="mt-1 text-xs text-gray-500">
            {formatCurrency(summary?.agencySettledPayouts)} disbursed
          </div>
        </Card>

        <Card className="p-5 border-l-4 border-emerald-500">
          <div className="text-sm font-medium text-gray-500">Gross Contribution</div>
          <div className="mt-2 text-2xl font-bold text-emerald-600">
            {formatCurrency(summary?.grossContribution)}
          </div>
          <div className="mt-1 text-xs text-emerald-700 font-medium">
            {summary?.grossMarginPercentage != null ? `${summary.grossMarginPercentage}% margin` : '0.0% margin'}
          </div>
        </Card>

        <Card className="p-5 border-l-4 border-amber-500">
          <div className="text-sm font-medium text-gray-500">Outstanding Receivables</div>
          <div className="mt-2 text-2xl font-bold text-amber-600">
            {formatCurrency(summary?.outstandingReceivables)}
          </div>
          <div className="mt-1 text-xs text-amber-700">
            {formatCurrency(summary?.agencyPendingPayables)} pending agency payables
          </div>
        </Card>
      </div>

      {/* Navigation Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab('overview')}
            className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
              activeTab === 'overview'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Invoices ({invoices.length})
          </button>
          <button
            onClick={() => setActiveTab('settlements')}
            className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
              activeTab === 'settlements'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Agency Settlements ({settlements.length})
          </button>
          <button
            onClick={() => setActiveTab('payments')}
            className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
              activeTab === 'payments'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            School Payments ({payments.length})
          </button>
        </nav>
      </div>

      {/* Invoices Tab */}
      {activeTab === 'overview' && (
        <div>
          {invoices.length === 0 ? (
            <EmptyState
              title="No Invoices Found"
              description="Generated invoices across all schools will appear in this registry."
            />
          ) : (
            <Card padding="none">
              <ResponsiveTable
                columns={[
                  {
                    header: 'Invoice #',
                    accessor: (row) => (
                      <span className="font-semibold text-indigo-600">{row.invoiceNumber}</span>
                    ),
                  },
                  {
                    header: 'School',
                    accessor: (row) => row.schoolName,
                  },
                  {
                    header: 'Order #',
                    accessor: (row) => row.orderNumber,
                  },
                  {
                    header: 'Total',
                    accessor: (row) => (
                      <span className="font-medium text-gray-900">{formatCurrency(row.totalAmount)}</span>
                    ),
                  },
                  {
                    header: 'Collected',
                    accessor: (row) => (
                      <span className="text-emerald-600 font-medium">{formatCurrency(row.paidAmount)}</span>
                    ),
                  },
                  {
                    header: 'Due',
                    accessor: (row) => (
                      <span className={`font-semibold ${row.dueAmount > 0 ? 'text-amber-600' : 'text-gray-500'}`}>
                        {formatCurrency(row.dueAmount)}
                      </span>
                    ),
                  },
                  {
                    header: 'Status',
                    accessor: (row) => (
                      <Badge variant={INVOICE_STATUS_VARIANTS[row.invoiceStatus] || 'neutral'}>
                        {row.invoiceStatus?.replace('_', ' ')}
                      </Badge>
                    ),
                  },
                  {
                    header: 'Due Date',
                    accessor: (row) => row.dueDate || 'N/A',
                  },
                ]}
                data={invoices}
              />
            </Card>
          )}
        </div>
      )}

      {/* Agency Settlements Tab */}
      {activeTab === 'settlements' && (
        <div>
          {settlements.length === 0 ? (
            <EmptyState
              title="No Agency Settlements"
              description="Agency settlements generated after job completion will be listed here."
            />
          ) : (
            <Card padding="none">
              <ResponsiveTable
                columns={[
                  {
                    header: 'Settlement #',
                    accessor: (row) => (
                      <span className="font-semibold text-indigo-600">{row.settlementNumber}</span>
                    ),
                  },
                  {
                    header: 'Printing Agency',
                    accessor: (row) => row.agencyName,
                  },
                  {
                    header: 'Job #',
                    accessor: (row) => row.jobNumber,
                  },
                  {
                    header: 'Agreed',
                    accessor: (row) => formatCurrency(row.agreedAmount),
                  },
                  {
                    header: 'Reprint Deduction',
                    accessor: (row) => (
                      <span className={row.deductionAmount > 0 ? 'text-red-600 font-medium' : 'text-gray-400'}>
                        {row.deductionAmount > 0 ? `-${formatCurrency(row.deductionAmount)}` : '₹0.00'}
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
                    header: 'Actions',
                    accessor: (row) =>
                      row.outstandingAmount > 0 ? (
                        <Button
                          size="sm"
                          variant="primary"
                          onClick={() => handlePaySettlement(row)}
                        >
                          Disburse Payout
                        </Button>
                      ) : (
                        <span className="text-xs text-emerald-600 font-medium">Fully Settled</span>
                      ),
                  },
                ]}
                data={settlements}
              />
            </Card>
          )}
        </div>
      )}

      {/* Payments Tab */}
      {activeTab === 'payments' && (
        <div>
          {payments.length === 0 ? (
            <EmptyState
              title="No School Payments Recorded"
              description="Inward payments from client schools will appear in this ledger."
            />
          ) : (
            <Card padding="none">
              <ResponsiveTable
                columns={[
                  {
                    header: 'Receipt #',
                    accessor: (row) => (
                      <span className="font-semibold text-gray-900">{row.paymentReference}</span>
                    ),
                  },
                  {
                    header: 'School',
                    accessor: (row) => row.schoolName,
                  },
                  {
                    header: 'Order #',
                    accessor: (row) => row.orderNumber,
                  },
                  {
                    header: 'Amount',
                    accessor: (row) => (
                      <span className="font-semibold text-gray-900">{formatCurrency(row.amount)}</span>
                    ),
                  },
                  {
                    header: 'Method',
                    accessor: (row) => (
                      <span className="text-xs font-medium text-gray-600 uppercase bg-gray-100 px-2 py-1 rounded">
                        {row.paymentMethod?.replace('_', ' ')}
                      </span>
                    ),
                  },
                  {
                    header: 'UTR / Ref',
                    accessor: (row) => row.transactionReference || 'N/A',
                  },
                  {
                    header: 'Status',
                    accessor: (row) => (
                      <Badge variant={PAYMENT_STATUS_VARIANTS[row.paymentStatus] || 'neutral'}>
                        {row.paymentStatus}
                      </Badge>
                    ),
                  },
                  {
                    header: 'Received At',
                    accessor: (row) =>
                      row.receivedAt ? new Date(row.receivedAt).toLocaleDateString() : 'N/A',
                  },
                ]}
                data={payments}
              />
            </Card>
          )}
        </div>
      )}

      {/* Disburse Payout Modal */}
      {payModalOpen && selectedSettlement && (
        <PaySettlementModal
          isOpen={payModalOpen}
          onClose={() => setPayModalOpen(false)}
          settlementId={selectedSettlement.id}
          settlementNumber={selectedSettlement.settlementNumber}
          payableAmount={selectedSettlement.payableAmount}
          outstandingAmount={selectedSettlement.outstandingAmount}
          onSettlementPaid={fetchData}
        />
      )}
    </div>
  );
}
