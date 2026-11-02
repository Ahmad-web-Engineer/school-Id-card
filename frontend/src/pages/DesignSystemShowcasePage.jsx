import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import Button from '../components/common/Button';
import Input from '../components/common/Input';
import Select from '../components/common/Select';
import Textarea from '../components/common/Textarea';
import Card from '../components/common/Card';
import Badge from '../components/common/Badge';
import Modal from '../components/common/Modal';
import ResponsiveTable from '../components/common/ResponsiveTable';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import ErrorState from '../components/common/ErrorState';
import { Activity, ShieldCheck, Laptop, Smartphone, Check, Sparkles } from 'lucide-react';

const mockTableData = [
  { id: '1', name: 'Springfield High School', code: 'SCH-001', students: 1250, status: 'Active' },
  { id: '2', name: 'Oakridge Academy', code: 'SCH-002', students: 840, status: 'Active' },
  { id: '3', name: 'Green Valley Public School', code: 'SCH-003', students: 430, status: 'Pending Review' },
];

const mockColumns = [
  { key: 'code', label: 'School Code' },
  { key: 'name', label: 'School Name' },
  { key: 'students', label: 'Enrolled' },
  {
    key: 'status',
    label: 'Status',
    render: (val) => (
      <Badge variant={val === 'Active' ? 'success' : 'warning'} size="sm">
        {val}
      </Badge>
    ),
  },
];

