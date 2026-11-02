import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import StudentPhotoAvatar from './StudentPhotoAvatar';
import SinglePhotoUpload from './SinglePhotoUpload';
import { Plus, Search, Edit2, Trash2, Check, AlertCircle, RefreshCw } from 'lucide-react';

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
  photoFileId: null,
  status: 'ACTIVE',
};

export default function StudentRosterTab() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [selectedClass, setSelectedClass] = useState('');
  const [selectedSection, setSelectedSection] = useState('');

  // Modal & Form State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingStudentId, setEditingStudentId] = useState(null);
  const [formState, setFormState] = useState(initialStudentForm);
  const [selectedPhotoFile, setSelectedPhotoFile] = useState(null);
  const [formError, setFormError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Fetch paginated student roster
  const { data, isLoading, error, refetch, isFetching } = useQuery({
    queryKey: ['school-students', page, search, selectedClass, selectedSection],
    queryFn: () =>
      api.get('/school/students', {
        params: {
          page,
          size: 10,
          search: search.trim() || undefined,
          className: selectedClass || undefined,
          section: selectedSection || undefined,
        },
      }),
  });

  // Deactivate Mutation
  const deactivateMutation = useMutation({
    mutationFn: (id) => api.delete(`/school/students/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['school-students'] });
      setSuccessMsg('Student record deactivated successfully.');
      setTimeout(() => setSuccessMsg(''), 4000);
    },
    onError: (err) => {
      setFormError(err.message || 'Failed to deactivate student.');
    },
  });

  const handleOpenCreateModal = () => {
    setEditingStudentId(null);
    setFormState(initialStudentForm);
    setSelectedPhotoFile(null);
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
      photoFileId: student.photoFileId || null,
      status: student.status || 'ACTIVE',
    });
    setSelectedPhotoFile(null);
    setFormError('');
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');

    if (!formState.admissionNumber.trim() || !formState.studentName.trim() || !formState.className.trim()) {
      setFormError('Admission Number, Student Name, and Class Name are required.');
      return;
    }

    setIsSubmitting(true);
    try {
      const studentPayload = {
        admissionNumber: formState.admissionNumber.trim(),
        studentName: formState.studentName.trim(),
        fatherName: formState.fatherName?.trim() || null,
        motherName: formState.motherName?.trim() || null,
        dateOfBirth: formState.dateOfBirth || null,
        gender: formState.gender,
        bloodGroup: formState.bloodGroup || null,
        className: formState.className.trim(),
        section: formState.section?.trim() || null,
        rollNumber: formState.rollNumber?.trim() || null,
        address: formState.address?.trim() || null,
        mobile: formState.mobile?.trim() || null,
        emergencyContact: formState.emergencyContact?.trim() || null,
        status: formState.status || 'ACTIVE',
      };

      let targetStudentId = editingStudentId;

      if (editingStudentId) {
        // Update basic record
        await api.put(`/school/students/${editingStudentId}`, studentPayload);
      } else {
        // Create basic record
        const createRes = await api.post('/school/students', studentPayload);
        targetStudentId = createRes?.data?.id || createRes?.id;
      }

      // If a new photo file was chosen, upload it for the student
      if (selectedPhotoFile && targetStudentId) {
        const photoFormData = new FormData();
        photoFormData.append('file', selectedPhotoFile);
        await api.put(`/school/students/${targetStudentId}/photo`, photoFormData, {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        });
      }

      queryClient.invalidateQueries({ queryKey: ['school-students'] });
      setIsModalOpen(false);
      setSuccessMsg(
        editingStudentId
          ? 'Student record updated successfully!'
          : 'New student added successfully!'
      );
      setTimeout(() => setSuccessMsg(''), 4000);
      setFormState(initialStudentForm);
      setSelectedPhotoFile(null);
    } catch (err) {
      setFormError(err.message || 'Failed to save student record.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const columns = [
    {
      key: 'student',
      label: 'Student',
      render: (_, row) => (
        <div className="flex items-center gap-3">
          <StudentPhotoAvatar
            photoFileId={row.photoFileId}
            name={row.studentName}
            size="md"
          />
          <div className="min-w-0">
            <div className="font-semibold text-slate-900 truncate">
              {row.studentName || '—'}
            </div>
            <div className="text-[11px] text-slate-500 truncate">
              {row.fatherName ? `Father: ${row.fatherName}` : 'Parent not listed'}
            </div>
          </div>
        </div>
      ),
    },
    {
      key: 'admissionNumber',
      label: 'Admission No',
      cellClassName: 'font-mono text-xs font-bold text-indigo-700',
    },
    {
      key: 'className',
      label: 'Class & Sec',
      render: (_, r) => (
        <span className="font-medium text-slate-800">
          {r.className || '—'}{r.section ? ` - ${r.section}` : ''}
        </span>
      ),
    },
    {
      key: 'rollNumber',
      label: 'Roll No',
      render: (val) => val || '—',
    },
    {
      key: 'bloodGroup',
      label: 'Blood Group',
      render: (val) => (val ? <Badge variant="neutral" size="sm">{val}</Badge> : '—'),
    },
    {
      key: 'mobile',
      label: 'Contact',
      render: (val) => (val ? <span className="font-mono text-xs">{val}</span> : '—'),
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
            onClick={() => handleOpenEditModal(row)}
            title="Edit student"
            aria-label={`Edit ${row.studentName}`}
          >
            <Edit2 className="h-3.5 w-3.5" />
          </Button>
          <Button
            size="sm"
            variant="outline"
            className="text-rose-600 hover:text-rose-700 hover:bg-rose-50"
            onClick={() => {
              if (window.confirm(`Are you sure you want to deactivate ${row.studentName}?`)) {
                deactivateMutation.mutate(row.id);
              }
            }}
            title="Deactivate student"
            aria-label={`Deactivate ${row.studentName}`}
          >
            <Trash2 className="h-3.5 w-3.5" />
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Action Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Registered Students</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            View, search, edit student details, and assign individual student portrait photos.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            disabled={isFetching}
            title="Refresh student list"
          >
            <RefreshCw className={`h-3.5 w-3.5 mr-1.5 ${isFetching ? 'animate-spin' : ''}`} />
            Refresh
          </Button>
          <Button onClick={handleOpenCreateModal} size="sm">
            <Plus className="h-4 w-4 mr-1.5" />
            Add Student
          </Button>
        </div>
      </div>

      {successMsg && (
        <div className="p-3.5 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2">
          <Check className="h-4 w-4 text-emerald-600 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Search & Filter Bar */}
      <Card bodyClassName="p-4">
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
          <div className="sm:col-span-2 relative">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search by student name, admission number, roll..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="w-full pl-9 pr-4 py-2 text-xs border border-slate-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <Select
            placeholder="All Classes"
            options={CLASS_OPTIONS}
            value={selectedClass}
            onChange={(e) => {
              setSelectedClass(e.target.value);
              setPage(0);
            }}
          />
          <Select
            placeholder="All Sections"
            options={SECTION_OPTIONS}
            value={selectedSection}
            onChange={(e) => {
              setSelectedSection(e.target.value);
              setPage(0);
            }}
          />
        </div>
      </Card>

      {/* Responsive Students Table (Auto-stacks to card view on mobile < 768px) */}
      <ResponsiveTable
        columns={columns}
        data={data?.data || []}
        loading={isLoading}
        error={error}
        onRetry={refetch}
        emptyTitle="No students found"
        emptyMessage={
          search || selectedClass || selectedSection
            ? 'No students match your current search or filter criteria.'
            : 'Click "Add Student" above, or use the "Import Students" tab to upload your roster from Excel or CSV.'
        }
      />

      {/* Pagination Controls */}
      {data?.pagination && data.pagination.totalPages > 1 && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-600 px-2">
          <span>
            Showing page {data.pagination.page + 1} of {data.pagination.totalPages} ({data.pagination.totalElements} total students)
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

      {/* Modal: Add / Edit Student */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingStudentId ? 'Edit Student Details' : 'Add New Student'}
        maxWidth="max-w-2xl"
        footer={
          <>
            <Button
              variant="outline"
              size="sm"
              disabled={isSubmitting}
              onClick={() => setIsModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              loading={isSubmitting}
              onClick={handleSubmit}
            >
              {editingStudentId ? 'Save Changes' : 'Create Student'}
            </Button>
          </>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {formError && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          {/* Student Portrait Photo Upload Section */}
          <SinglePhotoUpload
            currentPhotoFileId={formState.photoFileId}
            studentName={formState.studentName}
            selectedFile={selectedPhotoFile}
            onFileSelect={(file) => setSelectedPhotoFile(file)}
            onFileRemove={() => setSelectedPhotoFile(null)}
            disabled={isSubmitting}
          />

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <Input
              label="Admission Number"
              required
              placeholder="e.g. ADM-2026-001"
              value={formState.admissionNumber}
              onChange={(e) => setFormState({ ...formState, admissionNumber: e.target.value })}
            />
            <div className="sm:col-span-2">
              <Input
                label="Full Name"
                required
                placeholder="e.g. Aarav Sharma"
                value={formState.studentName}
                onChange={(e) => setFormState({ ...formState, studentName: e.target.value })}
              />
            </div>
            <Input
              label="Class Name"
              required
              placeholder="e.g. 10th"
              value={formState.className}
              onChange={(e) => setFormState({ ...formState, className: e.target.value })}
            />
            <Input
              label="Section"
              placeholder="e.g. A"
              value={formState.section}
              onChange={(e) => setFormState({ ...formState, section: e.target.value })}
            />
            <Input
              label="Roll Number"
              placeholder="e.g. 12"
              value={formState.rollNumber}
              onChange={(e) => setFormState({ ...formState, rollNumber: e.target.value })}
            />
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
              options={['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((b) => ({
                value: b,
                label: b,
              }))}
              value={formState.bloodGroup}
              onChange={(e) => setFormState({ ...formState, bloodGroup: e.target.value })}
            />
            <Input
              label="Father Name"
              placeholder="e.g. Rajesh Sharma"
              value={formState.fatherName}
              onChange={(e) => setFormState({ ...formState, fatherName: e.target.value })}
            />
            <Input
              label="Mother Name"
              placeholder="e.g. Pooja Sharma"
              value={formState.motherName}
              onChange={(e) => setFormState({ ...formState, motherName: e.target.value })}
            />
            <Input
              label="Mobile Phone"
              placeholder="9876543210"
              value={formState.mobile}
              onChange={(e) => setFormState({ ...formState, mobile: e.target.value })}
            />
            <Input
              label="Emergency Contact"
              placeholder="9876543219"
              value={formState.emergencyContact}
              onChange={(e) => setFormState({ ...formState, emergencyContact: e.target.value })}
            />
            <div className="sm:col-span-2">
              <Input
                label="Home Address"
                placeholder="Residential address"
                value={formState.address}
                onChange={(e) => setFormState({ ...formState, address: e.target.value })}
              />
            </div>
          </div>
        </form>
      </Modal>
    </div>
  );
}
