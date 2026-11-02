import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import { Plus, Search, UserPlus, Building2, Check, AlertCircle } from 'lucide-react';

export default function AgenciesPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [isAddUserOpen, setIsAddUserOpen] = useState(false);
  const [selectedAgency, setSelectedAgency] = useState(null);

  // Form states
  const [agencyForm, setAgencyForm] = useState({
    agencyName: '',
    contactPerson: '',
    email: '',
    mobile: '',
    address: '',
    productionCapacity: 5000,
    capabilities: 'Thermal PVC, UV Gloss, Barcode/QR',
  });
  const [userForm, setUserForm] = useState({
    fullName: '',
    email: '',
    mobile: '',
    password: '',
    designation: 'Production Manager',
  });
  const [formError, setFormError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Fetch paginated agencies
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['admin-agencies', page, search],
    queryFn: () =>
      api.get('/admin/agencies', {
        params: { page, size: 10, search: search.trim() || undefined },
      }),
  });

  // Mutation: Register Agency
  const registerMutation = useMutation({
    mutationFn: (newAgency) => api.post('/admin/agencies', newAgency),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-agencies'] });
      setIsRegisterOpen(false);
      setSuccessMsg('Printing agency registered successfully!');
      setTimeout(() => setSuccessMsg(''), 4000);
      setAgencyForm({
        agencyName: '',
        contactPerson: '',
        email: '',
        mobile: '',
        address: '',
        productionCapacity: 5000,
        capabilities: 'Thermal PVC, UV Gloss, Barcode/QR',
      });
    },
    onError: (err) => {
      setFormError(err.message || 'Failed to register printing agency.');
    },
  });

  // Mutation: Create Agency User
  const addUserMutation = useMutation({
    mutationFn: ({ agencyId, userData }) =>
      api.post(`/admin/agencies/${agencyId}/users`, userData),
    onSuccess: () => {
      setIsAddUserOpen(false);
      setSuccessMsg(`User created successfully for ${selectedAgency?.agencyName}!`);
      setTimeout(() => setSuccessMsg(''), 4000);
      setUserForm({
        fullName: '',
        email: '',
        mobile: '',
        password: '',
        designation: 'Production Manager',
      });
    },
    onError: (err) => {
      setFormError(err.message || 'Failed to create agency user.');
    },
  });

  const handleRegisterSubmit = (e) => {
    e.preventDefault();
    setFormError('');
    registerMutation.mutate({
      ...agencyForm,
      productionCapacity: Number(agencyForm.productionCapacity) || 1000,
    });
  };

  const handleAddUserSubmit = (e) => {
    e.preventDefault();
    setFormError('');
    if (!selectedAgency) return;
    addUserMutation.mutate({ agencyId: selectedAgency.id, userData: userForm });
  };

  const columns = [
    { key: 'agencyName', label: 'Agency Name', cellClassName: 'font-semibold text-slate-900' },
    { key: 'contactPerson', label: 'Contact Person' },
    { key: 'email', label: 'Email' },
    { key: 'mobile', label: 'Phone' },
    {
      key: 'productionCapacity',
      label: 'Capacity / Day',
      render: (val) => `${Number(val || 0).toLocaleString()} cards`,
    },
    {
      key: 'status',
      label: 'Status',
      render: (val) => (
        <Badge variant={val === 'ACTIVE' ? 'success' : 'neutral'} size="sm">
          {val}
        </Badge>
      ),
    },
    {
      key: 'actions',
      label: 'Actions',
      render: (_, row) => (
        <Button
          size="sm"
          variant="outline"
          onClick={() => {
            setSelectedAgency(row);
            setFormError('');
            setIsAddUserOpen(true);
          }}
        >
          <UserPlus className="h-3.5 w-3.5 mr-1" />
          Add User
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Printing Agencies</h1>
          <p className="text-xs text-slate-500 mt-1">
            Fulfillment partners and print workshops. Non-negotiable: Agencies are never visible to schools.
          </p>
        </div>
        <Button
          onClick={() => {
            setFormError('');
            setIsRegisterOpen(true);
          }}
        >
          <Plus className="h-4 w-4 mr-1.5" />
          Register Agency
        </Button>
      </div>

      {successMsg && (
        <div className="p-3 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2">
          <Check className="h-4 w-4 text-emerald-600" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Filter / Search Bar */}
      <Card bodyClassName="p-4">
        <div className="flex flex-col sm:flex-row items-center gap-3">
          <div className="relative flex-1 w-full">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search agencies by name or contact person..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="w-full pl-9 pr-4 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <Button variant="secondary" size="sm" onClick={() => refetch()}>
            Refresh
          </Button>
        </div>
      </Card>

      {/* Responsive Agencies Table (Cards on Mobile) */}
      <ResponsiveTable
        columns={columns}
        data={data?.data || []}
        loading={isLoading}
        error={error}
        onRetry={refetch}
        emptyTitle="No agencies found"
        emptyMessage={search ? 'No agencies match your search query.' : 'Click "Register Agency" to onboard your first printing agency.'}
      />

      {/* Pagination Controls */}
      {data?.pagination && data.pagination.totalPages > 1 && (
        <div className="flex items-center justify-between text-xs text-slate-600 px-2">
          <span>
            Showing page {data.pagination.page + 1} of {data.pagination.totalPages} ({data.pagination.totalElements} agencies)
          </span>
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="outline"
              disabled={data.pagination.page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </Button>
            <Button
              size="sm"
              variant="outline"
              disabled={data.pagination.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      {/* Modal: Register Agency */}
      <Modal
        isOpen={isRegisterOpen}
        onClose={() => setIsRegisterOpen(false)}
        title="Register Printing Agency"
        footer={
          <>
            <Button variant="outline" size="sm" onClick={() => setIsRegisterOpen(false)}>
              Cancel
            </Button>
            <Button size="sm" loading={registerMutation.isPending} onClick={handleRegisterSubmit}>
              Register Agency
            </Button>
          </>
        }
      >
        <form onSubmit={handleRegisterSubmit} className="space-y-4">
          {formError && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input
              label="Agency Name"
              required
              placeholder="e.g. Apex Card Printers"
              value={agencyForm.agencyName}
              onChange={(e) => setAgencyForm({ ...agencyForm, agencyName: e.target.value })}
            />
            <Input
              label="Contact Person"
              required
              placeholder="e.g. Robert Smith"
              value={agencyForm.contactPerson}
              onChange={(e) => setAgencyForm({ ...agencyForm, contactPerson: e.target.value })}
            />
            <Input
              label="Email Address"
              type="email"
              required
              placeholder="contact@apexprinters.com"
              value={agencyForm.email}
              onChange={(e) => setAgencyForm({ ...agencyForm, email: e.target.value })}
            />
            <Input
              label="Phone Number"
              required
              placeholder="9876543210"
              value={agencyForm.mobile}
              onChange={(e) => setAgencyForm({ ...agencyForm, mobile: e.target.value })}
            />
            <Input
              label="Daily Capacity (Cards/Day)"
              type="number"
              placeholder="5000"
              value={agencyForm.productionCapacity}
              onChange={(e) => setAgencyForm({ ...agencyForm, productionCapacity: e.target.value })}
            />
            <Input
              label="Special Capabilities"
              placeholder="e.g. UV Gloss, Smartcard RFID"
              value={agencyForm.capabilities}
              onChange={(e) => setAgencyForm({ ...agencyForm, capabilities: e.target.value })}
            />
            <div className="sm:col-span-2">
              <Input
                label="Workshop / Facility Address"
                placeholder="45 Industrial Zone"
                value={agencyForm.address}
                onChange={(e) => setAgencyForm({ ...agencyForm, address: e.target.value })}
              />
            </div>
          </div>
        </form>
      </Modal>

      {/* Modal: Add Agency User */}
      <Modal
        isOpen={isAddUserOpen}
        onClose={() => setIsAddUserOpen(false)}
        title={`Provision Login for ${selectedAgency?.agencyName || 'Agency'}`}
        footer={
          <>
            <Button variant="outline" size="sm" onClick={() => setIsAddUserOpen(false)}>
              Cancel
            </Button>
            <Button size="sm" loading={addUserMutation.isPending} onClick={handleAddUserSubmit}>
              Create Account
            </Button>
          </>
        }
      >
        <form onSubmit={handleAddUserSubmit} className="space-y-4">
          {formError && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}
          <div className="space-y-3">
            <Input
              label="Full Name"
              required
              placeholder="e.g. Ramesh Kumar"
              value={userForm.fullName}
              onChange={(e) => setUserForm({ ...userForm, fullName: e.target.value })}
            />
            <Input
              label="Login Email"
              type="email"
              required
              placeholder="operator@agency.com"
              value={userForm.email}
              onChange={(e) => setUserForm({ ...userForm, email: e.target.value })}
            />
            <Input
              label="Password (min 8 characters)"
              type="password"
              required
              placeholder="••••••••"
              value={userForm.password}
              onChange={(e) => setUserForm({ ...userForm, password: e.target.value })}
            />
            <Input
              label="Designation / Title"
              placeholder="e.g. Production Supervisor"
              value={userForm.designation}
              onChange={(e) => setUserForm({ ...userForm, designation: e.target.value })}
            />
            <Input
              label="Mobile Number"
              placeholder="9876543210"
              value={userForm.mobile}
              onChange={(e) => setUserForm({ ...userForm, mobile: e.target.value })}
            />
          </div>
        </form>
      </Modal>
    </div>
  );
}
