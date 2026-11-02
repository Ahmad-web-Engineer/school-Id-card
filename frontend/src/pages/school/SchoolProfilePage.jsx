import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import CardCanvas from '../../features/templates/CardCanvas';
import { useAuthStore } from '../../store/useAuthStore';
import {
  School,
  Building,
  Mail,
  Phone,
  MapPin,
  Globe,
  Award,
  PenTool,
  Upload,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  ShieldCheck,
  Eye,
  Save,
} from 'lucide-react';

const PROFILE_PREVIEW_ELEMENTS = [
  {
    id: 'el-header-bar',
    type: 'rect',
    x: 0,
    y: 0,
    width: 85.6,
    height: 14.5,
    backgroundColor: '#1e3a8a',
    borderColor: '#1e3a8a',
    borderWidth: 0,
    borderRadius: 0,
  },
  {
    id: 'el-school-logo',
    type: 'image',
    field: 'school_logo',
    x: 2.5,
    y: 1.5,
    width: 11.5,
    height: 11.5,
    label: 'School Logo',
  },
  {
    id: 'el-school-title',
    type: 'text',
    field: 'school_name',
    content: '{{school_name}}',
    x: 15.5,
    y: 1.2,
    width: 68,
    height: 4.8,
    fontFamily: 'Inter, sans-serif',
    fontSize: 9.5,
    fontWeight: 'bold',
    color: '#ffffff',
    textAlign: 'left',
  },
  {
    id: 'el-school-tagline',
    type: 'text',
    content: '{{school_tagline}}',
    x: 15.5,
    y: 6.0,
    width: 68,
    height: 3.2,
    fontFamily: 'Inter, sans-serif',
    fontSize: 5.5,
    fontWeight: 'medium',
    color: '#bfdbfe',
    textAlign: 'left',
  },
  {
    id: 'el-school-affiliation',
    type: 'text',
    content: 'Affiliation: {{affiliation_number}}',
    x: 15.5,
    y: 9.6,
    width: 68,
    height: 3.2,
    fontFamily: 'Inter, sans-serif',
    fontSize: 5.0,
    fontWeight: 'normal',
    color: '#e0e7ff',
    textAlign: 'left',
  },
  {
    id: 'el-photo-box',
    type: 'image',
    field: 'student_photo',
    x: 5,
    y: 16.5,
    width: 22,
    height: 27,
    borderColor: '#cbd5e1',
    borderWidth: 1,
    borderRadius: 2,
    label: 'Student Photo',
  },
  {
    id: 'el-student-name',
    type: 'text',
    field: 'student_name',
    content: '{{student_name}}',
    x: 30,
    y: 16.5,
    width: 50,
    height: 5,
    fontFamily: 'Inter, sans-serif',
    fontSize: 10.5,
    fontWeight: 'bold',
    color: '#0f172a',
    textAlign: 'left',
  },
  {
    id: 'el-adm-number',
    type: 'text',
    content: 'Adm No: {{admission_number}}',
    x: 30,
    y: 22.0,
    width: 32,
    height: 3.8,
    fontFamily: 'Inter, sans-serif',
    fontSize: 7.0,
    fontWeight: 'normal',
    color: '#334155',
    textAlign: 'left',
  },
  {
    id: 'el-class-sec',
    type: 'text',
    content: 'Class: {{class}} - {{section}}',
    x: 30,
    y: 26.2,
    width: 32,
    height: 3.8,
    fontFamily: 'Inter, sans-serif',
    fontSize: 7.0,
    fontWeight: 'normal',
    color: '#334155',
    textAlign: 'left',
  },
  {
    id: 'el-blood-group',
    type: 'text',
    content: 'Blood Grp: {{blood_group}}',
    x: 30,
    y: 30.4,
    width: 32,
    height: 3.8,
    fontFamily: 'Inter, sans-serif',
    fontSize: 7.0,
    fontWeight: 'semibold',
    color: '#dc2626',
    textAlign: 'left',
  },
  {
    id: 'el-dob',
    type: 'text',
    content: 'DOB: {{date_of_birth}}',
    x: 30,
    y: 34.6,
    width: 32,
    height: 3.8,
    fontFamily: 'Inter, sans-serif',
    fontSize: 7.0,
    fontWeight: 'normal',
    color: '#334155',
    textAlign: 'left',
  },
  {
    id: 'el-parent-phone',
    type: 'text',
    content: 'Emergency: {{emergency_contact}}',
    x: 5,
    y: 44.5,
    width: 50,
    height: 4,
    fontFamily: 'Inter, sans-serif',
    fontSize: 6.8,
    fontWeight: 'medium',
    color: '#0f172a',
    textAlign: 'left',
  },
  {
    id: 'el-signature-box',
    type: 'signature',
    label: 'Authorized Signatory',
    x: 57,
    y: 35.5,
    width: 25,
    height: 13,
    borderColor: '#94a3b8',
    borderWidth: 1,
    borderRadius: 1,
  },
  {
    id: 'el-footer-bar',
    type: 'rect',
    x: 0,
    y: 49.5,
    width: 85.6,
    height: 4.5,
    backgroundColor: '#1e3a8a',
    borderColor: '#1e3a8a',
    borderWidth: 0,
    borderRadius: 0,
  },
  {
    id: 'el-footer-website',
    type: 'text',
    content: '{{school_website}}',
    x: 2,
    y: 50.2,
    width: 81.6,
    height: 3.2,
    fontFamily: 'Inter, sans-serif',
    fontSize: 5.5,
    fontWeight: 'medium',
    color: '#ffffff',
    textAlign: 'center',
  },
];

