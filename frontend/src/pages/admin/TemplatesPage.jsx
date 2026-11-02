import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Select from '../../components/common/Select';
import Badge from '../../components/common/Badge';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import CardDesignerModal from '../../features/templates/CardDesignerModal';
import CreateTemplateModal from '../../features/templates/CreateTemplateModal';
import AssignTemplateModal from '../../features/templates/AssignTemplateModal';
import {
  Palette,
  Plus,
  Search,
  SlidersHorizontal,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Building,
  Sparkles,
  Layers,
} from 'lucide-react';

export default function TemplatesPage() {
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Modal states
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [designerTemplate, setDesignerTemplate] = useState(null); // { id, templateName }
  const [assignTemplate, setAssignTemplate] = useState(null);

  // Fetch templates paginated
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['admin-templates', page, search, categoryFilter, statusFilter],
    queryFn: () =>
      api.get('/admin/templates', {
        params: {
          page,
          size: pageSize,
          search: search.trim() || undefined,
          category: categoryFilter !== 'ALL' ? categoryFilter : undefined,
          status: statusFilter !== 'ALL' ? statusFilter : undefined,
        },
      }),
  });

  const templatesList = data?.data || [];
  const pagination = data?.pagination || {
    page: 0,
    size: 10,
    totalElements: templatesList.length,
    totalPages: 1,
    last: true,
  };

  const getApprovalBadge = (status) => {
    switch (status) {
      case 'APPROVED':
        return <Badge variant="success">APPROVED</Badge>;
      case 'PENDING_APPROVAL':
        return <Badge variant="warning">PENDING APPROVAL</Badge>;
      case 'ARCHIVED':
        return <Badge variant="neutral">ARCHIVED</Badge>;
      case 'DRAFT':
      default:
        return <Badge variant="info">DRAFT</Badge>;
    }
  };

  // Columns definition for ResponsiveTable
  const columns = [
    {
      key: 'templateName',
      label: 'Template Name',
      render: (val, row) => (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-md bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600 shrink-0">
            <Palette className="w-4 h-4" />
          </div>
          <div>
            <div className="font-semibold text-slate-900">{val}</div>
            <div className="text-[11px] text-slate-400 font-mono">
              ID: {row.id.slice(0, 8)}...
            </div>
          </div>
        </div>
      ),
    },
    {
      key: 'templateCategory',
      label: 'Category',
      render: (val) => (
        <span className="inline-flex items-center text-xs font-medium text-slate-700 bg-slate-100 px-2.5 py-0.5 rounded-full">
          {val || 'STUDENT'}
        </span>
      ),
    },
    {
      key: 'schoolName',
      label: 'Target Scope',
      render: (val, row) => (
        <div className="text-xs">
          {row.schoolId ? (
            <span className="inline-flex items-center gap-1 text-slate-800 font-medium">
              <Building className="w-3.5 h-3.5 text-slate-400" />
              {val || 'Assigned School'}
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 text-indigo-700 font-medium">
              <Sparkles className="w-3.5 h-3.5 text-indigo-500" />
              Global Platform
            </span>
          )}
        </div>
      ),
    },
    {
      key: 'latestVersionNumber',
      label: 'Version',
      render: (val) => (
        <span className="font-mono text-xs font-semibold text-slate-700 bg-slate-100 px-2 py-0.5 rounded">
          v{val || 1}
        </span>
      ),
    },
    {
      key: 'activeVersionApprovalStatus',
      label: 'Approval Status',
      render: (val) => getApprovalBadge(val || 'DRAFT'),
    },
    {
      key: 'createdAt',
      label: 'Created Date',
      render: (val) => (
        <span className="text-xs text-slate-500">
          {val ? new Date(val).toLocaleDateString() : '—'}
        </span>
      ),
    },
    {
      key: 'actions',
      label: 'Actions',
      cellClassName: 'text-right',
      render: (_, row) => (
        <div className="flex items-center justify-end gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setAssignTemplate(row)}
            className="text-xs"
          >
            Assign School
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={() =>
              setDesignerTemplate({ id: row.id, templateName: row.templateName })
            }
            className="flex items-center gap-1.5 text-xs"
          >
            <Palette className="w-3.5 h-3.5" />
            <span>Open Designer</span>
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header with Title and Create Action */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            Card Template Designer
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1 max-w-2xl">
            Design, configure, and approve CR80 ID card layout templates with dynamic student
            data placeholders, front and back canvas editors, and PDFBox-ready print schemas.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => setIsCreateOpen(true)}
          className="flex items-center gap-2 self-start sm:self-auto shadow-sm"
        >
          <Plus className="w-4 h-4" />
          <span>Create New Template</span>
        </Button>
      </div>

      {/* Filter and Search Bar */}
      <Card className="p-4 bg-white border border-slate-200">
        <div className="grid grid-cols-1 sm:grid-cols-12 gap-3">
          <div className="sm:col-span-6 relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search template name..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="w-full pl-9 pr-3 py-2 text-xs sm:text-sm rounded-md border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 bg-white"
            />
          </div>

          <div className="sm:col-span-3">
            <select
              value={categoryFilter}
              onChange={(e) => {
                setCategoryFilter(e.target.value);
                setPage(0);
              }}
              className="w-full py-2 px-3 text-xs sm:text-sm rounded-md border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 bg-white text-slate-700"
            >
              <option value="ALL">All Categories</option>
              <option value="STUDENT">Student Cards</option>
              <option value="STAFF">Staff & Faculty</option>
              <option value="VISITOR">Visitor Passes</option>
            </select>
          </div>

          <div className="sm:col-span-3">
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="w-full py-2 px-3 text-xs sm:text-sm rounded-md border border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 bg-white text-slate-700"
            >
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Templates Responsive Table */}
      <ResponsiveTable
        columns={columns}
        data={templatesList}
        loading={isLoading}
        error={isError ? error : null}
        onRetry={refetch}
        emptyTitle="No card templates found"
        emptyMessage="Click 'Create New Template' above to start designing your first student card layout."
      />

      {/* Pagination Bar */}
      {pagination.totalPages > 1 && (
        <div className="flex items-center justify-between px-2 text-xs sm:text-sm text-slate-600">
          <span>
            Showing Page <strong>{pagination.page + 1}</strong> of{' '}
            <strong>{pagination.totalPages}</strong> ({pagination.totalElements} total templates)
          </span>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={pagination.page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="flex items-center gap-1"
            >
              <ChevronLeft className="w-4 h-4" />
              <span>Previous</span>
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={pagination.last || pagination.page >= pagination.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="flex items-center gap-1"
            >
              <span>Next</span>
              <ChevronRight className="w-4 h-4" />
            </Button>
          </div>
        </div>
      )}

      {/* Create New Template Modal */}
      <CreateTemplateModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onCreated={(newTemplate) => {
          setDesignerTemplate({
            id: newTemplate.id,
            templateName: newTemplate.templateName,
          });
        }}
      />

      {/* Assign Template to School Modal */}
      <AssignTemplateModal
        isOpen={!!assignTemplate}
        onClose={() => setAssignTemplate(null)}
        template={assignTemplate}
        onAssigned={() => setAssignTemplate(null)}
      />

      {/* Full Visual Card Designer Modal */}
      {designerTemplate && (
        <CardDesignerModal
          isOpen={!!designerTemplate}
          onClose={() => setDesignerTemplate(null)}
          templateId={designerTemplate.id}
          templateName={designerTemplate.templateName}
        />
      )}
    </div>
  );
}
