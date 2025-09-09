import axios, { type AxiosResponse } from 'axios';
import type { 
  ApiResponse, 
  GameInfo, 
  HealthStatus, 
  User,
  GameSessionResponse,
  GameInitializationResponse,
  EmployeeResponse,
  ContractResponse,
  WeekTurnResponse,
  StartGameRequest,
  HireEmployeeRequest
} from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Create axios instance with default config (cookies will be sent automatically)
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Important for sending cookies
});

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized access
      console.warn('Unauthorized request');
    }
    return Promise.reject(error);
  }
);

export class ApiService {
  // Test endpoints
  static async getHealth(): Promise<HealthStatus> {
    const response: AxiosResponse<HealthStatus> = await apiClient.get('/health');
    return response.data;
  }

  static async getGameInfo(): Promise<GameInfo> {
    const response: AxiosResponse<GameInfo> = await apiClient.get('/game/info');
    return response.data;
  }

  // Legacy user endpoint (keeping for compatibility)
  static async registerUser(userData: {
    username: string;
    email: string;
    password: string;
  }): Promise<ApiResponse<User>> {
    const response: AxiosResponse<ApiResponse<User>> = await apiClient.post('/users/register', userData);
    return response.data;
  }

  // Game Management
  static async startGame(companyName: string): Promise<ApiResponse<GameInitializationResponse>> {
    const request: StartGameRequest = { companyName };
    const response: AxiosResponse<ApiResponse<GameInitializationResponse>> = 
      await apiClient.post('/game/start', request);
    return response.data;
  }

  static async getCurrentGame(): Promise<ApiResponse<GameSessionResponse | null>> {
    const response: AxiosResponse<ApiResponse<GameSessionResponse | null>> = 
      await apiClient.get('/game/current');
    return response.data;
  }

  static async endWeek(): Promise<ApiResponse<WeekTurnResponse>> {
    const response: AxiosResponse<ApiResponse<WeekTurnResponse>> = 
      await apiClient.get('/game/nextturn');
    return response.data;
  }

  // Employee Management
  static async getEmployees(): Promise<ApiResponse<EmployeeResponse[]>> {
    const response: AxiosResponse<ApiResponse<EmployeeResponse[]>> = 
      await apiClient.get('/employees');
    return response.data;
  }

  static async hireEmployee(employeeData: HireEmployeeRequest): Promise<ApiResponse<EmployeeResponse>> {
    const response: AxiosResponse<ApiResponse<EmployeeResponse>> = 
      await apiClient.post('/employees/hire', employeeData);
    return response.data;
  }

  // Contract Management
  static async getAvailableContracts(): Promise<ApiResponse<ContractResponse[]>> {
    const response: AxiosResponse<ApiResponse<ContractResponse[]>> = 
      await apiClient.get('/contracts/available');
    return response.data;
  }

  static async acceptContract(contractId: number): Promise<ApiResponse<ContractResponse>> {
    const response: AxiosResponse<ApiResponse<ContractResponse>> = 
      await apiClient.post(`/contracts/${contractId}/accept`);
    return response.data;
  }

  static async endGame(): Promise<ApiResponse<string>> {
    const response: AxiosResponse<ApiResponse<string>> = 
      await apiClient.get('/game/end');
    return response.data;
  }

  static async getAvailableEmployees(): Promise<ApiResponse<EmployeeResponse[]>> {
    const response: AxiosResponse<ApiResponse<EmployeeResponse[]>> = 
      await apiClient.get('/employees/available');
    return response.data;
  }

  static async getContracts(): Promise<ApiResponse<ContractResponse[]>> {
    const response: AxiosResponse<ApiResponse<ContractResponse[]>> = 
      await apiClient.get('/contracts/active');
    return response.data;
  }

  static async assignEmployeeToContract(contractId: number, employeeId: number): Promise<ApiResponse<string>> {
    const response: AxiosResponse<ApiResponse<string>> = 
      await apiClient.post(`/contracts/${contractId}/assign/${employeeId}`);
    return response.data;
  }
}