import React, { useState } from 'react';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import ResponsiveTable from '../../components/common/ResponsiveTable';
import { UserCheck, Plus, Search, Mail, Phone, Shield, CheckCircle2 } from 'lucide-react';

export default function TeachersStaffPage() {
  const [search, setSearch] = useState('');
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');

  // Initial demo roster
  const [staffList, setStaffList] = useState([
    {
      id: 'st-001',
      employeeId: 'EMP-0101',
      fullName: 'Anita Deshmukh',
      designation: 'Senior Mathematics Faculty',
      department: 'Mathematics & Science',
      email: 'anita.d@school.edu',
      mobile: '+91 98765 11001',
      bloodGroup: 'O+',
      cardStatus: 'ISSUED',
    },
    {
      id: 'st-002',
      employeeId: 'EMP-0102',
      fullName: 'Vikramaditya Rao',
      designation: 'Head of English & Literature',
      department: 'Humanities & Languages',
      email: 'vikram.rao@school.edu',
      mobile: '+91 98765 11002',
      bloodGroup: 'A+',
      cardStatus: 'ISSUED',
    },
    {
      id: 'st-003',
      employeeId: 'EMP-0103',
      fullName: 'Sneha Kulkarni',
      designation: 'Physics Lab Director',
      department: 'Science & Laboratories',
      email: 'sneha.k@school.edu',
      mobile: '+91 98765 11003',
      bloodGroup: 'B+',
      cardStatus: 'PENDING_PRINT',
    },
    {
      id: 'st-004',
      employeeId: 'EMP-0104',
      fullName: 'Mohammed Irfan',
      designation: 'Physical Education Director',
      department: 'Sports & Athletics',
      email: 'irfan.m@school.edu',
      mobile: '+91 98765 11004',
      bloodGroup: 'AB+',
      cardStatus: 'ISSUED',
    },
  ]);

  const [form, setForm] = useState({
    employeeId: '',
    fullName: '',
    designation: 'Faculty Member',
    department: 'Academics',
    email: '',
    mobile: '',
    bloodGroup: 'O+',
  });

  const handleAddStaff = (e) => {
    e.preventDefault();
    if (!form.fullName || !form.employeeId) return;

    const newStaff = {
      id: `st-${Date.now()}`,
      ...form,
      cardStatus: 'PENDING_PRINT',
    };

    setStaffList([newStaff, ...staffList]);
    setIsAddOpen(false);
    setSuccessMsg(`Staff member ${form.fullName} added successfully to roster!`);
    setTimeout(() => setSuccessMsg(''), 4000);
    setForm({
      employeeId: '',
      fullName: '',
      designation: 'Faculty Member',
      department: 'Academics',
      email: '',
      mobile: '',
      bloodGroup: 'O+',
    });
  };

  const filteredStaff = staffList.filter((s) => {
    const q = search.toLowerCase();
    return (
      s.fullName.toLowerCase().includes(q) ||
      s.employeeId.toLowerCase().includes(q) ||
      s.designation.toLowerCase().includes(q) ||
      s.department.toLowerCase().includes(q)
    );
  });

  const columns = [
    {
      key: 'employeeId',
      label: 'Emp ID',
      cellClassName: 'font-mono text-xs font-bold text-indigo-700',
    },
    {
      key: 'fullName',
      label: 'Staff Name',
      cellClassName: 'font-semibold text-slate-900',
    },
    {
      key: 'designation',
      label: 'Designation & Department',
      render: (val, row) => (
        <div>
          <div className="text-xs font-medium text-slate-800">{val}</div>
          <div className="text-[11px] text-slate-500">{row.department}</div>
        </div>
      ),
    },
    {
      key: 'contact',
      label: 'Contact Details',
      render: (_, row) => (
        <div className="text-[11px] text-slate-600">
          <div>{row.email}</div>
          <div className="font-mono text-slate-500">{row.mobile}</div>
        </div>
      ),
    },
    {
      key: 'bloodGroup',
      label: 'Blood Group',
      render: (val) => (
        <span className="font-mono text-xs font-bold text-rose-700 bg-rose-50 px-2 py-0.5 rounded border border-rose-100">
          {val}
        </span>
      ),
    },
    {
      key: 'cardStatus',
      label: 'ID Card Status',
      render: (val) => (
        <Badge variant={val === 'ISSUED' ? 'success' : 'warning'} size="sm">
          {val === 'ISSUED' ? 'Card Active' : 'Pending Order'}
        </Badge>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Teachers & Staff Directory</h1>
          <p className="text-xs text-slate-500 mt-1">
            Faculty and administrative staff roster for institutional staff ID card issuance and campus access control.
          </p>
        </div>
        <Button onClick={() => setIsAddOpen(true)} className="flex items-center gap-1.5">
          <Plus className="h-4 w-4" />
          <span>Add Staff Member</span>
        </Button>
      </div>

      {successMsg && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-lg flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Filter Bar */}
      <Card>
        <div className="relative">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search staff by name, employee ID, department, or title..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-4 py-2 border border-slate-300 rounded-lg text-xs focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500"
          />
        </div>
      </Card>

      {/* Staff Roster Table */}
      <Card>
        <ResponsiveTable
          columns={columns}
          data={filteredStaff}
          keyField="id"
          emptyMessage="No staff members match your search criteria."
        />
      </Card>

      {/* Modal: Add Staff Member */}
      <Modal
        isOpen={isAddOpen}
        onClose={() => setIsAddOpen(false)}
        title="Add Faculty / Staff Member"
        maxWidth="max-w-lg"
        footer={
          <>
            <Button variant="secondary" onClick={() => setIsAddOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" onClick={handleAddStaff}>
              Save to Roster
            </Button>
          </>
        }
      >
        <form onSubmit={handleAddStaff} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Employee ID / Code"
              required
              placeholder="e.g. EMP-0105"
              value={form.employeeId}
              onChange={(e) => setForm({ ...form, employeeId: e.target.value })}
            />
            <Input
              label="Full Name"
              required
              placeholder="e.g. Rajesh K. Sharma"
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Designation / Role"
              required
              placeholder="e.g. Senior Faculty"
              value={form.designation}
              onChange={(e) => setForm({ ...form, designation: e.target.value })}
            />
            <Input
              label="Department"
              required
              placeholder="e.g. Science & Technology"
              value={form.department}
              onChange={(e) => setForm({ ...form, department: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Staff Email"
              type="email"
              placeholder="staff@school.edu"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
            <Input
              label="Contact Phone"
              placeholder="+91 98765 00000"
              value={form.mobile}
              onChange={(e) => setForm({ ...form, mobile: e.target.value })}
            />
          </div>

          <Input
            label="Blood Group"
            placeholder="e.g. O+, A+, B+, AB-"
            value={form.bloodGroup}
            onChange={(e) => setForm({ ...form, bloodGroup: e.target.value })}
          />
        </form>
      </Modal>
    </div>
  );
}
