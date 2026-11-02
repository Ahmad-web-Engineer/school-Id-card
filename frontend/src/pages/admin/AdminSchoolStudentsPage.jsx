import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import { Plus, Search, Edit2, Trash2, Check, AlertCircle, Users, ArrowLeft } from 'lucide-react';

const CLASS_OPTIONS = [
  { value: '', label: 'All Classes' },
  ...['Nursery', 'LKG', 'UKG', '1st', '2nd', '3rd', '4th', '5th', '6th', '7th', '8th', '9th', '10th', '11th', '12th'].map(
    (c) => ({ value: c, label: `Class ${c}` })
  ),
];

const SECTION_OPTIONS = [
  { value: '', label: 'All Sections' },
  ...['A', 'B', 'C', 'D', 'E'].map((s) => ({ value: s, label: `Section ${s}` })),
];

const initialStudentForm = {
  admissionNumber: '',
  studentName: '',
  fatherName: '',
  motherName: '',
  dateOfBirth: '',
  gender: 'MALE',
  bloodGroup: 'B+',
  className: '10th',
  section: 'A',
  rollNumber: '',
  address: '',
  mobile: '',
  emergencyContact: '',
  status: 'ACTIVE',
};

export default function AdminSchoolStudentsPage() {
  const { schoolId: paramSchoolId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Selected school state
  const [selectedSchoolId, setSelectedSchoolId] = useState(paramSchoolId || '');
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [selectedClass, setSelectedClass] = useState('');
  const [selectedSection, setSelectedSection] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingStudentId, setEditingStudentId] = useState(null);
  const [formState, setFormState] = useState(initialStudentForm);
  const [formError, setFormError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Fetch list of schools for dropdown selector
  const { data: schoolsData } = useQuery({
    queryKey: ['admin-schools-list'],
    queryFn: () => api.get('/admin/schools', { params: { page: 0, size: 100 } }),
  });

  const schools = schoolsData?.data || [];
  const activeSchoolId = selectedSchoolId || (schools.length > 0 ? schools[0].id : null);
  const activeSchool = schools.find((s) => s.id === activeSchoolId);

  // Fetch students for active school
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['admin-school-students', activeSchoolId, page, search, selectedClass, selectedSection],
    queryFn: () =>
      api.get(`/admin/schools/${activeSchoolId}/students`, {
        params: {
          page,
          size: 10,
          search: search.trim() || undefined,
          className: selectedClass || undefined,
          section: selectedSection || undefined,
        },
      }),
    enabled: !!activeSchoolId,
  });

  // Create Student Mutation
  const createMutation = useMutation({
    mutationFn: (newStudent) => api.post(`/admin/schools/${activeSchoolId}/students`, newStudent),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-school-students', activeSchoolId] });
      setIsModalOpen(false);
      setSuccessMsg('Student created successfully!');
      setTimeout(() => setSuccessMsg(''), 4000);
      setFormState(initialStudentForm);
    },
    onError: (err) => {
      setFormError(err.message || 'Failed to create student.');
    },
  });

  // Update Student Mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, updatedStudent }) =>
      api.put(`/admin/schools/${activeSchoolId}/students/${id}`, updatedStudent),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-school-students', activeSchoolId] });
      setIsModalOpen(false);
      setEditingStudentId(null);
      setSuccessMsg('Student updated successfully!');
      setTimeout(() => setSuccessMsg(''), 4000);
      setFormState(initialStudentForm);
    },
    onError: (err) => {
      setFormError(err.message || 'Failed to update student.');
    },
  });

  // Deactivate Mutation
  const deactivateMutation = useMutation({
    mutationFn: (id) => api.delete(`/admin/schools/${activeSchoolId}/students/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-school-students', activeSchoolId] });
      setSuccessMsg('Student deactivated successfully.');
      setTimeout(() => setSuccessMsg(''), 4000);
    },
    onError: (err) => {
      setSuccessMsg('');
      alert(err.message || 'Failed to deactivate student.');
    },
  });

  const handleOpenCreateModal = () => {
    setEditingStudentId(null);
    setFormState(initialStudentForm);
    setFormError('');
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (student) => {
    setEditingStudentId(student.id);
    setFormState({
      admissionNumber: student.admissionNumber || '',
      studentName: student.studentName || '',
      fatherName: student.fatherName || '',
      motherName: student.motherName || '',
      dateOfBirth: student.dateOfBirth || '',
      gender: student.gender || 'MALE',
      bloodGroup: student.bloodGroup || 'B+',
      className: student.className || '10th',
      section: student.section || 'A',
      rollNumber: student.rollNumber || '',
      address: student.address || '',
      mobile: student.mobile || '',
      emergencyContact: student.emergencyContact || '',
      status: student.status || 'ACTIVE',
    });
    setFormError('');
    setIsModalOpen(true);
  };

  const handleFormSubmit = (e) => {
    e.preventDefault();
    setFormError('');

    if (!formState.admissionNumber.trim() || !formState.studentName.trim()) {
      setFormError('Admission Number and Student Name are mandatory.');
      return;
    }

    if (editingStudentId) {
      updateMutation.mutate({ id: editingStudentId, updatedStudent: formState });
    } else {
      createMutation.mutate(formState);
    }
  };

  const columns = [
    {
      key: 'admissionNumber',
      label: 'Adm No',
      cellClassName: 'font-mono text-xs font-bold text-indigo-700',
    },
    {
      key: 'studentName',
      label: 'Student Name',
      render: (_, row) => (
        <div>
          <div className="font-semibold text-slate-900">{row.studentName}</div>
          <div className="text-[11px] text-slate-500">
            {row.fatherName ? `F: ${row.fatherName}` : 'Parent details not set'}
          </div>
        </div>
      ),
    },
    {
      key: 'className',
      label: 'Class & Sec',
      render: (_, row) => (
        <span className="font-medium text-slate-800">
          {row.className || '—'} {row.section ? `(${row.section})` : ''}
          {row.rollNumber ? ` • Roll: ${row.rollNumber}` : ''}
        </span>
      ),
    },
    {
      key: 'gender',
      label: 'Gender / Blood',
      render: (_, row) => (
        <span className="text-xs text-slate-600">
          {row.gender || '—'} {row.bloodGroup ? `• ${row.bloodGroup}` : ''}
        </span>
      ),
    },
    {
      key: 'mobile',
      label: 'Contact',
      render: (_, row) => (
        <span className="text-xs text-slate-600 font-mono">
          {row.mobile || row.emergencyContact || '—'}
        </span>
      ),
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
            variant="ghost"
            onClick={() => handleOpenEditModal(row)}
            title="Edit Student"
          >
            <Edit2 className="h-3.5 w-3.5 text-slate-600 hover:text-indigo-600" />
          </Button>
          {row.status === 'ACTIVE' && (
            <Button
              size="sm"
              variant="ghost"
              onClick={() => {
                if (window.confirm(`Deactivate student "${row.studentName}"?`)) {
                  deactivateMutation.mutate(row.id);
                }
              }}
              title="Deactivate Student"
            >
              <Trash2 className="h-3.5 w-3.5 text-slate-400 hover:text-rose-600" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  const schoolSelectOptions = schools.map((s) => ({
    value: s.id,
    label: `${s.schoolName} (${s.schoolCode})`,
  }));

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            {paramSchoolId && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => navigate('/admin/schools')}
                className="p-1 -ml-1 text-slate-500"
              >
                <ArrowLeft className="h-4 w-4" />
              </Button>
            )}
            <h1 className="text-xl font-bold text-slate-900">Student Rosters</h1>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Super Admin student directory view and oversight across registered schools.
          </p>
        </div>

        {activeSchoolId && (
          <Button onClick={handleOpenCreateModal}>
            <Plus className="h-4 w-4 mr-1.5" />
            Add Student
          </Button>
        )}
      </div>

      {/* School Selector & Filters */}
      <Card bodyClassName="p-4 space-y-3">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          <div className="md:col-span-2">
            <Select
              label="Select Institution"
              options={schoolSelectOptions}
              value={activeSchoolId || ''}
              onChange={(e) => {
                setSelectedSchoolId(e.target.value);
                setPage(0);
              }}
            />
          </div>

          <Select
            label="Class"
            options={CLASS_OPTIONS}
            value={selectedClass}
            onChange={(e) => {
              setSelectedClass(e.target.value);
              setPage(0);
            }}
          />

          <Select
            label="Section"
            options={SECTION_OPTIONS}
            value={selectedSection}
            onChange={(e) => {
              setSelectedSection(e.target.value);
              setPage(0);
            }}
          />
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-3 pt-2 border-t border-slate-100">
          <div className="relative flex-1 w-full">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search students by admission number, name, or phone..."
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

      {successMsg && (
        <div className="p-3 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2">
          <Check className="h-4 w-4 text-emerald-600 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Responsive Students Table */}
      {!activeSchoolId ? (
        <Card bodyClassName="p-8 text-center text-slate-500 text-sm">
          No schools registered yet. Please onboard a school first.
        </Card>
      ) : (
        <ResponsiveTable
          columns={columns}
          data={data?.data || []}
          loading={isLoading}
          error={error}
          onRetry={refetch}
          emptyTitle="No students found"
          emptyMessage={
            search || selectedClass || selectedSection
              ? 'No students match your filter criteria.'
              : `No students enrolled yet for ${activeSchool?.schoolName || 'this school'}.`
          }
        />
      )}

      {/* Pagination Controls */}
      {data?.pagination && data.pagination.totalPages > 1 && (
        <div className="flex items-center justify-between text-xs text-slate-600 px-2">
          <span>
            Showing page {data.pagination.page + 1} of {data.pagination.totalPages} ({data.pagination.totalElements} students)
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

      {/* Modal: Create or Edit Student */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingStudentId ? 'Edit Student Record' : `Add Student to ${activeSchool?.schoolName || 'School'}`}
        size="lg"
        footer={
          <>
            <Button variant="outline" size="sm" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button
              size="sm"
              loading={createMutation.isPending || updateMutation.isPending}
              onClick={handleFormSubmit}
            >
              {editingStudentId ? 'Save Changes' : 'Enroll Student'}
            </Button>
          </>
        }
      >
        <form onSubmit={handleFormSubmit} className="space-y-4">
          {formError && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input
              label="Admission Number *"
              required
              placeholder="e.g. ADM-2026-001"
              value={formState.admissionNumber}
              disabled={!!editingStudentId}
              onChange={(e) => setFormState({ ...formState, admissionNumber: e.target.value })}
            />
            <Input
              label="Student Full Name *"
              required
              placeholder="Full name"
              value={formState.studentName}
              onChange={(e) => setFormState({ ...formState, studentName: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input
              label="Father's Name"
              placeholder="Father's name"
              value={formState.fatherName}
              onChange={(e) => setFormState({ ...formState, fatherName: e.target.value })}
            />
            <Input
              label="Mother's Name"
              placeholder="Mother's name"
              value={formState.motherName}
              onChange={(e) => setFormState({ ...formState, motherName: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <Input
              label="Date of Birth"
              type="date"
              value={formState.dateOfBirth}
              onChange={(e) => setFormState({ ...formState, dateOfBirth: e.target.value })}
            />
            <Select
              label="Gender"
              options={[
                { value: 'MALE', label: 'Male' },
                { value: 'FEMALE', label: 'Female' },
                { value: 'OTHER', label: 'Other' },
              ]}
              value={formState.gender}
              onChange={(e) => setFormState({ ...formState, gender: e.target.value })}
            />
            <Select
              label="Blood Group"
              options={['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((bg) => ({
                value: bg,
                label: bg,
              }))}
              value={formState.bloodGroup}
              onChange={(e) => setFormState({ ...formState, bloodGroup: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <Select
              label="Class *"
              options={CLASS_OPTIONS.filter((c) => c.value)}
              value={formState.className}
              onChange={(e) => setFormState({ ...formState, className: e.target.value })}
            />
            <Select
              label="Section *"
              options={SECTION_OPTIONS.filter((s) => s.value)}
              value={formState.section}
              onChange={(e) => setFormState({ ...formState, section: e.target.value })}
            />
            <Input
              label="Roll Number"
              placeholder="e.g. 15"
              value={formState.rollNumber}
              onChange={(e) => setFormState({ ...formState, rollNumber: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input
              label="Primary Mobile"
              placeholder="10-digit mobile"
              value={formState.mobile}
              onChange={(e) => setFormState({ ...formState, mobile: e.target.value })}
            />
            <Input
              label="Emergency Contact"
              placeholder="Emergency phone"
              value={formState.emergencyContact}
              onChange={(e) => setFormState({ ...formState, emergencyContact: e.target.value })}
            />
          </div>

          <Input
            label="Residential Address"
            placeholder="Street address, City, State"
            value={formState.address}
            onChange={(e) => setFormState({ ...formState, address: e.target.value })}
          />
        </form>
      </Modal>
    </div>
  );
}
