import axios, { type AxiosResponse } from 'axios';
import type { ApiResponse, GameInfo, HealthStatus, User } from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Create axios instance with default config
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for adding auth tokens later
apiClient.interceptors.request.use(
  (config) => {
    // Add auth token here when implemented
    // const token = localStorage.getItem('authToken');
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`;
    // }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized access
      // redirect to login
    }
    return Promise.reject(error);
  }
);

export class ApiService {
  static async getHealth(): Promise<HealthStatus> {
    const response: AxiosResponse<HealthStatus> = await apiClient.get('/health');
    return response.data;
  }

  static async getGameInfo(): Promise<GameInfo> {
    const response: AxiosResponse<GameInfo> = await apiClient.get('/game/info');
    return response.data;
  }

  static async registerUser(userData: {
    username: string;
    email: string;
    password: string;
  }): Promise<ApiResponse<User>> {
    const response: AxiosResponse<ApiResponse<User>> = await apiClient.post('/users/register', userData);
    return response.data;
  }
}