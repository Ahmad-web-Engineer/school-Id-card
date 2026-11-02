import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import {
  DEFAULT_FRONT_DESIGN,
  DEFAULT_BACK_DESIGN,
  CARD_STANDARDS,
} from './templatesConstants';
import { AlertCircle } from 'lucide-react';

export default function CreateTemplateModal({ isOpen, onClose, onCreated }) {
  const queryClient = useQueryClient();

  const [form, setForm] = useState({
    templateName: '',
    templateCategory: 'STUDENT',
    schoolId: '',
    preset: 'CR80_LANDSCAPE',
  });
  const [formError, setFormError] = useState('');

  // Fetch registered schools for the schoolId dropdown option
  const { data: schoolsData } = useQuery({
    queryKey: ['admin-schools-dropdown'],
    queryFn: () => api.get('/admin/schools', { params: { page: 0, size: 100 } }),
    enabled: isOpen,
  });

  const schools = schoolsData?.data || [];

  const createMutation = useMutation({
    mutationFn: (payload) => api.post('/admin/templates', payload),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['admin-templates'] });
      onClose();
      if (onCreated && res?.data) {
        onCreated(res.data);
      }
    },
    onError: (err) => {
      setFormError(err.message || 'Failed to create template.');
    },
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    setFormError('');

    if (!form.templateName.trim()) {
      setFormError('Template name is required.');
      return;
    }

    const standard = CARD_STANDARDS[form.preset] || CARD_STANDARDS.CR80_LANDSCAPE;

    const payload = {
      templateName: form.templateName.trim(),
      templateCategory: form.templateCategory,
      schoolId: form.schoolId ? form.schoolId : null,
      cardWidthMm: standard.widthMm,
      cardHeightMm: standard.heightMm,
      frontDesignJson: JSON.stringify(DEFAULT_FRONT_DESIGN),
      backDesignJson: JSON.stringify(DEFAULT_BACK_DESIGN),
    };

    createMutation.mutate(payload);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Create New ID Card Template"
      maxWidth="max-w-lg"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={createMutation.isPending}>
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            loading={createMutation.isPending}
          >
            Create & Open Designer
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

        <Input
          label="Template Name"
          name="templateName"
          required
          placeholder="e.g., Standard Horizontal Student Card"
          value={form.templateName}
          onChange={(e) => setForm({ ...form, templateName: e.target.value })}
        />

        <Select
          label="Template Category"
          name="templateCategory"
          value={form.templateCategory}
          onChange={(e) => setForm({ ...form, templateCategory: e.target.value })}
          options={[
            { value: 'STUDENT', label: 'Student ID Card' },
            { value: 'STAFF', label: 'Staff / Faculty Card' },
            { value: 'VISITOR', label: 'Visitor Pass' },
          ]}
        />

        <Select
          label="Card Format & Dimensions"
          name="preset"
          value={form.preset}
          onChange={(e) => setForm({ ...form, preset: e.target.value })}
          options={[
            {
              value: 'CR80_LANDSCAPE',
              label: 'CR80 Landscape (85.6 × 54.0 mm, 3mm Bleed)',
            },
            {
              value: 'CR80_PORTRAIT',
              label: 'CR80 Portrait (54.0 × 85.6 mm, 3mm Bleed)',
            },
          ]}
        />

        <Select
          label="Scope / Target School"
          name="schoolId"
          value={form.schoolId}
          onChange={(e) => setForm({ ...form, schoolId: e.target.value })}
          options={[
            { value: '', label: 'Global Platform Template (Available for all schools)' },
            ...schools.map((s) => ({
              value: s.id,
              label: `${s.schoolName} (${s.schoolCode || 'School'})`,
            })),
          ]}
          hint="Global templates can be assigned to any school during academic sessions."
        />
      </form>
    </Modal>
  );
}
