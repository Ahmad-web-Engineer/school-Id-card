import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../components/common/Button';
import Input from '../components/common/Input';
import { useAuthStore } from '../store/useAuthStore';
import api from '../services/api';
import { Shield, School, Printer, AlertCircle } from 'lucide-react';

export default function LoginPage() {
  const navigate = useNavigate();
  const { isAuthenticated, user, setAuth } = useAuthStore();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

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
        navigate('/design-system');
      }
    } catch (err) {
      setErrorMessage(err.message || 'Invalid email or password.');
    } finally {
      setLoading(false);
    }
  };

  const fillQuickCredentials = (role) => {
    setErrorMessage('');
    if (role === 'ADMIN') {
      setEmail('admin@platform.com');
      setPassword('Password123!');
    }
  };

  return (
    <form onSubmit={handleLogin} className="space-y-5">
      <div>
        <h3 className="text-sm font-semibold text-slate-900 mb-1">Sign in to your account</h3>
        <p className="text-xs text-slate-500">
          Enter your registered email and password to access your portal.
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

      {/* Preset demo credentials helper */}
      <div className="pt-3 border-t border-slate-100">
        <p className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-2">
          Demo Default Credentials
        </p>
        <button
          type="button"
          onClick={() => fillQuickCredentials('ADMIN')}
          className="w-full text-left text-xs text-indigo-600 hover:text-indigo-800 bg-indigo-50/60 hover:bg-indigo-50 p-2 rounded border border-indigo-100 flex items-center justify-between"
        >
          <span>Super Admin: <code className="font-mono text-slate-700">admin@platform.com</code></span>
          <span className="text-[10px] bg-indigo-200/60 px-1.5 py-0.5 rounded text-indigo-800">Auto-fill</span>
        </button>
      </div>
    </form>
  );
}
