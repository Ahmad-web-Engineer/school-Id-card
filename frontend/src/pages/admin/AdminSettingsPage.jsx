import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import {
  SlidersHorizontal,
  CreditCard,
  Building2,
  DollarSign,
  Percent,
  Truck,
  Clock,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  Save,
  RotateCcw,
  TrendingUp,
  Info
} from 'lucide-react';

export default function AdminSettingsPage() {
  const queryClient = useQueryClient();

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['admin-platform-settings'],
    queryFn: () => api.get('/admin/settings'),
  });

  const settings = data?.data;

  const [form, setForm] = useState({
    pricingPvcCard: '45.00',
    pricingRfidCard: '95.00',
    pricingLanyard: '18.00',
    pricingCardHolder: '12.00',
    pricingRushFee: '20.00',
    agencyCostPerCard: '25.00',
    taxRatePercent: '18.00',
    flatShippingFee: '150.00',
    minOrderQuantity: 25,
    paymentDueDays: 30,
    quotationValidityDays: 15,
    defaultDeliveryModel: 'DIRECT_TO_SCHOOL',
    platformCurrency: 'INR',
  });

  const [successMsg, setSuccessMsg] = useState('');
  const [formError, setFormError] = useState('');

  useEffect(() => {
    if (settings) {
      setForm({
        pricingPvcCard: settings.pricingPvcCard != null ? String(settings.pricingPvcCard) : '45.00',
        pricingRfidCard: settings.pricingRfidCard != null ? String(settings.pricingRfidCard) : '95.00',
        pricingLanyard: settings.pricingLanyard != null ? String(settings.pricingLanyard) : '18.00',
        pricingCardHolder: settings.pricingCardHolder != null ? String(settings.pricingCardHolder) : '12.00',
        pricingRushFee: settings.pricingRushFee != null ? String(settings.pricingRushFee) : '20.00',
        agencyCostPerCard: settings.agencyCostPerCard != null ? String(settings.agencyCostPerCard) : '25.00',
        taxRatePercent: settings.taxRatePercent != null ? String(settings.taxRatePercent) : '18.00',
        flatShippingFee: settings.flatShippingFee != null ? String(settings.flatShippingFee) : '150.00',
        minOrderQuantity: settings.minOrderQuantity ?? 25,
        paymentDueDays: settings.paymentDueDays ?? 30,
        quotationValidityDays: settings.quotationValidityDays ?? 15,
        defaultDeliveryModel: settings.defaultDeliveryModel || 'DIRECT_TO_SCHOOL',
        platformCurrency: settings.platformCurrency || 'INR',
      });
    }
  }, [settings]);

  const updateMutation = useMutation({
    mutationFn: (payload) => api.put('/admin/settings', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-platform-settings'] });
      setSuccessMsg('Platform printing charges and operational rules saved successfully!');
      setTimeout(() => setSuccessMsg(''), 5000);
    },
    onError: (err) => {
      setFormError(err.message || 'Failed to update platform settings.');
    },
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    setFormError('');
    setSuccessMsg('');

    const payload = {
      pricingPvcCard: parseFloat(form.pricingPvcCard),
      pricingRfidCard: parseFloat(form.pricingRfidCard),
      pricingLanyard: parseFloat(form.pricingLanyard),
      pricingCardHolder: parseFloat(form.pricingCardHolder),
      pricingRushFee: parseFloat(form.pricingRushFee),
      agencyCostPerCard: parseFloat(form.agencyCostPerCard),
      taxRatePercent: parseFloat(form.taxRatePercent),
      flatShippingFee: parseFloat(form.flatShippingFee),
      minOrderQuantity: parseInt(form.minOrderQuantity, 10),
      paymentDueDays: parseInt(form.paymentDueDays, 10),
      quotationValidityDays: parseInt(form.quotationValidityDays, 10),
      defaultDeliveryModel: form.defaultDeliveryModel,
      platformCurrency: form.platformCurrency.trim().toUpperCase(),
    };

    updateMutation.mutate(payload);
  };

  if (isLoading) {
    return (
      <div className="py-12 flex justify-center">
        <LoadingSpinner size="lg" className="text-indigo-600" />
      </div>
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Could not load platform settings"
        message={error?.message || 'Error communicating with settings service.'}
        errorCode={error?.errorCode}
        onRetry={refetch}
      />
    );
  }

  const pvcPrice = parseFloat(form.pricingPvcCard) || 0;
  const agencyCost = parseFloat(form.agencyCostPerCard) || 0;
  const grossMargin = pvcPrice - agencyCost;
  const marginPercent = pvcPrice > 0 ? ((grossMargin / pvcPrice) * 100).toFixed(1) : '0.0';

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900 flex items-center gap-2">
            <SlidersHorizontal className="h-5 w-5 text-indigo-600" />
            Printing Charges & Platform Settings
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Configure system-wide card printing base prices, manufacturing costs, tax rates, and commercial terms.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="brand" size="md">
            CURRENCY: {form.platformCurrency}
          </Badge>
          <Badge variant="success" size="md">
            LIVE CONFIG
          </Badge>
        </div>
      </div>

      {/* Alerts */}
      {successMsg && (
        <div className="p-3.5 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-lg flex items-center gap-2.5 shadow-2xs">
          <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
          <span className="font-medium">{successMsg}</span>
        </div>
      )}

      {formError && (
        <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-lg flex items-center gap-2.5 shadow-2xs">
          <AlertCircle className="h-4 w-4 shrink-0 text-rose-600" />
          <span className="font-medium">{formError}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Main Configuration Columns (8 cols) */}
          <div className="lg:col-span-8 space-y-6">
            {/* Card 1: Base Card Printing Charges (to Schools) */}
            <Card
              title="School ID Card Base Selling Charges"
              subtitle="Default unit selling rates billed to school institutions per card item."
            >
              <div className="space-y-4 pt-1">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input
                    label="PVC ID Card Unit Price (₹)"
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={form.pricingPvcCard}
                    onChange={(e) => setForm({ ...form, pricingPvcCard: e.target.value })}
                    helperText="Standard 30 mil CR80 PVC card"
                  />
                  <Input
                    label="RFID / Smart Card Unit Price (₹)"
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={form.pricingRfidCard}
                    onChange={(e) => setForm({ ...form, pricingRfidCard: e.target.value })}
                    helperText="13.56 MHz / 125 kHz chip cards"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <Input
                    label="Custom Lanyard Price (₹)"
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={form.pricingLanyard}
                    onChange={(e) => setForm({ ...form, pricingLanyard: e.target.value })}
                    helperText="Printed satin lanyard ribbon"
                  />
                  <Input
                    label="Protective Card Pouch (₹)"
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={form.pricingCardHolder}
                    onChange={(e) => setForm({ ...form, pricingCardHolder: e.target.value })}
                    helperText="Rigid or clear vinyl holder"
                  />
                  <Input
                    label="Rush Surcharge / Card (₹)"
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={form.pricingRushFee}
                    onChange={(e) => setForm({ ...form, pricingRushFee: e.target.value })}
                    helperText="24h expedited production"
                  />
                </div>
              </div>
            </Card>

            {/* Card 2: Agency Manufacturing Rates */}
            <Card
              title="Agency Production & Manufacturing Costs"
              subtitle="Base production rates contracted with printing agencies (Non-negotiable Rule 1: kept strictly hidden from schools)."
            >
              <div className="space-y-4 pt-1">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input
                    label="Agency Cost per Printed Card (₹)"
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={form.agencyCostPerCard}
                    onChange={(e) => setForm({ ...form, agencyCostPerCard: e.target.value })}
                    helperText="Base settlement rate disbursed to printer"
                  />
                  <Input
                    label="Minimum Order Quantity (MOQ)"
                    type="number"
                    min="1"
                    required
                    value={form.minOrderQuantity}
                    onChange={(e) => setForm({ ...form, minOrderQuantity: e.target.value })}
                    helperText="Minimum cards required per print run"
                  />
                </div>
              </div>
            </Card>

            {/* Card 3: Tax, Shipping & Commercial Parameters */}
            <Card
              title="Taxes, Shipping & Commercial Terms"
              subtitle="Statutory tax computation, shipping logistics, and quotation terms."
            >
              <div className="space-y-4 pt-1">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input
                    label="Default GST Rate (%)"
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={form.taxRatePercent}
                    onChange={(e) => setForm({ ...form, taxRatePercent: e.target.value })}
                    helperText="Standard Goods & Services Tax percentage"
                  />
                  <Input
                    label="Flat Courier & Delivery Fee (₹)"
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={form.flatShippingFee}
                    onChange={(e) => setForm({ ...form, flatShippingFee: e.target.value })}
                    helperText="Standard door-step delivery charge"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input
                    label="Quotation Validity (Days)"
                    type="number"
                    min="1"
                    required
                    value={form.quotationValidityDays}
                    onChange={(e) => setForm({ ...form, quotationValidityDays: e.target.value })}
                    helperText="Days before quotation expires"
                  />
                  <Input
                    label="Invoice Payment Terms (Days)"
                    type="number"
                    min="1"
                    required
                    value={form.paymentDueDays}
                    onChange={(e) => setForm({ ...form, paymentDueDays: e.target.value })}
                    helperText="Due period for credit settlements"
                  />
                </div>
              </div>
            </Card>

            {/* Card 4: Operations & Fulfillment Policy */}
            <Card
              title="Operations & Regional Settings"
              subtitle="Platform currency and default delivery routing."
            >
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
                <div>
                  <label className="block text-xs font-semibold text-slate-800 mb-1">
                    Default Delivery Route Model
                  </label>
                  <select
                    value={form.defaultDeliveryModel}
                    onChange={(e) => setForm({ ...form, defaultDeliveryModel: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs bg-white focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500"
                  >
                    <option value="DIRECT_TO_SCHOOL">DIRECT_TO_SCHOOL (Agency ships directly to School)</option>
                    <option value="PLATFORM_QC_THEN_SCHOOL">PLATFORM_QC_THEN_SCHOOL (Agency ships to Hub for Central QC)</option>
                  </select>
                  <span className="text-[11px] text-slate-400 mt-1 block">Default routing assigned to new job dispatches</span>
                </div>

                <Input
                  label="Platform Currency Code"
                  required
                  placeholder="INR"
                  value={form.platformCurrency}
                  onChange={(e) => setForm({ ...form, platformCurrency: e.target.value.toUpperCase() })}
                  helperText="ISO 4217 currency code (e.g. INR, USD)"
                />
              </div>
            </Card>

            {/* Save Buttons */}
            <div className="flex items-center justify-end gap-3 pt-2">
              <Button
                type="button"
                variant="secondary"
                onClick={() => refetch()}
                disabled={updateMutation.isPending}
              >
                <RotateCcw className="h-4 w-4 mr-1.5" />
                Reset Defaults
              </Button>
              <Button
                type="submit"
                variant="primary"
                loading={updateMutation.isPending}
                className="flex items-center gap-1.5"
              >
                <Save className="h-4 w-4" />
                <span>Save Printing Charges & Settings</span>
              </Button>
            </div>
          </div>

          {/* Right Summary / Margin Calculator Sidebar (4 cols) */}
          <div className="lg:col-span-4 space-y-4 sticky top-6">
            <Card title="Unit Economics & Margin Preview">
              <div className="space-y-4 text-xs">
                <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg space-y-2">
                  <div className="flex items-center justify-between text-slate-600">
                    <span>School PVC Price:</span>
                    <span className="font-semibold text-slate-900">₹{pvcPrice.toFixed(2)}</span>
                  </div>
                  <div className="flex items-center justify-between text-slate-600">
                    <span>Agency Base Cost:</span>
                    <span className="font-semibold text-rose-700">-₹{agencyCost.toFixed(2)}</span>
                  </div>
                  <div className="pt-2 border-t border-slate-200 flex items-center justify-between font-bold">
                    <span className="text-slate-900">Gross Margin:</span>
                    <span className="text-emerald-700">₹{grossMargin.toFixed(2)} ({marginPercent}%)</span>
                  </div>
                </div>

                <div className="space-y-2 pt-2 border-t border-slate-100 text-slate-600">
                  <div className="flex items-center gap-2">
                    <TrendingUp className="h-4 w-4 text-emerald-600 shrink-0" />
                    <span>Real-time unit margin automatically reflected in quotation defaults.</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Percent className="h-4 w-4 text-indigo-600 shrink-0" />
                    <span>Applied GST: {form.taxRatePercent}% on subtotal.</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <ShieldCheck className="h-4 w-4 text-indigo-600 shrink-0" />
                    <span>Role isolation active: Agency partners never see school charges or platform margins.</span>
                  </div>
                </div>

                {settings?.lastUpdatedAt && (
                  <div className="pt-3 border-t border-slate-100 text-[11px] text-slate-400">
                    Last updated: {new Date(settings.lastUpdatedAt).toLocaleString()}
                  </div>
                )}
              </div>
            </Card>
          </div>
        </div>
      </form>
    </div>
  );
}
