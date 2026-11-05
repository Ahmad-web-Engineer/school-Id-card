import React, { useState, useEffect } from 'react';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import RecordPaymentModal from '../../features/finance/RecordPaymentModal';
import { INVOICE_STATUS_VARIANTS, PAYMENT_STATUS_VARIANTS, formatCurrency } from '../../features/finance/financeConstants';
import api from '../../services/api';
import { extractArray } from '../../services/responseUtils';

export default function SchoolFinancePage() {
  const [activeTab, setActiveTab] = useState('invoices');
  const [summary, setSummary] = useState(null);
  const [invoices, setInvoices] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Pay modal state
  const [payModalOpen, setPayModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [sumRes, invRes, payRes] = await Promise.all([
        api.get('/school/finance/summary'),
        api.get('/school/finance/invoices'),
        api.get('/school/finance/payments'),
      ]);

      setSummary(sumRes?.data?.data || sumRes?.data || null);
      setInvoices(extractArray(invRes));
      setPayments(extractArray(payRes));
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load financial records');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handlePayClick = (inv) => {
    setSelectedOrder({
      id: inv.orderId,
      number: inv.orderNumber,
      dueAmount: inv.dueAmount,
    });
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

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Billing & Payments</h1>
        <p className="mt-1 text-sm text-gray-500">
          Track tax invoices, submit online/offline milestone payments, and view accounting balances.
        </p>
      </div>

      {/* Financial Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <Card className="p-5">
          <div className="text-sm font-medium text-gray-500">Total Billed</div>
          <div className="mt-2 text-2xl font-bold text-gray-900">
            {formatCurrency(summary?.totalInvoiced)}
          </div>
          <div className="mt-1 text-xs text-gray-400">Total of all issued invoices</div>
        </Card>

        <Card className="p-5">
          <div className="text-sm font-medium text-gray-500">Total Paid</div>
          <div className="mt-2 text-2xl font-bold text-emerald-600">
            {formatCurrency(summary?.totalPaid)}
          </div>
          <div className="mt-1 text-xs text-gray-400">Cleared payments to platform</div>
        </Card>

        <Card className="p-5 border-l-4 border-amber-500">
          <div className="text-sm font-medium text-gray-500">Outstanding Balance</div>
          <div className="mt-2 text-2xl font-bold text-amber-600">
            {formatCurrency(summary?.outstandingBalance)}
          </div>
          <div className="mt-1 text-xs text-gray-400">Pending payment amount</div>
        </Card>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab('invoices')}
            className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
              activeTab === 'invoices'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Tax Invoices ({invoices.length})
          </button>
          <button
            onClick={() => setActiveTab('payments')}
            className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
              activeTab === 'payments'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Payment History ({payments.length})
          </button>
        </nav>
      </div>

      {/* Invoices Tab */}
      {activeTab === 'invoices' && (
        <div>
          {invoices.length === 0 ? (
            <EmptyState
              title="No Invoices Issued Yet"
              description="Tax invoices will appear here once your orders are verified and billed."
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
                    header: 'Order #',
                    accessor: (row) => row.orderNumber,
                  },
                  {
                    header: 'Total Amount',
                    accessor: (row) => (
                      <span className="font-medium text-gray-900">{formatCurrency(row.totalAmount)}</span>
                    ),
                  },
                  {
                    header: 'Paid',
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
                  {
                    header: 'Actions',
                    accessor: (row) =>
                      row.dueAmount > 0 ? (
                        <Button
                          size="sm"
                          variant="primary"
                          onClick={() => handlePayClick(row)}
                        >
                          Make Payment
                        </Button>
                      ) : (
                        <span className="text-xs text-emerald-600 font-medium">Settled in Full</span>
                      ),
                  },
                ]}
                data={invoices}
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
              title="No Payments Recorded"
              description="Payments made towards your order invoices will appear in this audit log."
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
                    header: 'Transaction ID',
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
                    header: 'Received Date',
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

      {/* Payment Modal */}
      {payModalOpen && selectedOrder && (
        <RecordPaymentModal
          isOpen={payModalOpen}
          onClose={() => setPayModalOpen(false)}
          orderId={selectedOrder.id}
          orderNumber={selectedOrder.number}
          dueAmount={selectedOrder.dueAmount}
          role="SCHOOL"
          onPaymentRecorded={fetchData}
        />
      )}
    </div>
  );
}
