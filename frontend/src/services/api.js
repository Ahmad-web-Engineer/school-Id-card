import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
  timeout: 90000,
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
  async (error) => {
    let errorEnvelope = {
      success: false,
      errorCode: error.code || 'UNKNOWN_ERROR',
      message: error.message || 'An unexpected network error occurred.',
      timestamp: new Date().toISOString(),
    };

    if (error.response?.data) {
      if (error.response.data instanceof Blob) {
        try {
          const text = await error.response.data.text();
          const json = JSON.parse(text);
          if (json && json.message) {
            errorEnvelope = json;
          }
        } catch (ignored) {
          errorEnvelope.message = `Request failed with status ${error.response.status}`;
        }
      } else if (typeof error.response.data === 'object') {
        errorEnvelope = error.response.data;
      }
    }

    return Promise.reject(errorEnvelope);
  }
);

export default api;
