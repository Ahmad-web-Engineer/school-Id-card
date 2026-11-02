import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import { AlertCircle, CheckCircle2 } from 'lucide-react';

export default function AssignTemplateModal({
  isOpen,
  onClose,
  template,
  onAssigned,
}) {
  const queryClient = useQueryClient();

  const [schoolId, setSchoolId] = useState('');
  const [academicSession, setAcademicSession] = useState('2026-2027');
  const [formError, setFormError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Fetch registered schools
  const { data: schoolsData, isLoading: isLoadingSchools } = useQuery({
    queryKey: ['admin-schools-for-assign'],
    queryFn: () => api.get('/admin/schools', { params: { page: 0, size: 100 } }),
    enabled: isOpen,
  });

  const schools = schoolsData?.data || [];

  const assignMutation = useMutation({
    mutationFn: (payload) =>
      api.post(`/admin/templates/${template.id}/assign`, payload),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['admin-templates'] });
      setSuccessMessage('Template successfully assigned to school!');
      setTimeout(() => {
        setSuccessMessage('');
        onClose();
        if (onAssigned) onAssigned();
      }, 1500);
    },
    onError: (err) => {
      setFormError(err.message || 'Failed to assign template to school.');
    },
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    setFormError('');

    if (!schoolId) {
      setFormError('Please select a school to assign.');
      return;
    }

    if (!academicSession.trim()) {
      setFormError('Academic session is required (e.g. 2026-2027).');
      return;
    }

    assignMutation.mutate({
      schoolId,
      academicSession: academicSession.trim(),
      active: true,
    });
  };

  if (!template) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Assign Template to School"
      maxWidth="max-w-md"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={assignMutation.isPending}>
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            loading={assignMutation.isPending}
          >
            Confirm Assignment
          </Button>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {formError && (
          <div className="p-3 bg-rose-50 border border-rose-200 rounded-md text-xs text-rose-700 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{formError}</span>
          </div>
        )}

        {successMessage && (
          <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-md text-xs text-emerald-700 flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs">
          <span className="text-slate-500 block">Selected Template:</span>
          <strong className="text-slate-900 text-sm block mt-0.5">
            {template.templateName}
          </strong>
          <span className="text-slate-500 font-mono text-[11px] mt-1 inline-block">
            Category: {template.templateCategory || 'STUDENT'} • Latest Version: v{template.latestVersionNumber || 1}
          </span>
        </div>

        <Select
          label="Target School"
          name="schoolId"
          required
          value={schoolId}
          onChange={(e) => setSchoolId(e.target.value)}
          options={[
            { value: '', label: isLoadingSchools ? 'Loading schools...' : 'Select a School...' },
            ...schools.map((s) => ({
              value: s.id,
              label: `${s.schoolName} (${s.schoolCode || s.city || 'School'})`,
            })),
          ]}
        />

        <Input
          label="Academic Session"
          name="academicSession"
          required
          placeholder="e.g. 2026-2027"
          value={academicSession}
          onChange={(e) => setAcademicSession(e.target.value)}
          hint="Enables cards under this template for the specified academic year."
        />
      </form>
    </Modal>
  );
}
