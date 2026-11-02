import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
  timeout: 15000,
});

// Request interceptor: attach auth token if available
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: unwrap response data or extract standard error shape
api.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    const errorEnvelope = error.response?.data || {
      success: false,
      errorCode: error.code || 'UNKNOWN_ERROR',
      message: error.message || 'An unexpected network error occurred.',
      timestamp: new Date().toISOString(),
    };
    return Promise.reject(errorEnvelope);
  }
);

export default api;