export default function SchoolProfilePage() {
  const queryClient = useQueryClient();
  const token = useAuthStore((state) => state.token);

  const getAuthFileUrl = (fileId) => {
    if (!fileId) return '';
    return `/api/v1/files/${fileId}${token ? `?token=${encodeURIComponent(token)}` : ''}`;
  };

  const { data: profileData, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['school-own-profile'],
    queryFn: () => api.get('/school/profile'),
  });

  const school = profileData?.data;

  const [form, setForm] = useState({
    schoolName: '',
    address: '',
    city: '',
    state: '',
    pincode: '',
    email: '',
    mobile: '',
    website: '',
    affiliationNumber: '',
    tagline: '',
    principalName: '',
    logoFileId: '',
    signatureFileId: '',
  });

  const [logoPreviewUrl, setLogoPreviewUrl] = useState('');
  const [signaturePreviewUrl, setSignaturePreviewUrl] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [formError, setFormError] = useState('');
  const [logoUploading, setLogoUploading] = useState(false);
  const [sigUploading, setSigUploading] = useState(false);

  useEffect(() => {
    if (school) {
      setForm({
        schoolName: school.schoolName || '',
        address: school.address || '',
        city: school.city || '',
        state: school.state || '',
        pincode: school.pincode || '',
        email: school.email || '',
        mobile: school.mobile || '',
        website: school.website || '',
        affiliationNumber: school.affiliationNumber || '',
        tagline: school.tagline || '',
        principalName: school.principalName || '',
        logoFileId: school.logoFileId || '',
        signatureFileId: school.signatureFileId || '',
      });

      if (school.logoFileId) {
        setLogoPreviewUrl(getAuthFileUrl(school.logoFileId));
      } else {
        setLogoPreviewUrl('');
      }
      if (school.signatureFileId) {
        setSignaturePreviewUrl(getAuthFileUrl(school.signatureFileId));
      } else {
        setSignaturePreviewUrl('');
      }
    }
  }, [school, token]);

  // Mutation: Save School Settings
  const saveMutation = useMutation({
    mutationFn: (payload) => api.put('/school/profile', payload),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['school-own-profile'] });
      setSuccessMsg('School settings saved successfully! All ID card templates will now auto-populate with your data.');
      setTimeout(() => setSuccessMsg(''), 5000);
    },
    onError: (err) => {
      setFormError(err.message || 'Failed to update school settings.');
    },
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    setFormError('');
    setSuccessMsg('');
    saveMutation.mutate(form);
  };

  const handleLogoUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Local instant preview
    const localUrl = URL.createObjectURL(file);
    setLogoPreviewUrl(localUrl);

    setLogoUploading(true);
    setFormError('');

    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await api.post('/school/profile/logo', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      if (res?.data?.logoFileId) {
        setForm((prev) => ({ ...prev, logoFileId: res.data.logoFileId }));
        setLogoPreviewUrl(getAuthFileUrl(res.data.logoFileId));
      }
      queryClient.invalidateQueries({ queryKey: ['school-own-profile'] });
      setSuccessMsg('Institutional logo uploaded and linked successfully!');
      setTimeout(() => setSuccessMsg(''), 4000);
    } catch (err) {
      setFormError(err.message || 'Failed to upload school logo.');
    } finally {
      setLogoUploading(false);
    }
  };

  const handleSignatureUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Local instant preview
    const localUrl = URL.createObjectURL(file);
    setSignaturePreviewUrl(localUrl);

    setSigUploading(true);
    setFormError('');

    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await api.post('/school/profile/signature', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      if (res?.data?.signatureFileId) {
        setForm((prev) => ({ ...prev, signatureFileId: res.data.signatureFileId }));
        setSignaturePreviewUrl(getAuthFileUrl(res.data.signatureFileId));
      }
      queryClient.invalidateQueries({ queryKey: ['school-own-profile'] });
      setSuccessMsg('Principal signature uploaded and linked successfully!');
      setTimeout(() => setSuccessMsg(''), 4000);
    } catch (err) {
      setFormError(err.message || 'Failed to upload authorized signature.');
    } finally {
      setSigUploading(false);
    }
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
        title="Could not load school settings"
        message={error?.message || 'Error communicating with school profile service.'}
        errorCode={error?.errorCode}
        onRetry={refetch}
      />
    );
  }

  // Simulated active live student preview combining current school settings
  const livePreviewData = {
    student_name: 'Aarav Sharma',
    admission_number: 'ADM-2026-001',
    class: '10th',
    section: 'A',
    roll_number: '12',
    date_of_birth: '15-05-2010',
    blood_group: 'B+',
    emergency_contact: '+91 98765 43210',
    school_name: form.schoolName || school?.schoolName || 'Your School Name',
    school_code: school?.schoolCode || 'SCH-001',
    school_address: form.address ? `${form.address}, ${form.city}` : '14 Heritage Boulevard',
    school_phone: form.mobile || '+91 80 2345 6789',
    school_email: form.email || 'office@school.edu',
    school_website: form.website || 'www.school.edu',
    affiliation_number: form.affiliationNumber || 'CBSE / STATE BOARD',
    school_tagline: form.tagline || 'Excellence in Education',
    principal_name: form.principalName || 'Principal',
    academic_session: '2026-2027',
    photo_url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80',
    school_logo_url: logoPreviewUrl || null,
    signature_url: signaturePreviewUrl || null,
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">School Profile & Card Identity Settings</h1>
          <p className="text-xs text-slate-500 mt-1">
            Configure institutional details, logo, affiliation, and authorized signature. These values automatically auto-populate onto all student and staff ID card templates.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="brand" size="md">
            CODE: {school?.schoolCode || 'GWIS-001'}
          </Badge>
          <Badge variant="success" size="md">
            {school?.status || 'ACTIVE'}
          </Badge>
        </div>
      </div>

      {/* Success / Error Feedback */}
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

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Form: Settings & Uploads (7 cols) */}
        <form onSubmit={handleSubmit} className="lg:col-span-7 space-y-6">
          {/* Card 1: Institutional Identity */}
          <Card title="Institution Identity & Branding">
            <div className="space-y-4 pt-1">
              <Input
                label="Institution Full Name"
                required
                placeholder="e.g. Greenwood International High School"
                value={form.schoolName}
                onChange={(e) => setForm({ ...form, schoolName: e.target.value })}
              />

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Input
                  label="Affiliation / Board Accreditation"
                  placeholder="e.g. CBSE / AFF-830192 or ICSE"
                  value={form.affiliationNumber}
                  onChange={(e) => setForm({ ...form, affiliationNumber: e.target.value })}
                />
                <Input
                  label="Official Website URL"
                  placeholder="e.g. https://greenwood.edu"
                  value={form.website}
                  onChange={(e) => setForm({ ...form, website: e.target.value })}
                />
              </div>

              <Input
                label="Motto / Tagline"
                placeholder="e.g. Empowering Minds, Inspiring Futures"
                value={form.tagline}
                onChange={(e) => setForm({ ...form, tagline: e.target.value })}
              />
            </div>
          </Card>

          {/* Card 2: Digital Asset Uploads (Logo & Signature) */}
          <Card title="Official Logo & Authorized Signature">
            <p className="text-xs text-slate-500 mb-4">
              Upload high-resolution image assets. Both will be automatically superimposed on your school's ID cards.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Logo Box */}
              <div className="p-4 border border-slate-200 rounded-lg bg-slate-50/50 flex flex-col justify-between space-y-3">
                <div>
                  <span className="text-xs font-semibold text-slate-800 block">School Official Logo</span>
                  <span className="text-[11px] text-slate-500">Square or transparent PNG recommended</span>
                </div>

                <div className="h-24 bg-white border border-dashed border-slate-300 rounded flex items-center justify-center p-2 overflow-hidden relative">
                  {logoPreviewUrl ? (
                    <img
                      src={logoPreviewUrl}
                      alt="Logo Preview"
                      className="h-full object-contain"
                      onError={(e) => {
                        e.currentTarget.style.display = 'none';
                        e.currentTarget.parentElement?.querySelector('.logo-fallback')?.classList.remove('hidden');
                      }}
                    />
                  ) : null}
                  <div className={`logo-fallback text-center text-slate-400 ${logoPreviewUrl ? 'hidden' : ''}`}>
                    <Sparkles className="h-6 w-6 mx-auto mb-1 text-slate-300" />
                    <span className="text-[10px]">No logo uploaded</span>
                  </div>
                </div>

                <label className="cursor-pointer inline-flex items-center justify-center gap-1.5 px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-semibold rounded border border-indigo-200 transition-colors">
                  <Upload className="h-3.5 w-3.5" />
                  <span>{logoUploading ? 'Uploading...' : 'Choose Logo File'}</span>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleLogoUpload}
                    disabled={logoUploading}
                    className="hidden"
                  />
                </label>
              </div>

              {/* Signature Box */}
              <div className="p-4 border border-slate-200 rounded-lg bg-slate-50/50 flex flex-col justify-between space-y-3">
                <div>
                  <span className="text-xs font-semibold text-slate-800 block">Principal / Authority Signature</span>
                  <span className="text-[11px] text-slate-500">Transparent PNG signature asset</span>
                </div>

                <div className="h-24 bg-white border border-dashed border-slate-300 rounded flex items-center justify-center p-2 overflow-hidden relative">
                  {signaturePreviewUrl ? (
                    <img
                      src={signaturePreviewUrl}
                      alt="Signature Preview"
                      className="h-full object-contain"
                      onError={(e) => {
                        e.currentTarget.style.display = 'none';
                        e.currentTarget.parentElement?.querySelector('.sig-fallback')?.classList.remove('hidden');
                      }}
                    />
                  ) : null}
                  <div className={`sig-fallback text-center text-slate-400 ${signaturePreviewUrl ? 'hidden' : ''}`}>
                    <PenTool className="h-6 w-6 mx-auto mb-1 text-slate-300" />
                    <span className="text-[10px]">No signature uploaded</span>
                  </div>
                </div>

                <label className="cursor-pointer inline-flex items-center justify-center gap-1.5 px-3 py-1.5 bg-purple-50 hover:bg-purple-100 text-purple-700 text-xs font-semibold rounded border border-purple-200 transition-colors">
                  <Upload className="h-3.5 w-3.5" />
                  <span>{sigUploading ? 'Uploading...' : 'Choose Signature File'}</span>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleSignatureUpload}
                    disabled={sigUploading}
                    className="hidden"
                  />
                </label>
              </div>
            </div>

            <div className="mt-4 pt-3 border-t border-slate-100">
              <Input
                label="Signatory Full Name & Title"
                placeholder="e.g. Dr. Rajesh Verma (Principal)"
                value={form.principalName}
                onChange={(e) => setForm({ ...form, principalName: e.target.value })}
              />
            </div>
          </Card>

          {/* Card 3: Contact & Campus Address */}
          <Card title="Official Contact & Campus Location">
            <div className="space-y-4 pt-1">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Input
                  label="Official Contact Email"
                  type="email"
                  required
                  placeholder="office@greenwood.edu"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                />
                <Input
                  label="Official Telephone / Mobile"
                  placeholder="9876543210"
                  value={form.mobile}
                  onChange={(e) => setForm({ ...form, mobile: e.target.value })}
                />
              </div>

              <Input
                label="Campus Street Address"
                placeholder="e.g. 14 Heritage Boulevard, Indiranagar"
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
              />

              <div className="grid grid-cols-3 gap-3">
                <Input
                  label="City"
                  placeholder="Bengaluru"
                  value={form.city}
                  onChange={(e) => setForm({ ...form, city: e.target.value })}
                />
                <Input
                  label="State"
                  placeholder="Karnataka"
                  value={form.state}
                  onChange={(e) => setForm({ ...form, state: e.target.value })}
                />
                <Input
                  label="Pincode"
                  placeholder="560038"
                  value={form.pincode}
                  onChange={(e) => setForm({ ...form, pincode: e.target.value })}
                />
              </div>
            </div>
          </Card>

          {/* Action Button */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="secondary"
              onClick={() => refetch()}
              disabled={saveMutation.isPending}
            >
              Reset Changes
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={saveMutation.isPending}
              className="flex items-center gap-1.5"
            >
              <Save className="h-4 w-4" />
              <span>Save School Settings</span>
            </Button>
          </div>
        </form>

        {/* Right Preview: Live ID Card Auto-Populate Demonstration (5 cols) */}
        <div className="lg:col-span-5 space-y-4 sticky top-6">
          <Card
            title="Auto-Populate Live Card Preview"
            subtitle="Demonstrating how your school data, logo, and signature automatically render onto student cards."
          >
            <div className="bg-slate-50 p-2 sm:p-4 rounded-lg border border-slate-200 flex flex-col items-center justify-center">
              <CardCanvas
                widthMm={85.6}
                heightMm={54.0}
                bleedMarginMm={3.0}
                showBleed={false}
                elements={PROFILE_PREVIEW_ELEMENTS}
                interactive={false}
                previewData={livePreviewData}
                scale={4.2}
              />
            </div>

            <div className="mt-4 pt-3 border-t border-slate-100 space-y-2 text-xs text-slate-600">
              <div className="flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-indigo-600 shrink-0" />
                <span>
                  <strong>Institution Name & Logo:</strong> Sourced live from your branding settings.
                </span>
              </div>
              <div className="flex items-center gap-2">
                <PenTool className="h-4 w-4 text-purple-600 shrink-0" />
                <span>
                  <strong>Authorized Signature:</strong> Automatically applied to valid ID card prints.
                </span>
              </div>
              <div className="flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-emerald-600 shrink-0" />
                <span>
                  <strong>Tamper-Proof Isolation:</strong> Agency fulfillment partners receive print-ready vector packages without direct access to school credentials.
                </span>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
