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
import { 
  Plus, 
  Search, 
  UserPlus, 
  Users, 
  School, 
  Check, 
  AlertCircle, 
  KeyRound, 
  Lock, 
  CheckCircle2, 
  RefreshCw,
  Upload,
  Image as ImageIcon
} from 'lucide-react';

export default function SchoolsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [isAddUserOpen, setIsAddUserOpen] = useState(false);
  const [isManageUsersOpen, setIsManageUsersOpen] = useState(false);
  const [isLogoModalOpen, setIsLogoModalOpen] = useState(false);
  const [selectedSchool, setSelectedSchool] = useState(null);

  // Logo state
  const [logoFile, setLogoFile] = useState(null);
  const [logoPreviewUrl, setLogoPreviewUrl] = useState('');
  const [logoError, setLogoError] = useState('');
  const [logoLoading, setLogoLoading] = useState(false);

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

  // Password reset state
  const [activeResetUserId, setActiveResetUserId] = useState(null);
  const [newPassword, setNewPassword] = useState('');
  const [resetError, setResetError] = useState('');
  const [resetSuccess, setResetSuccess] = useState('');
  const [resetLoading, setResetLoading] = useState(false);

  // Fetch paginated schools
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['admin-schools', page, search],
    queryFn: () =>
      api.get('/admin/schools', {
        params: { page, size: 10, search: search.trim() || undefined },
      }),
  });

  // Fetch school users when Manage Users modal is open
  const { 
    data: schoolUsersData, 
    isLoading: isUsersLoading, 
    refetch: refetchUsers 
  } = useQuery({
    queryKey: ['admin-school-users', selectedSchool?.id],
    queryFn: () => api.get(`/admin/schools/${selectedSchool.id}/users`),
    enabled: isManageUsersOpen && !!selectedSchool,
  });

  const schoolUsers = schoolUsersData?.data || [];

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
      queryClient.invalidateQueries({ queryKey: ['admin-school-users', selectedSchool?.id] });
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

  const handleLogoUploadSubmit = async (e) => {
    e.preventDefault();
    if (!logoFile || !selectedSchool) {
      setLogoError('Please choose an image file to upload.');
      return;
    }

    setLogoLoading(true);
    setLogoError('');

    try {
      const formData = new FormData();
      formData.append('file', logoFile);

      await api.post(`/admin/schools/${selectedSchool.id}/logo`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      queryClient.invalidateQueries({ queryKey: ['admin-schools'] });
      setIsLogoModalOpen(false);
      setSuccessMsg(`Official logo updated successfully for ${selectedSchool.schoolName}!`);
      setTimeout(() => setSuccessMsg(''), 4000);
      setLogoFile(null);
      setLogoPreviewUrl('');
    } catch (err) {
      setLogoError(err.message || 'Failed to upload school logo.');
    } finally {
      setLogoLoading(false);
    }
  };

  const columns = [
    { key: 'schoolCode', label: 'Code', cellClassName: 'font-mono text-xs font-bold text-indigo-700' },
    {
      key: 'schoolName',
      label: 'School Institution',
      render: (val, row) => (
        <div className="flex items-center gap-2.5">
          <div className="h-8 w-8 rounded-md border border-slate-200 bg-white flex items-center justify-center overflow-hidden shrink-0 shadow-2xs">
            {row.logoFileId ? (
              <img
                src={`/api/v1/files/${row.logoFileId}`}
                alt={val}
                className="h-full w-full object-contain p-0.5"
                onError={(e) => {
                  e.currentTarget.style.display = 'none';
                }}
              />
            ) : (
              <span className="text-xs font-bold text-slate-400 bg-slate-50 h-full w-full flex items-center justify-center">
                {val ? val.charAt(0).toUpperCase() : 'S'}
              </span>
            )}
          </div>
          <div>
            <div className="font-semibold text-slate-900">{val}</div>
            <div className="text-[11px] text-slate-500">{row.city || '—'}, {row.state || '—'}</div>
          </div>
        </div>
      ),
    },
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
            onClick={() => {
              setSelectedSchool(row);
              setLogoFile(null);
              setLogoPreviewUrl(row.logoFileId ? `/api/v1/files/${row.logoFileId}` : '');
              setLogoError('');
              setIsLogoModalOpen(true);
            }}
            title="Upload or Change School Logo"
          >
            <Upload className="h-3.5 w-3.5 mr-1" />
            Logo
          </Button>
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
              setResetError('');
              setResetSuccess('');
              setActiveResetUserId(null);
              setIsManageUsersOpen(true);
            }}
            title="Manage School User Logins and Passwords"
          >
            <KeyRound className="h-3.5 w-3.5 mr-1" />
            Logins
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setSelectedSchool(row);
              setFormError('');
              setIsAddUserOpen(true);
            }}
            title="Provision Additional School User"
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
              placeholder="Search by school name, unique code, or city..."
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

      {/* Schools Table */}
      <Card>
        <ResponsiveTable
          columns={columns}
          data={data?.data || []}
          isLoading={isLoading}
          keyField="id"
          emptyMessage="No schools found matching your search criteria."
          pagination={{
            page: data?.pagination?.page || 0,
            totalPages: data?.pagination?.totalPages || 0,
            totalElements: data?.pagination?.totalElements || 0,
            onPageChange: (newPage) => setPage(newPage),
          }}
        />
      </Card>

      {/* Modal: Register School */}
      <Modal
        isOpen={isRegisterOpen}
        onClose={() => setIsRegisterOpen(false)}
        title="Register New School Institution"
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
              Register Institution
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
              label="School Code (Alphanumeric)"
              required
              placeholder="e.g. GWS-001"
              value={schoolForm.schoolCode}
              onChange={(e) => setSchoolForm({ ...schoolForm, schoolCode: e.target.value })}
            />
          </div>
          <Input
            label="Address"
            required
            placeholder="14 Heritage Boulevard, Indiranagar"
            value={schoolForm.address}
            onChange={(e) => setSchoolForm({ ...schoolForm, address: e.target.value })}
          />
          <div className="grid grid-cols-3 gap-3">
            <Input
              label="City"
              required
              placeholder="Bengaluru"
              value={schoolForm.city}
              onChange={(e) => setSchoolForm({ ...schoolForm, city: e.target.value })}
            />
            <Input
              label="State"
              required
              placeholder="Karnataka"
              value={schoolForm.state}
              onChange={(e) => setSchoolForm({ ...schoolForm, state: e.target.value })}
            />
            <Input
              label="Pincode"
              required
              placeholder="560038"
              value={schoolForm.pincode}
              onChange={(e) => setSchoolForm({ ...schoolForm, pincode: e.target.value })}
            />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input
              label="Official Email"
              type="email"
              required
              placeholder="admin@greenwood.edu"
              value={schoolForm.email}
              onChange={(e) => setSchoolForm({ ...schoolForm, email: e.target.value })}
            />
            <Input
              label="Contact Mobile"
              required
              placeholder="9876543210"
              value={schoolForm.mobile}
              onChange={(e) => setSchoolForm({ ...schoolForm, mobile: e.target.value })}
            />
          </div>
        </form>
      </Modal>

      {/* Modal: Add User */}
      <Modal
        isOpen={isAddUserOpen}
        onClose={() => setIsAddUserOpen(false)}
        title={`Add Administrator for ${selectedSchool?.schoolName || 'School'}`}
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
              Create User Login
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

      {/* Modal: Manage School Users & Passwords */}
      <Modal
        isOpen={isManageUsersOpen}
        onClose={() => setIsManageUsersOpen(false)}
        title={`User Logins & Credentials: ${selectedSchool?.schoolName || ''}`}
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
              Active administrative logins configured for this institution. You can inspect roles or reset credentials.
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
          ) : schoolUsers.length === 0 ? (
            <div className="p-6 text-center bg-slate-50 border border-dashed border-slate-200 rounded-lg">
              <Users className="h-6 w-6 text-slate-400 mx-auto mb-1" />
              <p className="text-xs text-slate-600">No login accounts provisioned yet for this school.</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100 border border-slate-200 rounded-lg overflow-hidden bg-white">
              {schoolUsers.map((u) => (
                <div key={u.id} className="p-4 space-y-3">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-xs text-slate-900">{u.fullName}</span>
                        <Badge variant="brand" size="sm">{u.designation || 'Staff'}</Badge>
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
                        className="flex-1 px-3 py-1.5 border border-slate-300 rounded text-xs focus:ring-1 focus:ring-indigo-500"
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

      {/* Modal: Upload School Official Logo */}
      <Modal
        isOpen={isLogoModalOpen}
        onClose={() => setIsLogoModalOpen(false)}
        title={`Official Logo: ${selectedSchool?.schoolName || 'School'}`}
        maxWidth="max-w-md"
        footer={
          <>
            <Button
              variant="secondary"
              onClick={() => setIsLogoModalOpen(false)}
              disabled={logoLoading}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleLogoUploadSubmit}
              loading={logoLoading}
              disabled={!logoFile || logoLoading}
            >
              Upload Official Logo
            </Button>
          </>
        }
      >
        <form onSubmit={handleLogoUploadSubmit} className="space-y-4">
          {logoError && (
            <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-lg flex items-center gap-2">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{logoError}</span>
            </div>
          )}

          <div className="text-xs text-slate-500">
            Upload the official emblem or crest for <strong>{selectedSchool?.schoolName}</strong> ({selectedSchool?.schoolCode}). High-resolution PNG or JPG with transparent or light background is recommended.
          </div>

          <div className="p-4 border border-slate-200 rounded-lg bg-slate-50 flex flex-col items-center justify-center space-y-3">
            <div className="h-28 w-28 bg-white border border-dashed border-slate-300 rounded-lg flex items-center justify-center p-2 overflow-hidden shadow-2xs">
              {logoPreviewUrl ? (
                <img src={logoPreviewUrl} alt="Logo preview" className="h-full w-full object-contain" />
              ) : (
                <div className="text-center text-slate-400">
                  <ImageIcon className="h-8 w-8 mx-auto mb-1 text-slate-300" />
                  <span className="text-[10px]">No logo set</span>
                </div>
              )}
            </div>

            <label className="cursor-pointer inline-flex items-center gap-1.5 px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-semibold rounded border border-indigo-200 transition-colors">
              <Upload className="h-3.5 w-3.5" />
              <span>{logoFile ? 'Change File' : 'Choose Logo File'}</span>
              <input
                type="file"
                accept="image/*"
                onChange={(e) => {
                  const f = e.target.files?.[0];
                  if (f) {
                    setLogoFile(f);
                    setLogoPreviewUrl(URL.createObjectURL(f));
                    setLogoError('');
                  }
                }}
                disabled={logoLoading}
                className="hidden"
              />
            </label>
            {logoFile && (
              <span className="text-[11px] text-slate-500 truncate max-w-xs">{logoFile.name} ({(logoFile.size / 1024).toFixed(1)} KB)</span>
            )}
          </div>
        </form>
      </Modal>
    </div>
  );
}