export default function DesignSystemShowcasePage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [btnLoading, setBtnLoading] = useState(false);
  const [inputVal, setInputVal] = useState('');
  const [inputErr, setInputErr] = useState('');

  // Live health query to test Axios + TanStack Query against backend API
  const { data: healthData, isLoading: healthLoading, isError: healthIsError, error: healthError, refetch: refetchHealth } = useQuery({
    queryKey: ['system-health'],
    queryFn: () => api.get('/health'),
    retry: 1,
  });

  return (
    <div className="space-y-8 pb-12">
      {/* Top Banner */}
      <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="p-2 rounded-lg bg-indigo-50 text-indigo-600">
                <Sparkles className="h-5 w-5" />
              </span>
              <h1 className="text-xl font-bold text-slate-900">Platform Design System & Verification</h1>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Reusable UI tokens, responsive layouts (360px - 1920px), and state containers.
            </p>
          </div>
          <div className="flex items-center gap-2 text-xs font-medium text-slate-600 bg-slate-100 px-3 py-1.5 rounded-lg">
            <Smartphone className="h-4 w-4 text-slate-500" /> 360px Mobile &bull; <Laptop className="h-4 w-4 text-slate-500" /> 1920px Desktop
          </div>
        </div>
      </div>

      {/* Backend API Health Status Check */}
      <Card
        title="Live Backend Connectivity (Contract Verification)"
        subtitle="Verifies frontend Axios client and backend GET /api/v1/health API integration"
      >
        {healthLoading ? (
          <div className="flex items-center gap-3 py-4 text-slate-600">
            <LoadingSpinner size="md" className="text-indigo-600" />
            <span className="text-xs font-medium">Probing /api/v1/health...</span>
          </div>
        ) : healthIsError ? (
          <ErrorState
            title="Backend Not Reachable"
            message={healthError?.message || 'Could not connect to Spring Boot server.'}
            errorCode={healthError?.errorCode}
            onRetry={refetchHealth}
          />
        ) : (
          <div className="p-4 rounded-lg bg-emerald-50 border border-emerald-200 flex items-start justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-full bg-emerald-100 text-emerald-700">
                <ShieldCheck className="h-5 w-5" />
              </div>
              <div>
                <h4 className="text-xs font-bold text-emerald-900 uppercase tracking-wide">
                  Backend API Operational
                </h4>
                <div className="mt-1 text-xs text-emerald-800 space-y-0.5">
                  <p>Service: <code className="font-mono">{healthData?.data?.service}</code></p>
                  <p>Version: <code className="font-mono">{healthData?.data?.version}</code> &bull; Status: <code className="font-mono">{healthData?.data?.status}</code></p>
                  <p className="text-[11px] text-emerald-600">Timestamp: {healthData?.data?.timestamp}</p>
                </div>
              </div>
            </div>
            <Button size="sm" variant="secondary" onClick={() => refetchHealth()}>
              Refresh Health
            </Button>
          </div>
        )}
      </Card>

      {/* Buttons Showcase */}
      <Card title="Button Component" subtitle="Accessible actions with states">
        <div className="space-y-4">
          <div className="flex flex-wrap items-center gap-3">
            <Button variant="primary">Primary Action</Button>
            <Button variant="secondary">Secondary Action</Button>
            <Button variant="outline">Outline Action</Button>
            <Button variant="danger">Danger Action</Button>
          </div>
          <div className="flex flex-wrap items-center gap-3 pt-2 border-t border-slate-100">
            <Button size="sm">Small (sm)</Button>
            <Button size="md">Medium (md)</Button>
            <Button size="lg">Large (lg)</Button>
            <Button
              loading={btnLoading}
              onClick={() => {
                setBtnLoading(true);
                setTimeout(() => setBtnLoading(false), 1500);
              }}
            >
              Click for Loading State
            </Button>
            <Button disabled>Disabled Action</Button>
          </div>
        </div>
      </Card>

      {/* Badges Showcase */}
      <Card title="Badges & Status Tokens" subtitle="Role tags and order status representations">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="brand">SUPER_ADMIN</Badge>
          <Badge variant="info">SCHOOL</Badge>
          <Badge variant="warning">PRINTING_AGENCY</Badge>
          <Badge variant="success">Active / Approved</Badge>
          <Badge variant="danger">Rejected / Defect</Badge>
          <Badge variant="neutral">Draft</Badge>
        </div>
      </Card>

      {/* Form Controls Showcase */}
      <Card title="Form Inputs & Validation" subtitle="Semantic labels, focus rings, error binding">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-2xl">
          <Input
            label="School Name"
            placeholder="e.g. Greenwood International"
            required
            helperText="Official registered school name."
          />
          <Input
            label="Contact Email"
            type="email"
            placeholder="admin@school.edu"
            required
            value={inputVal}
            onChange={(e) => {
              setInputVal(e.target.value);
              if (!e.target.value.includes('@')) {
                setInputErr('Please enter a valid school email address');
              } else {
                setInputErr('');
              }
            }}
            error={inputErr}
          />
          <Select
            label="Academic Session"
            required
            options={[
              { value: '2026-2027', label: '2026-2027 (Current)' },
              { value: '2027-2028', label: '2027-2028' },
            ]}
          />
          <Input label="Disabled Field" disabled value="Read-only system identifier" />
          <div className="sm:col-span-2">
            <Textarea
              label="Delivery Instructions"
              placeholder="Provide delivery landmarks or specific dispatch instructions..."
              helperText="Optional notes for courier partner."
            />
          </div>
        </div>
      </Card>

      {/* Responsive Table Component Showcase */}
      <Card
        title="Responsive Data Table (Desktop Table ↔ Mobile Stacked Cards)"
        subtitle="Automatic responsiveness: clean grid on desktop, cards on mobile viewport (< 768px)"
      >
        <ResponsiveTable
          columns={mockColumns}
          data={mockTableData}
          keyField="id"
        />
      </Card>

      {/* State Indicators Showcase (Empty & Error) */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card title="Empty State Component">
          <EmptyState
            title="No orders yet"
            description="When the school places an ID card order, it will appear here."
            action={<Button size="sm">Create First Order</Button>}
          />
        </Card>
        <Card title="Error State Component">
          <ErrorState
            title="Failed to load student records"
            message="Database connection timed out while querying the cohort."
            errorCode="RECORD_ACCESS_FORBIDDEN"
            onRetry={() => alert('Retry triggered')}
          />
        </Card>
      </div>

      {/* Modal Dialog Showcase */}
      <Card title="Accessible Modal Dialog" subtitle="Focus management and backdrop controls">
        <div>
          <Button onClick={() => setModalOpen(true)}>Open Modal Dialog</Button>
          <Modal
            isOpen={modalOpen}
            onClose={() => setModalOpen(false)}
            title="Confirm Student Verification"
            footer={
              <>
                <Button variant="outline" size="sm" onClick={() => setModalOpen(false)}>
                  Cancel
                </Button>
                <Button size="sm" onClick={() => setModalOpen(false)}>
                  Confirm & Submit
                </Button>
              </>
            }
          >
            <p className="text-sm text-slate-600">
              Are you sure you want to approve this student batch? Snapshots of student data and
              card designs will be permanently locked for the printing order.
            </p>
          </Modal>
        </div>
      </Card>
    </div>
  );
}
