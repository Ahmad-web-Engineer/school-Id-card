import { create } from 'zustand';

const storedUser = localStorage.getItem('auth_user');

export const useAuthStore = create((set) => ({
  user: storedUser ? JSON.parse(storedUser) : null,
  token: localStorage.getItem('auth_token') || null,
  refreshToken: localStorage.getItem('auth_refresh_token') || null,
  isAuthenticated: !!localStorage.getItem('auth_token'),

  setAuth: (user, token, refreshToken) => {
    localStorage.setItem('auth_token', token);
    if (refreshToken) {
      localStorage.setItem('auth_refresh_token', refreshToken);
    }
    localStorage.setItem('auth_user', JSON.stringify(user));
    set({ user, token, refreshToken, isAuthenticated: true });
  },

  clearAuth: () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_refresh_token');
    localStorage.removeItem('auth_user');
    set({ user: null, token: null, refreshToken: null, isAuthenticated: false });
  },
}));
