import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../components/common/Button';
import Input from '../components/common/Input';
import Modal from '../components/common/Modal';
import { useAuthStore } from '../store/useAuthStore';
import api from '../services/api';
import { Shield, School, Printer, AlertCircle, UserPlus, CheckCircle2 } from 'lucide-react';

export default function LoginPage() {
  const navigate = useNavigate();
  const { isAuthenticated, user, setAuth } = useAuthStore();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Self-Registration Modal state
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [regLoading, setRegLoading] = useState(false);
  const [regError, setRegError] = useState('');
  const [regSuccess, setRegSuccess] = useState('');
  const [regForm, setRegForm] = useState({
    schoolName: '',
    schoolCode: '',
    address: '',
    city: '',
    state: '',
    pincode: '',
    schoolEmail: '',
    schoolMobile: '',
    adminFullName: '',
    adminEmail: '',
    adminPassword: '',
    adminMobile: '',
    adminDesignation: 'Principal',
  });

  useEffect(() => {
    if (isAuthenticated && user) {
      if (user.role === 'SUPER_ADMIN') {
        navigate('/admin', { replace: true });
      } else if (user.role === 'SCHOOL') {
        navigate('/school', { replace: true });
      } else if (user.role === 'PRINTING_AGENCY') {
        navigate('/agency', { replace: true });
      }
    }
  }, [isAuthenticated, user, navigate]);

  const handleLogin = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setLoading(true);

    try {
      const response = await api.post('/auth/login', {
        email: email.trim(),
        password,
      });

      const { accessToken, refreshToken, user } = response.data;
      setAuth(user, accessToken, refreshToken);

      if (user.role === 'SUPER_ADMIN') {
        navigate('/admin');
      } else if (user.role === 'SCHOOL') {
        navigate('/school');
      } else if (user.role === 'PRINTING_AGENCY') {
        navigate('/agency');
      } else {
        navigate('/admin');
      }
    } catch (err) {
      setErrorMessage(err.message || 'Invalid email or password.');
    } finally {
      setLoading(false);
    }
  };

  const fillQuickCredentials = (role) => {
    setErrorMessage('');
    if (role === 'SUPER_ADMIN') {
      setEmail('admin@platform.com');
      setPassword('Password123!');
    } else if (role === 'SCHOOL') {
      setEmail('school@greenwood.edu');
      setPassword('Password123!');
    } else if (role === 'PRINTING_AGENCY') {
      setEmail('agency@apexprints.com');
      setPassword('Password123!');
    }
  };

  const handleRegisterSchool = async (e) => {
    e.preventDefault();
    setRegError('');
    setRegLoading(true);

    try {
      const res = await api.post('/auth/register-school', {
        schoolName: regForm.schoolName.trim(),
        schoolCode: regForm.schoolCode.trim().toUpperCase(),
        address: regForm.address.trim(),
        city: regForm.city.trim(),
        state: regForm.state.trim(),
        pincode: regForm.pincode.trim(),
        schoolEmail: regForm.schoolEmail.trim(),
        schoolMobile: regForm.schoolMobile.trim(),
        adminFullName: regForm.adminFullName.trim(),
        adminEmail: regForm.adminEmail.trim(),
        adminPassword: regForm.adminPassword,
        adminMobile: regForm.adminMobile.trim() || undefined,
        adminDesignation: regForm.adminDesignation.trim() || 'Principal',
      });

      const { accessToken, refreshToken, user } = res.data;
      setRegSuccess('School registration successful! Logging you in...');
      setTimeout(() => {
        setAuth(user, accessToken, refreshToken);
        setIsRegisterOpen(false);
        navigate('/school');
      }, 1200);
    } catch (err) {
      setRegError(err.message || 'Failed to register school. Please verify all fields.');
    } finally {
      setRegLoading(false);
    }
  };

  return (
    <>
      <form onSubmit={handleLogin} className="space-y-5">
        <div>
          <h3 className="text-sm font-semibold text-slate-900 mb-1">Sign in to your account</h3>
          <p className="text-xs text-slate-500">
            Enter your registered email and password to access your role-based portal.
          </p>
        </div>

        {errorMessage && (
          <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 flex items-start gap-2 text-rose-700 text-xs">
            <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
            <span>{errorMessage}</span>
          </div>
        )}

        <Input
          label="Email address"
          id="login-email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="e.g. admin@platform.com"
          autoComplete="email"
        />

        <Input
          label="Password"
          id="login-password"
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
          autoComplete="current-password"
        />

        <Button type="submit" loading={loading} className="w-full">
          Sign In
        </Button>

        {/* Quick Demo Credentials for Evaluation */}
        <div className="pt-4 border-t border-slate-100 space-y-2">
          <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
            Quick 1-Click Demo Credentials
          </p>
          <div className="grid grid-cols-1 gap-2">
            <button
              type="button"
              onClick={() => fillQuickCredentials('SUPER_ADMIN')}
              className="w-full text-left text-xs text-indigo-700 hover:text-indigo-900 bg-indigo-50/70 hover:bg-indigo-100/70 p-2.5 rounded-lg border border-indigo-200/80 flex items-center justify-between transition-colors"
            >
              <div className="flex items-center gap-2">
                <Shield className="h-4 w-4 text-indigo-600" />
                <div>
                  <div className="font-semibold text-slate-800">Super Admin</div>
                  <div className="font-mono text-[11px] text-slate-500">admin@platform.com</div>
                </div>
              </div>
              <span className="text-[10px] font-medium bg-indigo-200/80 text-indigo-800 px-2 py-0.5 rounded">Auto-fill</span>
            </button>

            <button
              type="button"
              onClick={() => fillQuickCredentials('SCHOOL')}
              className="w-full text-left text-xs text-purple-700 hover:text-purple-900 bg-purple-50/70 hover:bg-purple-100/70 p-2.5 rounded-lg border border-purple-200/80 flex items-center justify-between transition-colors"
            >
              <div className="flex items-center gap-2">
                <School className="h-4 w-4 text-purple-600" />
                <div>
                  <div className="font-semibold text-slate-800">Greenwood School</div>
                  <div className="font-mono text-[11px] text-slate-500">school@greenwood.edu</div>
                </div>
              </div>
              <span className="text-[10px] font-medium bg-purple-200/80 text-purple-800 px-2 py-0.5 rounded">Auto-fill</span>
            </button>

            <button
              type="button"
              onClick={() => fillQuickCredentials('PRINTING_AGENCY')}
              className="w-full text-left text-xs text-amber-700 hover:text-amber-900 bg-amber-50/70 hover:bg-amber-100/70 p-2.5 rounded-lg border border-amber-200/80 flex items-center justify-between transition-colors"
            >
              <div className="flex items-center gap-2">
                <Printer className="h-4 w-4 text-amber-600" />
                <div>
                  <div className="font-semibold text-slate-800">Apex Card Printers</div>
                  <div className="font-mono text-[11px] text-slate-500">agency@apexprints.com</div>
                </div>
              </div>
              <span className="text-[10px] font-medium bg-amber-200/80 text-amber-800 px-2 py-0.5 rounded">Auto-fill</span>
            </button>
          </div>
        </div>

        {/* Public Self-Registration for New Schools */}
        <div className="pt-2 text-center">
          <p className="text-xs text-slate-500">
            Are you a new educational institution?{' '}
            <button
              type="button"
              onClick={() => {
                setRegError('');
                setRegSuccess('');
                setIsRegisterOpen(true);
              }}
              className="font-semibold text-indigo-600 hover:text-indigo-800 underline underline-offset-2 ml-1"
            >
              Register your school
            </button>
          </p>
        </div>
      </form>

      {/* School Self-Registration Modal */}
      <Modal
        isOpen={isRegisterOpen}
        onClose={() => setIsRegisterOpen(false)}
        title="Register New School Institution"
        maxWidth="max-w-2xl"
        footer={
          <>
            <Button variant="secondary" onClick={() => setIsRegisterOpen(false)} disabled={regLoading}>
              Cancel
            </Button>
            <Button variant="primary" onClick={handleRegisterSchool} loading={regLoading}>
              Complete Registration
            </Button>
          </>
        }
      >
        <form onSubmit={handleRegisterSchool} className="space-y-4">
          {regError && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-md text-xs text-rose-700 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{regError}</span>
            </div>
          )}

          {regSuccess && (
            <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-md text-xs text-emerald-700 flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-600" />
              <span>{regSuccess}</span>
            </div>
          )}

          <div className="border-b border-slate-200 pb-2">
            <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Institution Details</h4>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <Input
              label="School Name"
              required
              placeholder="e.g. Cambridge High School"
              value={regForm.schoolName}
              onChange={(e) => setRegForm({ ...regForm, schoolName: e.target.value })}
            />
            <Input
              label="School Code (Short identifier)"
              required
              placeholder="e.g. CHS-BLR"
              value={regForm.schoolCode}
              onChange={(e) => setRegForm({ ...regForm, schoolCode: e.target.value })}
            />
          </div>

          <Input
            label="Campus Address"
            required
            placeholder="e.g. 15 Orchard Road, Tech Zone"
            value={regForm.address}
            onChange={(e) => setRegForm({ ...regForm, address: e.target.value })}
          />

          <div className="grid grid-cols-3 gap-3">
            <Input
              label="City"
              required
              placeholder="e.g. Bengaluru"
              value={regForm.city}
              onChange={(e) => setRegForm({ ...regForm, city: e.target.value })}
            />
            <Input
              label="State"
              required
              placeholder="e.g. Karnataka"
              value={regForm.state}
              onChange={(e) => setRegForm({ ...regForm, state: e.target.value })}
            />
            <Input
              label="Pincode"
              required
              placeholder="e.g. 560001"
              value={regForm.pincode}
              onChange={(e) => setRegForm({ ...regForm, pincode: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <Input
              label="School Official Email"
              type="email"
              required
              placeholder="office@cambridge.edu"
              value={regForm.schoolEmail}
              onChange={(e) => setRegForm({ ...regForm, schoolEmail: e.target.value })}
            />
            <Input
              label="School Phone"
              required
              placeholder="9876543210"
              value={regForm.schoolMobile}
              onChange={(e) => setRegForm({ ...regForm, schoolMobile: e.target.value })}
            />
          </div>

          <div className="border-b border-slate-200 pb-2 pt-2">
            <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Primary Admin Credentials</h4>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <Input
              label="Administrator Full Name"
              required
              placeholder="e.g. Dr. Jennifer Adams"
              value={regForm.adminFullName}
              onChange={(e) => setRegForm({ ...regForm, adminFullName: e.target.value })}
            />
            <Input
              label="Designation"
              placeholder="e.g. Principal / Administrator"
              value={regForm.adminDesignation}
              onChange={(e) => setRegForm({ ...regForm, adminDesignation: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <Input
              label="Login Email Address"
              type="email"
              required
              placeholder="admin@cambridge.edu"
              value={regForm.adminEmail}
              onChange={(e) => setRegForm({ ...regForm, adminEmail: e.target.value })}
            />
            <Input
              label="Login Password (Min 8 chars, 1 uppercase, 1 digit, 1 special)"
              type="password"
              required
              placeholder="••••••••"
              value={regForm.adminPassword}
              onChange={(e) => setRegForm({ ...regForm, adminPassword: e.target.value })}
            />
          </div>
        </form>
      </Modal>
    </>
  );
}
