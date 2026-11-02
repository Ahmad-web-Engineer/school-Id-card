import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Textarea from '../../components/common/Textarea';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import { ACCESSORY_OPTIONS } from './ordersConstants';
import {
  Layers,
  Users,
  Package,
  Truck,
  CheckCircle2,
  AlertTriangle,
  Search,
  Check,
  ChevronRight,
  ChevronLeft,
  Image as ImageIcon
} from 'lucide-react';

export default function NewOrderWizardModal({ isOpen, onClose, onSuccess }) {
  const queryClient = useQueryClient();
  const [currentStep, setCurrentStep] = useState(1); // 1: Template, 2: Students, 3: Accessories, 4: Delivery & Review

  // Form states
  const [selectedTemplateId, setSelectedTemplateId] = useState(null);
  const [selectedTemplateVersionId, setSelectedTemplateVersionId] = useState(null);
  const [selectedStudentIds, setSelectedStudentIds] = useState([]);
  
  // Student filtering in Step 2
  const [studentSearch, setStudentSearch] = useState('');
  const [classFilter, setClassFilter] = useState('');

  // Accessories in Step 3
  const [accessories, setAccessories] = useState([
    { type: 'LANYARD', enabled: false, quantity: 0, unitPrice: 25.00, itemDescription: 'Premium Polyester Lanyard' },
    { type: 'CARD_HOLDER', enabled: false, quantity: 0, unitPrice: 15.00, itemDescription: 'Rigid PVC Transparent Card Holder' },
    { type: 'CLIP', enabled: false, quantity: 0, unitPrice: 10.00, itemDescription: 'Alligator Clip with Vinyl Strap' },
  ]);

  // Delivery & contact in Step 4
  const [deliveryAddress, setDeliveryAddress] = useState('');
  const [contactPerson, setContactPerson] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [notes, setNotes] = useState('');

  const [formError, setFormError] = useState(null);

  // 1. Fetch school approved templates
  const { data: templatesRes, isLoading: isLoadingTemplates } = useQuery({
    queryKey: ['school-templates-for-order'],
    queryFn: () => api.get('/school/templates'),
    enabled: isOpen,
  });

  // 2. Fetch school students roster
  const { data: studentsRes, isLoading: isLoadingStudents } = useQuery({
    queryKey: ['school-students-for-order'],
    queryFn: () => api.get('/school/students', { params: { page: 0, size: 500 } }),
    enabled: isOpen,
  });

  // 3. Fetch school profile to prefill delivery
  const { data: profileRes } = useQuery({
    queryKey: ['school-profile-for-order'],
    queryFn: () => api.get('/school/profile'),
    enabled: isOpen,
  });

  // Prefill delivery when profile loads
  useEffect(() => {
    if (profileRes?.data) {
      const p = profileRes.data;
      const addr = [p.addressLine1, p.addressLine2, p.city, p.state, p.postalCode].filter(Boolean).join(', ');
      setDeliveryAddress(addr || '');
      setContactPerson(p.contactPersonName || p.principalName || '');
      setContactPhone(p.contactPhone || p.phone || '');
    }
  }, [profileRes]);

  // When selected student count changes, auto-update enabled accessories quantities
  useEffect(() => {
    const count = selectedStudentIds.length;
    setAccessories((prev) =>
      prev.map((acc) => (acc.enabled ? { ...acc, quantity: count } : acc))
    );
  }, [selectedStudentIds.length]);

  // Reset modal state on open
  useEffect(() => {
    if (isOpen) {
      setCurrentStep(1);
      setSelectedTemplateId(null);
      setSelectedTemplateVersionId(null);
      setSelectedStudentIds([]);
      setFormError(null);
    }
  }, [isOpen]);

  const templates = templatesRes?.data || [];
  const students = studentsRes?.data?.content || [];

  // Filter students based on search and class
  const filteredStudents = students.filter((s) => {
    const matchesSearch =
      !studentSearch ||
      s.fullName?.toLowerCase().includes(studentSearch.toLowerCase()) ||
      s.admissionNumber?.toLowerCase().includes(studentSearch.toLowerCase());
    const matchesClass = !classFilter || s.standard === classFilter;
    return matchesSearch && matchesClass;
  });

  // Unique classes for filter dropdown
  const uniqueClasses = Array.from(new Set(students.map((s) => s.standard).filter(Boolean))).sort();

  const handleSelectAllFiltered = () => {
    const filteredIds = filteredStudents.map((s) => s.id);
    const combined = Array.from(new Set([...selectedStudentIds, ...filteredIds]));
    setSelectedStudentIds(combined);
  };

  const handleClearSelected = () => {
    setSelectedStudentIds([]);
  };

  const toggleStudentSelection = (id) => {
    setSelectedStudentIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleAccessoryToggle = (index, enabled) => {
    setAccessories((prev) => {
      const copy = [...prev];
      copy[index] = {
        ...copy[index],
        enabled,
        quantity: enabled ? selectedStudentIds.length : 0,
      };
      return copy;
    });
  };

  const handleAccessoryQuantityChange = (index, qty) => {
    setAccessories((prev) => {
      const copy = [...prev];
      copy[index] = {
        ...copy[index],
        quantity: Math.max(0, parseInt(qty, 10) || 0),
      };
      return copy;
    });
  };

  // Create order mutation
  const createOrderMutation = useMutation({
    mutationFn: async ({ shouldSubmit = false }) => {
      setFormError(null);
      const activeAccessories = accessories
        .filter((a) => a.enabled && a.quantity > 0)
        .map((a) => ({
          itemType: a.type,
          itemDescription: a.itemDescription,
          quantity: a.quantity,
          unitPrice: a.unitPrice,
        }));

      const payload = {
        templateVersionId: selectedTemplateVersionId,
        studentIds: selectedStudentIds,
        additionalItems: activeAccessories,
        deliveryAddress: deliveryAddress.trim() || 'School Campus Delivery',
        contactPerson: contactPerson.trim() || 'School Administrator',
        contactPhone: contactPhone.trim() || '0000000000',
        notes: notes.trim() || undefined,
      };

      const res = await api.post('/school/orders', payload);
      const order = res.data;

      if (shouldSubmit && order?.id) {
        await api.post(`/school/orders/${order.id}/submit`);
      }

      return order;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['school-orders'] });
      if (onSuccess) onSuccess(data);
      onClose();
    },
    onError: (err) => {
      setFormError(err.message || 'Failed to create order. Please try again.');
    },
  });

  const handleNextStep = () => {
    setFormError(null);
    if (currentStep === 1) {
      if (!selectedTemplateVersionId) {
        setFormError('Please select an ID card template to proceed.');
        return;
      }
      setCurrentStep(2);
    } else if (currentStep === 2) {
      if (selectedStudentIds.length === 0) {
        setFormError('Please select at least one student for the printing order.');
        return;
      }
      setCurrentStep(3);
    } else if (currentStep === 3) {
      setCurrentStep(4);
    }
  };

  const selectedTemplate = templates.find((t) => t.id === selectedTemplateId);
  const selectedStudentsData = students.filter((s) => selectedStudentIds.includes(s.id));
  const missingPhotosCount = selectedStudentsData.filter((s) => !s.hasPhoto).length;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Create New ID Card Printing Order"
      maxWidth="max-w-4xl"
      footer={
        <div className="flex items-center justify-between w-full">
          <div>
            {currentStep > 1 && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentStep((s) => s - 1)}
                disabled={createOrderMutation.isPending}
              >
                <ChevronLeft className="h-4 w-4 mr-1" />
                Back
              </Button>
            )}
          </div>
          <div className="flex items-center space-x-3">
            <Button
              variant="outline"
              size="sm"
              onClick={onClose}
              disabled={createOrderMutation.isPending}
            >
              Cancel
            </Button>
            {currentStep < 4 ? (
              <Button size="sm" onClick={handleNextStep}>
                Next Step
                <ChevronRight className="h-4 w-4 ml-1" />
              </Button>
            ) : (
              <div className="flex items-center space-x-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => createOrderMutation.mutate({ shouldSubmit: false })}
                  disabled={createOrderMutation.isPending}
                >
                  {createOrderMutation.isPending ? <LoadingSpinner size="sm" className="mr-1" /> : null}
                  Save as Draft
                </Button>
                <Button
                  size="sm"
                  onClick={() => createOrderMutation.mutate({ shouldSubmit: true })}
                  disabled={createOrderMutation.isPending}
                >
                  {createOrderMutation.isPending ? <LoadingSpinner size="sm" className="mr-1" /> : null}
                  Submit Order
                </Button>
              </div>
            )}
          </div>
        </div>
      }
    >
      <div className="space-y-6">
        {/* Step Indicator */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-4">
          {[
            { num: 1, title: 'Card Template', icon: Layers },
            { num: 2, title: 'Select Students', icon: Users },
            { num: 3, title: 'Accessories', icon: Package },
            { num: 4, title: 'Delivery & Review', icon: Truck },
          ].map((step) => {
            const Icon = step.icon;
            const isActive = currentStep === step.num;
            const isCompleted = currentStep > step.num;
            return (
              <div key={step.num} className="flex items-center space-x-2">
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-semibold ${
                    isCompleted
                      ? 'bg-emerald-600 text-white'
                      : isActive
                      ? 'bg-indigo-600 text-white ring-4 ring-indigo-50'
                      : 'bg-slate-100 text-slate-500'
                  }`}
                >
                  {isCompleted ? <Check className="h-4 w-4" /> : step.num}
                </div>
                <span
                  className={`text-xs font-medium hidden sm:inline ${
                    isActive ? 'text-indigo-900 font-semibold' : 'text-slate-500'
                  }`}
                >
                  {step.title}
                </span>
              </div>
            );
          })}
        </div>

        {formError && (
          <div className="rounded-lg bg-rose-50 border border-rose-200 p-3 flex items-start space-x-2">
            <AlertTriangle className="h-4 w-4 text-rose-600 mt-0.5 flex-shrink-0" />
            <p className="text-xs text-rose-700">{formError}</p>
          </div>
        )}

        {/* STEP 1: Template Selection */}
        {currentStep === 1 && (
          <div className="space-y-4">
            <div>
              <h4 className="text-sm font-semibold text-slate-900">Select Approved ID Card Template</h4>
              <p className="text-xs text-slate-500">
                Choose the verified template that will be printed for this batch.
              </p>
            </div>

            {isLoadingTemplates ? (
              <div className="py-12 flex justify-center">
                <LoadingSpinner size="md" className="text-indigo-600" />
              </div>
            ) : templates.length === 0 ? (
              <div className="rounded-lg border border-dashed border-slate-200 p-8 text-center bg-slate-50">
                <Layers className="h-8 w-8 text-slate-400 mx-auto mb-2" />
                <p className="text-sm font-medium text-slate-700">No approved templates assigned</p>
                <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                  Your school does not have any approved ID card templates assigned yet. Contact the printing administrator.
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                {templates.map((tpl) => {
                  const isSelected = selectedTemplateId === tpl.id;
                  return (
                    <div
                      key={tpl.id}
                      onClick={() => {
                        setSelectedTemplateId(tpl.id);
                        setSelectedTemplateVersionId(tpl.approvedVersionId || tpl.versionId);
                      }}
                      className={`relative rounded-lg border p-4 cursor-pointer transition-all ${
                        isSelected
                          ? 'border-indigo-600 bg-indigo-50/40 ring-2 ring-indigo-500/20'
                          : 'border-slate-200 hover:border-slate-300 bg-white'
                      }`}
                    >
                      {isSelected && (
                        <div className="absolute top-2 right-2 w-5 h-5 bg-indigo-600 text-white rounded-full flex items-center justify-center">
                          <Check className="h-3 w-3" />
                        </div>
                      )}
                      <div className="flex items-center space-x-2 mb-2">
                        <Layers className="h-4 w-4 text-indigo-600" />
                        <h5 className="text-xs font-semibold text-slate-900 truncate">{tpl.name}</h5>
                      </div>
                      <div className="space-y-1 text-[11px] text-slate-500">
                        <p>Orientation: <span className="font-medium text-slate-700 capitalize">{tpl.orientation?.toLowerCase()}</span></p>
                        <p>Dimensions: <span className="font-medium text-slate-700">{tpl.cardWidthMm} × {tpl.cardHeightMm} mm</span></p>
                        <p>Version: <span className="font-medium text-slate-700">v{tpl.versionNumber || 1}</span></p>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* STEP 2: Student Roster Selection */}
        {currentStep === 2 && (
          <div className="space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <h4 className="text-sm font-semibold text-slate-900">Select Students ({selectedStudentIds.length} Selected)</h4>
                <p className="text-xs text-slate-500">
                  Select students to include in this print batch. Snapshots will be frozen at order creation.
                </p>
              </div>
              <div className="flex items-center space-x-2">
                <Button variant="outline" size="sm" onClick={handleSelectAllFiltered}>
                  Select All ({filteredStudents.length})
                </Button>
                {selectedStudentIds.length > 0 && (
                  <Button variant="ghost" size="sm" onClick={handleClearSelected} className="text-rose-600">
                    Clear Selection
                  </Button>
                )}
              </div>
            </div>

            {/* Filter Bar */}
            <div className="flex flex-col sm:flex-row items-center gap-3">
              <div className="relative flex-1 w-full">
                <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search student name or admission number..."
                  value={studentSearch}
                  onChange={(e) => setStudentSearch(e.target.value)}
                  className="w-full pl-9 pr-3 py-1.5 text-xs rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <select
                value={classFilter}
                onChange={(e) => setClassFilter(e.target.value)}
                className="w-full sm:w-36 text-xs px-2.5 py-1.5 rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="">All Classes</option>
                {uniqueClasses.map((c) => (
                  <option key={c} value={c}>Class {c}</option>
                ))}
              </select>
            </div>

            {/* Student Table */}
            {isLoadingStudents ? (
              <div className="py-12 flex justify-center">
                <LoadingSpinner size="md" className="text-indigo-600" />
              </div>
            ) : filteredStudents.length === 0 ? (
              <div className="py-8 text-center text-xs text-slate-500 bg-slate-50 rounded-lg border border-slate-100">
                No students match your filter criteria.
              </div>
            ) : (
              <div className="border border-slate-200 rounded-lg max-h-72 overflow-y-auto">
                <table className="min-w-full divide-y divide-slate-100 text-xs">
                  <thead className="bg-slate-50 text-slate-600 sticky top-0 z-10 font-semibold">
                    <tr>
                      <th className="px-3 py-2 text-left w-10">Select</th>
                      <th className="px-3 py-2 text-left">Student Name</th>
                      <th className="px-3 py-2 text-left">Admission No</th>
                      <th className="px-3 py-2 text-left">Class & Sec</th>
                      <th className="px-3 py-2 text-left">Roll No</th>
                      <th className="px-3 py-2 text-left">Photo</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-50 text-slate-800">
                    {filteredStudents.map((s) => {
                      const isSelected = selectedStudentIds.includes(s.id);
                      return (
                        <tr
                          key={s.id}
                          onClick={() => toggleStudentSelection(s.id)}
                          className={`cursor-pointer hover:bg-slate-50 transition-colors ${
                            isSelected ? 'bg-indigo-50/50' : ''
                          }`}
                        >
                          <td className="px-3 py-2">
                            <input
                              type="checkbox"
                              checked={isSelected}
                              onChange={() => {}}
                              className="rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 h-4 w-4"
                            />
                          </td>
                          <td className="px-3 py-2 font-medium text-slate-900">{s.fullName}</td>
                          <td className="px-3 py-2 text-slate-500 font-mono">{s.admissionNumber}</td>
                          <td className="px-3 py-2">{s.standard}{s.section ? ` - ${s.section}` : ''}</td>
                          <td className="px-3 py-2 text-slate-500">{s.rollNumber || '-'}</td>
                          <td className="px-3 py-2">
                            {s.hasPhoto ? (
                              <span className="inline-flex items-center text-[10px] text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full font-medium">
                                <Check className="h-3 w-3 mr-0.5" /> Photo Ready
                              </span>
                            ) : (
                              <span className="inline-flex items-center text-[10px] text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full font-medium">
                                <AlertTriangle className="h-3 w-3 mr-0.5" /> Missing Photo
                              </span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* STEP 3: Accessories */}
        {currentStep === 3 && (
          <div className="space-y-4">
            <div>
              <h4 className="text-sm font-semibold text-slate-900">Accessories & Supplementary Items</h4>
              <p className="text-xs text-slate-500">
                Optionally include lanyards, holders, and clips for this print batch.
              </p>
            </div>

            <div className="space-y-3">
              {accessories.map((acc, idx) => (
                <div
                  key={acc.type}
                  className={`rounded-lg border p-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3 transition-colors ${
                    acc.enabled ? 'border-indigo-500 bg-indigo-50/20' : 'border-slate-200 bg-white'
                  }`}
                >
                  <div className="flex items-start space-x-3">
                    <input
                      type="checkbox"
                      id={`acc-${idx}`}
                      checked={acc.enabled}
                      onChange={(e) => handleAccessoryToggle(idx, e.target.checked)}
                      className="mt-1 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 h-4 w-4"
                    />
                    <div>
                      <label htmlFor={`acc-${idx}`} className="text-xs font-semibold text-slate-900 cursor-pointer">
                        {acc.itemDescription}
                      </label>
                      <p className="text-[11px] text-slate-500">Estimated unit price: ₹{acc.unitPrice.toFixed(2)}</p>
                    </div>
                  </div>

                  {acc.enabled && (
                    <div className="flex items-center space-x-2 pl-7 sm:pl-0">
                      <span className="text-xs text-slate-600">Qty:</span>
                      <input
                        type="number"
                        min="1"
                        value={acc.quantity}
                        onChange={(e) => handleAccessoryQuantityChange(idx, e.target.value)}
                        className="w-20 px-2 py-1 text-xs rounded border border-slate-200 text-center font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      />
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* STEP 4: Delivery & Review */}
        {currentStep === 4 && (
          <div className="space-y-4">
            <div>
              <h4 className="text-sm font-semibold text-slate-900">Delivery Information & Final Review</h4>
              <p className="text-xs text-slate-500">
                Confirm delivery address and review order quantities before submission.
              </p>
            </div>

            {missingPhotosCount > 0 && (
              <div className="rounded-lg bg-amber-50 border border-amber-200 p-3 flex items-start space-x-2">
                <AlertTriangle className="h-4 w-4 text-amber-600 mt-0.5 flex-shrink-0" />
                <div className="text-xs text-amber-800">
                  <span className="font-semibold">{missingPhotosCount} student(s)</span> in this order do not have a photo uploaded. The cards will be printed without photos unless photos are updated prior to production.
                </div>
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <Input
                label="Contact Person Name"
                placeholder="Principal / Admin in charge"
                value={contactPerson}
                onChange={(e) => setContactPerson(e.target.value)}
                required
              />
              <Input
                label="Contact Phone"
                placeholder="10-digit mobile number"
                value={contactPhone}
                onChange={(e) => setContactPhone(e.target.value)}
                required
              />
            </div>

            <Textarea
              label="Delivery Address"
              rows={2}
              placeholder="Campus gate, address, city, state, pin..."
              value={deliveryAddress}
              onChange={(e) => setDeliveryAddress(e.target.value)}
              required
            />

            <Textarea
              label="Order Notes / Instructions (Optional)"
              rows={2}
              placeholder="Any special packing, sorting by class, or deadline requests..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />

            {/* Review Summary */}
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-4 space-y-2">
              <h5 className="text-xs font-semibold text-slate-900 uppercase tracking-wider">Order Summary</h5>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1 text-xs">
                <div>
                  <span className="text-slate-500 block">Template</span>
                  <span className="font-semibold text-slate-800">{selectedTemplate?.name || 'Selected Template'}</span>
                </div>
                <div>
                  <span className="text-slate-500 block">ID Cards</span>
                  <span className="font-semibold text-slate-800">{selectedStudentIds.length} cards</span>
                </div>
                <div>
                  <span className="text-slate-500 block">Accessories</span>
                  <span className="font-semibold text-slate-800">
                    {accessories.filter((a) => a.enabled && a.quantity > 0).length} items
                  </span>
                </div>
                <div>
                  <span className="text-slate-500 block">Total Quantity</span>
                  <span className="font-semibold text-slate-800">
                    {selectedStudentIds.length +
                      accessories
                        .filter((a) => a.enabled)
                        .reduce((acc, curr) => acc + curr.quantity, 0)}{' '}
                    units
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
