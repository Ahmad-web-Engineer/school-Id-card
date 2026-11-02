import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Menu, X, LogOut, ChevronRight, Shield, School, Printer, User } from 'lucide-react';
import Badge from '../common/Badge';
import NotificationBell from '../../features/notifications/NotificationBell';
import { useAuthStore } from '../../store/useAuthStore';

const roleMeta = {
  SUPER_ADMIN: {
    title: 'Platform Admin',
    badge: 'Super Admin',
    badgeVariant: 'brand',
    icon: Shield,
    home: '/admin',
  },
  SCHOOL: {
    title: 'School Portal',
    badge: 'School',
    badgeVariant: 'info',
    icon: School,
    home: '/school',
  },
  PRINTING_AGENCY: {
    title: 'Agency Portal',
    badge: 'Agency',
    badgeVariant: 'warning',
    icon: Printer,
    home: '/agency',
  },
};

export default function PortalLayout({ role = 'SUPER_ADMIN', navItems = [], children }) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const { user, clearAuth } = useAuthStore();
  const currentRole = roleMeta[role] || roleMeta.SUPER_ADMIN;
  const RoleIcon = currentRole.icon;

  // Auto-close drawer when route changes
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  // Close drawer on Escape key
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') setMobileMenuOpen(false);
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const handleLogout = () => {
    clearAuth();
    navigate('/login', { replace: true });
  };

  return (
    <div className="min-h-full flex flex-col md:flex-row bg-slate-50">
      {/* Mobile top bar (< 768px) */}
      <header className="md:hidden flex items-center justify-between bg-white border-b border-slate-200 px-4 py-3 sticky top-0 z-30">
        <div className="flex items-center gap-2">
          <RoleIcon className="h-5 w-5 text-indigo-600" />
          <span className="font-bold text-sm text-slate-900 tracking-tight">{currentRole.title}</span>
          <Badge variant={currentRole.badgeVariant} size="sm">{currentRole.badge}</Badge>
        </div>
        <div className="flex items-center gap-2">
          <NotificationBell />
          <button
            type="button"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle navigation menu"
            className="p-1.5 rounded-md text-slate-600 hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            {mobileMenuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </button>
        </div>
      </header>

      {/* Mobile Backdrop Overlay */}
      {mobileMenuOpen && (
        <div
          className="fixed inset-0 z-30 bg-slate-900/60 backdrop-blur-xs md:hidden"
          onClick={() => setMobileMenuOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* Sidebar for Desktop & Mobile Overlay */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-64 bg-slate-900 text-slate-100 transform transition-transform duration-200 ease-in-out md:translate-x-0 md:static md:flex md:flex-col md:h-screen ${
          mobileMenuOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0 hidden md:flex'
        }`}
      >
        {/* Brand / Logo Header */}
        <div className="h-16 flex items-center justify-between px-6 border-b border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="h-8 w-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white font-bold text-sm shadow-md">
              ID
            </div>
            <div>
              <div className="font-semibold text-sm leading-tight text-white">ID Card Platform</div>
              <div className="text-[11px] text-slate-400">{currentRole.title}</div>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setMobileMenuOpen(false)}
            className="md:hidden text-slate-400 hover:text-white p-1"
            aria-label="Close menu"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto" aria-label="Sidebar Navigation">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            const ItemIcon = item.icon;
            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setMobileMenuOpen(false)}
                className={`flex items-center gap-3 px-3 py-2 rounded-md text-xs font-medium transition-colors ${
                  isActive
                    ? 'bg-indigo-600 text-white shadow-sm'
                    : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                }`}
              >
                {ItemIcon && <ItemIcon className="h-4 w-4 shrink-0" />}
                <span className="flex-1 truncate">{item.label}</span>
                {isActive && <ChevronRight className="h-3.5 w-3.5 text-indigo-200" />}
              </Link>
            );
          })}
        </nav>

        {/* User / Logout Footer */}
        <div className="p-4 border-t border-slate-800 flex items-center justify-between bg-slate-950/40">
          <div className="flex items-center gap-2.5 truncate">
            <div className="h-8 w-8 rounded-full bg-indigo-700/80 border border-indigo-500/30 flex items-center justify-center text-xs font-semibold text-white shrink-0">
              {user?.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
            </div>
            <div className="truncate">
              <div className="text-xs font-medium text-white truncate">{user?.fullName || 'Authorized User'}</div>
              <div className="text-[10px] text-slate-400 truncate">{user?.email || currentRole.badge}</div>
            </div>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            title="Log out"
            aria-label="Log out"
            className="p-1.5 text-slate-400 hover:text-rose-400 rounded-md hover:bg-slate-800 transition-colors shrink-0"
          >
            <LogOut className="h-4 w-4" />
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 md:h-screen md:overflow-y-auto">
        {/* Desktop Header */}
        <header className="hidden md:flex items-center justify-between h-16 bg-white border-b border-slate-200 px-8 sticky top-0 z-20">
          <div className="flex items-center gap-3">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Section</span>
            <span className="text-sm font-semibold text-slate-800">
              {navItems.find((n) => n.path === location.pathname)?.label || 'Overview'}
            </span>
          </div>
          <div className="flex items-center gap-4">
            <NotificationBell />
            <div className="flex items-center gap-2.5 pl-4 border-l border-slate-200">
              <div className="text-right">
                <div className="text-xs font-medium text-slate-800">{user?.fullName || 'Authorized User'}</div>
                <div className="text-[10px] text-slate-400">{currentRole.badge}</div>
              </div>
              <button
                type="button"
                onClick={handleLogout}
                title="Log out"
                aria-label="Log out"
                className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
              >
                <LogOut className="h-4 w-4" />
              </button>
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto" tabIndex="-1">
          {children}
        </main>
      </div>
    </div>
  );
}
