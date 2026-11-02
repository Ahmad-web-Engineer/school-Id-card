import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import { Plus, Search, UserPlus, Users, School, Check, AlertCircle } from 'lucide-react';

export default function SchoolsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [isAddUserOpen, setIsAddUserOpen] = useState(false);
  const [selectedSchool, setSelectedSchool] = useState(null);

  // Form states
  const [schoolForm, setSchoolForm] = useState({
    schoolName: '',
    schoolCode: '',
    address: '',
    city: '',
    state: '',
    pincode: '',
    email: '',
    mobile: '',
  });
  const [userForm, setUserForm] = useState({
    fullName: '',
    email: '',
    mobile: '',
    password: '',
    designation: 'Principal',
  });
  const [formError, setFormError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Fetch paginated schools
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['admin-schools', page, search],
    queryFn: () =>
      api.get('/admin/schools', {
        params: { page, size: 10, search: search.trim() || undefined },
      }),
  });

  // Mutation: Register School
  const registerMutation = useMutation({
    mutationFn: (newSchool) => api.post('/admin/schools', newSchool),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-schools'] });
      setIsRegisterOpen(false);
      setSuccessMsg('School registered successfully!');
      setTimeout(() => setSuccessMsg(''), 4000);
      setSchoolForm({
        schoolName: '',
        schoolCode: '',
        address: '',
        city: '',
        state: '',
        pincode: '',
        email: '',
        mobile: '',
      });
    },
    onError: (err) => {
      setFormError(err.message || 'Failed to register school.');
    },
  });

  // Mutation: Create School User
  const addUserMutation = useMutation({
    mutationFn: ({ schoolId, userData }) =>
      api.post(`/admin/schools/${schoolId}/users`, userData),
    onSuccess: () => {
      setIsAddUserOpen(false);
      setSuccessMsg(`User created successfully for ${selectedSchool?.schoolName}!`);
      setTimeout(() => setSuccessMsg(''), 4000);
      setUserForm({
        fullName: '',
        email: '',
        mobile: '',
        password: '',
        designation: 'Principal',
      });
    },
    onError: (err) => {
      setFormError(err.message || 'Failed to create school user.');
    },
  });

  const handleRegisterSubmit = (e) => {
    e.preventDefault();
    setFormError('');
    registerMutation.mutate(schoolForm);
  };

  const handleAddUserSubmit = (e) => {
    e.preventDefault();
    setFormError('');
    if (!selectedSchool) return;
    addUserMutation.mutate({ schoolId: selectedSchool.id, userData: userForm });
  };

  const columns = [
    { key: 'schoolCode', label: 'Code', cellClassName: 'font-mono text-xs font-bold text-indigo-700' },
    { key: 'schoolName', label: 'School Name', cellClassName: 'font-semibold text-slate-900' },
    { key: 'city', label: 'City / State', render: (val, row) => `${row.city || '—'}, ${row.state || '—'}` },
    { key: 'email', label: 'Contact Email' },
    { key: 'mobile', label: 'Phone' },
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
            onClick={() => navigate(`/admin/schools/${row.id}/students`)}
            title="View School Students"
          >
            <Users className="h-3.5 w-3.5 mr-1" />
            Students
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setSelectedSchool(row);
              setFormError('');
              setIsAddUserOpen(true);
            }}
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
          <h1 className="text-xl font-bold text-slate-900">Registered Schools</h1>
          <p className="text-xs text-slate-500 mt-1">
            Manage participating educational institutions, onboard new schools, and provision administrative logins.
          </p>
        </div>
        <Button
          onClick={() => {
            setFormError('');
            setIsRegisterOpen(true);
          }}
        >
          <Plus className="h-4 w-4 mr-1.5" />
          Register School
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
              placeholder="Search schools by name, code, or city..."
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

      {/* Responsive Schools Table (Card Stack on Mobile) */}
      <ResponsiveTable
        columns={columns}
        data={data?.data || []}
        loading={isLoading}
        error={error}
        onRetry={refetch}
        emptyTitle="No schools found"
        emptyMessage={search ? 'No schools match your search query.' : 'Click "Register School" above to onboard your first school.'}
      />

      {/* Pagination Controls */}
      {data?.pagination && data.pagination.totalPages > 1 && (
        <div className="flex items-center justify-between text-xs text-slate-600 px-2">
          <span>
            Showing page {data.pagination.page + 1} of {data.pagination.totalPages} ({data.pagination.totalElements} schools)
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

      {/* Modal: Register School */}
      <Modal
        isOpen={isRegisterOpen}
        onClose={() => setIsRegisterOpen(false)}
        title="Register New School"
        footer={
          <>
            <Button variant="outline" size="sm" onClick={() => setIsRegisterOpen(false)}>
              Cancel
            </Button>
            <Button size="sm" loading={registerMutation.isPending} onClick={handleRegisterSubmit}>
              Register School
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
              label="School Name"
              required
              placeholder="e.g. Greenwood High School"
              value={schoolForm.schoolName}
              onChange={(e) => setSchoolForm({ ...schoolForm, schoolName: e.target.value })}
            />
            <Input
              label="School Code (Unique)"
              required
              placeholder="e.g. GHS-001"
              value={schoolForm.schoolCode}
              onChange={(e) => setSchoolForm({ ...schoolForm, schoolCode: e.target.value })}
            />
            <Input
              label="Contact Email"
              type="email"
              required
              placeholder="admin@school.edu"
              value={schoolForm.email}
              onChange={(e) => setSchoolForm({ ...schoolForm, email: e.target.value })}
            />
            <Input
              label="Phone Number"
              placeholder="9876543210"
              value={schoolForm.mobile}
              onChange={(e) => setSchoolForm({ ...schoolForm, mobile: e.target.value })}
            />
            <div className="sm:col-span-2">
              <Input
                label="Street Address"
                placeholder="123 Education Boulevard"
                value={schoolForm.address}
                onChange={(e) => setSchoolForm({ ...schoolForm, address: e.target.value })}
              />
            </div>
            <Input
              label="City"
              placeholder="e.g. Mumbai"
              value={schoolForm.city}
              onChange={(e) => setSchoolForm({ ...schoolForm, city: e.target.value })}
            />
            <Input
              label="State"
              placeholder="e.g. Maharashtra"
              value={schoolForm.state}
              onChange={(e) => setSchoolForm({ ...schoolForm, state: e.target.value })}
            />
            <Input
              label="Pincode"
              placeholder="400001"
              value={schoolForm.pincode}
              onChange={(e) => setSchoolForm({ ...schoolForm, pincode: e.target.value })}
            />
          </div>
        </form>
      </Modal>

      {/* Modal: Add School User */}
      <Modal
        isOpen={isAddUserOpen}
        onClose={() => setIsAddUserOpen(false)}
        title={`Provision Login for ${selectedSchool?.schoolName || 'School'}`}
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
              placeholder="e.g. Dr. Ramesh Gupta"
              value={userForm.fullName}
              onChange={(e) => setUserForm({ ...userForm, fullName: e.target.value })}
            />
            <Input
              label="Login Email"
              type="email"
              required
              placeholder="principal@school.edu"
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
              placeholder="e.g. Principal / IT Coordinator"
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
