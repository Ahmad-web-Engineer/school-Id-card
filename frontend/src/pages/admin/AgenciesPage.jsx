import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import { 
  Plus, 
  Search, 
  UserPlus, 
  Building2, 
  Check, 
  AlertCircle, 
  KeyRound, 
  Lock, 
  CheckCircle2 
} from 'lucide-react';

export default function AgenciesPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [isAddUserOpen, setIsAddUserOpen] = useState(false);
  const [isManageUsersOpen, setIsManageUsersOpen] = useState(false);
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

  // Password reset state
  const [activeResetUserId, setActiveResetUserId] = useState(null);
  const [newPassword, setNewPassword] = useState('');
  const [resetError, setResetError] = useState('');
  const [resetSuccess, setResetSuccess] = useState('');
  const [resetLoading, setResetLoading] = useState(false);

  // Fetch paginated agencies
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['admin-agencies', page, search],
    queryFn: () =>
      api.get('/admin/agencies', {
        params: { page, size: 10, search: search.trim() || undefined },
      }),
  });

  // Fetch agency users when Manage Users modal is open
  const { 
    data: agencyUsersData, 
    isLoading: isUsersLoading, 
    refetch: refetchUsers 
  } = useQuery({
    queryKey: ['admin-agency-users', selectedAgency?.id],
    queryFn: () => api.get(`/admin/agencies/${selectedAgency.id}/users`),
    enabled: isManageUsersOpen && !!selectedAgency,
  });

  const agencyUsers = agencyUsersData?.data || [];

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
      queryClient.invalidateQueries({ queryKey: ['admin-agency-users', selectedAgency?.id] });
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
    registerMutation.mutate(agencyForm);
  };

  const handleAddUserSubmit = (e) => {
    e.preventDefault();
    setFormError('');
    if (!selectedAgency) return;
    addUserMutation.mutate({ agencyId: selectedAgency.id, userData: userForm });
  };

  const handleResetPassword = async (userId) => {
    if (!newPassword || newPassword.length < 8) {
      setResetError('Password must be at least 8 characters long.');
      return;
    }
    setResetLoading(true);
    setResetError('');
    setResetSuccess('');

    try {
      await api.post(`/admin/users/${userId}/reset-password`, {
        newPassword,
      });
      setResetSuccess('Password reset successfully!');
      setActiveResetUserId(null);
      setNewPassword('');
      setTimeout(() => setResetSuccess(''), 4000);
    } catch (err) {
      setResetError(err.message || 'Failed to reset password.');
    } finally {
      setResetLoading(false);
    }
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
        <div className="flex items-center gap-1.5">
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setSelectedAgency(row);
              setResetError('');
              setResetSuccess('');
              setActiveResetUserId(null);
              setIsManageUsersOpen(true);
            }}
            title="Manage Agency User Logins and Passwords"
          >
            <KeyRound className="h-3.5 w-3.5 mr-1" />
            Logins
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setSelectedAgency(row);
              setFormError('');
              setIsAddUserOpen(true);
            }}
            title="Provision Additional Agency User"
          >
            <UserPlus className="h-3.5 w-3.5 mr-1" />
            Add User
          </Button>
        </div>
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
            Verified production partners, manufacturing capacity tracking, and agency administrative credentials.
          </p>
        </div>
        <Button
          onClick={() => {
            setFormError('');
            setIsRegisterOpen(true);
          }}
        >
          <Plus className="h-4 w-4 mr-1.5" />
          Onboard Agency
        </Button>
      </div>

      {/* Alerts */}
      {successMsg && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-lg flex items-center gap-2">
          <Check className="h-4 w-4 shrink-0 text-emerald-600" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Filter / Search Bar */}
      <Card>
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search by agency name, contact person, or email..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="w-full pl-9 pr-4 py-2 border border-slate-300 rounded-lg text-xs focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>
        </div>
      </Card>

      {/* Agencies Table */}
      <Card>
        <ResponsiveTable
          columns={columns}
          data={data?.data || []}
          isLoading={isLoading}
          keyField="id"
          emptyMessage="No printing agencies found matching your search criteria."
          pagination={{
            page: data?.pagination?.page || 0,
            totalPages: data?.pagination?.totalPages || 0,
            totalElements: data?.pagination?.totalElements || 0,
            onPageChange: (newPage) => setPage(newPage),
          }}
        />
      </Card>

      {/* Modal: Register Agency */}
      <Modal
        isOpen={isRegisterOpen}
        onClose={() => setIsRegisterOpen(false)}
        title="Onboard New Printing Agency"
        maxWidth="max-w-xl"
        footer={
          <>
            <Button
              variant="secondary"
              onClick={() => setIsRegisterOpen(false)}
              disabled={registerMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleRegisterSubmit}
              loading={registerMutation.isPending}
            >
              Onboard Agency
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
              placeholder="e.g. Apex Card Printers Ltd"
              value={agencyForm.agencyName}
              onChange={(e) => setAgencyForm({ ...agencyForm, agencyName: e.target.value })}
            />
            <Input
              label="Contact Person"
              required
              placeholder="e.g. Suresh Patel"
              value={agencyForm.contactPerson}
              onChange={(e) => setAgencyForm({ ...agencyForm, contactPerson: e.target.value })}
            />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input
              label="Email Address"
              type="email"
              required
              placeholder="agency@apexprints.com"
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
          </div>
          <Input
            label="Facility Address"
            required
            placeholder="Plot 45, Peenya Industrial Area, Phase II"
            value={agencyForm.address}
            onChange={(e) => setAgencyForm({ ...agencyForm, address: e.target.value })}
          />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input
              label="Daily Capacity (Cards / Day)"
              type="number"
              required
              value={agencyForm.productionCapacity}
              onChange={(e) => setAgencyForm({ ...agencyForm, productionCapacity: parseInt(e.target.value, 10) || 0 })}
            />
            <Input
              label="Capabilities"
              placeholder="Thermal PVC, UV Gloss, RFID"
              value={agencyForm.capabilities}
              onChange={(e) => setAgencyForm({ ...agencyForm, capabilities: e.target.value })}
            />
          </div>
        </form>
      </Modal>

      {/* Modal: Add Agency User */}
      <Modal
        isOpen={isAddUserOpen}
        onClose={() => setIsAddUserOpen(false)}
        title={`Add Staff Account for ${selectedAgency?.agencyName || 'Agency'}`}
        maxWidth="max-w-lg"
        footer={
          <>
            <Button
              variant="secondary"
              onClick={() => setIsAddUserOpen(false)}
              disabled={addUserMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleAddUserSubmit}
              loading={addUserMutation.isPending}
            >
              Create Staff Login
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

      {/* Modal: Manage Agency Users & Passwords */}
      <Modal
        isOpen={isManageUsersOpen}
        onClose={() => setIsManageUsersOpen(false)}
        title={`Staff Logins & Credentials: ${selectedAgency?.agencyName || ''}`}
        maxWidth="max-w-2xl"
        footer={
          <Button variant="secondary" onClick={() => setIsManageUsersOpen(false)}>
            Close
          </Button>
        }
      >
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-xs text-slate-500">
              Active operational accounts assigned to this printing agency partner.
            </p>
            <Button
              size="sm"
              variant="outline"
              onClick={() => {
                setIsManageUsersOpen(false);
                setIsAddUserOpen(true);
              }}
            >
              <UserPlus className="h-3.5 w-3.5 mr-1" />
              Add User
            </Button>
          </div>

          {resetSuccess && (
            <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-lg flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
              <span>{resetSuccess}</span>
            </div>
          )}

          {resetError && (
            <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-lg flex items-center gap-2">
              <AlertCircle className="h-4 w-4 shrink-0 text-rose-600" />
              <span>{resetError}</span>
            </div>
          )}

          {isUsersLoading ? (
            <div className="p-6 text-center text-xs text-slate-500">Loading user accounts...</div>
          ) : agencyUsers.length === 0 ? (
            <div className="p-6 text-center bg-slate-50 border border-dashed border-slate-200 rounded-lg">
              <Building2 className="h-6 w-6 text-slate-400 mx-auto mb-1" />
              <p className="text-xs text-slate-600">No login accounts provisioned yet for this agency.</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100 border border-slate-200 rounded-lg overflow-hidden bg-white">
              {agencyUsers.map((u) => (
                <div key={u.id} className="p-4 space-y-3">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-xs text-slate-900">{u.fullName}</span>
                        <Badge variant="warning" size="sm">{u.designation || 'Staff'}</Badge>
                        <Badge variant={u.status === 'ACTIVE' ? 'success' : 'neutral'} size="sm">
                          {u.status}
                        </Badge>
                      </div>
                      <div className="text-[11px] text-slate-500 font-mono mt-0.5">{u.email}</div>
                    </div>
                    <div>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          setActiveResetUserId(activeResetUserId === u.id ? null : u.id);
                          setNewPassword('');
                          setResetError('');
                        }}
                      >
                        <Lock className="h-3 w-3 mr-1" />
                        {activeResetUserId === u.id ? 'Cancel' : 'Reset Password'}
                      </Button>
                    </div>
                  </div>

                  {activeResetUserId === u.id && (
                    <div className="pt-2 border-t border-slate-100 flex items-center gap-2">
                      <input
                        type="password"
                        placeholder="Enter new password (min 8 chars)"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        className="flex-1 px-3 py-1.5 border border-slate-300 rounded text-xs focus:ring-1 focus:ring-amber-500"
                      />
                      <Button
                        size="sm"
                        variant="primary"
                        onClick={() => handleResetPassword(u.id)}
                        loading={resetLoading}
                      >
                        Save Password
                      </Button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}
