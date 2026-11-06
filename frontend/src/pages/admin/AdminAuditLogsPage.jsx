import React, { useState, useEffect } from 'react';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import ErrorState from '../../components/common/ErrorState';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import api from '../../services/api';
import { extractArray } from '../../services/responseUtils';

export default function AdminAuditLogsPage() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionFilter, setActionFilter] = useState('');
  const [entityFilter, setEntityFilter] = useState('');
  const [selectedLog, setSelectedLog] = useState(null);

  const fetchLogs = async () => {
    try {
      setLoading(true);
      setError(null);
      let query = '/admin/audit-logs?size=30';
      if (actionFilter) query += `&action=${encodeURIComponent(actionFilter)}`;
      if (entityFilter) query += `&entityType=${encodeURIComponent(entityFilter)}`;

      const res = await api.get(query);
      setLogs(extractArray(res));
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load audit logs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [actionFilter, entityFilter]);

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Security & Activity Audit Trail</h1>
        <p className="mt-1 text-sm text-gray-500">
          Immutable event log of all system security actions, administrative decisions, and financial operations.
        </p>
      </div>

      {/* Filter Toolbar */}
      <Card className="p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Filter by Action"
            placeholder="e.g. PAYMENT_RECORDED, ORDER_SUBMITTED"
            value={actionFilter}
            onChange={(e) => setActionFilter(e.target.value)}
          />
          <Input
            label="Filter by Entity Type"
            placeholder="e.g. ORDER, PAYMENT, SETTLEMENT, USER"
            value={entityFilter}
            onChange={(e) => setEntityFilter(e.target.value)}
          />
        </div>
      </Card>

      {/* Audit Log Table */}
      {loading ? (
        <div className="flex justify-center items-center py-24">
          <LoadingSpinner size="lg" />
        </div>
      ) : error ? (
        <ErrorState message={error} onRetry={fetchLogs} />
      ) : logs.length === 0 ? (
        <EmptyState
          title="No Audit Logs Found"
          description="Activity events will be recorded automatically as platform operations occur."
        />
      ) : (
        <Card padding="none">
          <ResponsiveTable
            columns={[
              {
                header: 'Timestamp',
                accessor: (row) => (
                  <span className="text-xs text-gray-500 whitespace-nowrap">
                    {row.createdAt ? new Date(row.createdAt).toLocaleString() : 'N/A'}
                  </span>
                ),
              },
              {
                header: 'Actor',
                accessor: (row) => (
                  <div>
                    <div className="font-semibold text-gray-900 text-sm">{row.actorName || 'System'}</div>
                    <div className="text-xs text-gray-400">{row.actorRole || 'SYSTEM'}</div>
                  </div>
                ),
              },
              {
                header: 'Action',
                accessor: (row) => (
                  <Badge variant="brand">{row.action}</Badge>
                ),
              },
              {
                header: 'Target Entity',
                accessor: (row) => (
                  <span className="text-xs font-medium text-gray-700 bg-gray-100 px-2 py-0.5 rounded">
                    {row.entityType} {row.entityId ? `(#${row.entityId})` : ''}
                  </span>
                ),
              },
              {
                header: 'IP Address',
                accessor: (row) => (
                  <span className="text-xs text-gray-400 font-mono">{row.ipAddress || '127.0.0.1'}</span>
                ),
              },
              {
                header: 'Details',
                accessor: (row) => (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => setSelectedLog(row)}
                  >
                    View Payload
                  </Button>
                ),
              },
            ]}
            data={logs}
          />
        </Card>
      )}

      {/* Payload Modal */}
      {selectedLog && (
        <Modal
          isOpen={!!selectedLog}
          onClose={() => setSelectedLog(null)}
          title={`Audit Event: ${selectedLog.action}`}
        >
          <div className="space-y-3">
            <div className="text-xs text-gray-500">
              Recorded at: {selectedLog.createdAt ? new Date(selectedLog.createdAt).toLocaleString() : 'N/A'}
            </div>
            <div className="text-xs text-gray-500">
              Actor: {selectedLog.actorName} ({selectedLog.actorRole}) | IP: {selectedLog.ipAddress || 'N/A'}
            </div>
            <div className="bg-slate-900 text-slate-100 p-4 rounded-md text-xs font-mono overflow-x-auto max-h-60">
              <pre>{selectedLog.details || 'No payload recorded'}</pre>
            </div>
            <div className="flex justify-end pt-3">
              <Button variant="secondary" onClick={() => setSelectedLog(null)}>
                Close
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
