export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  errors?: string[];
}

export interface GameInfo {
  name: string;
  version: string;
  description: string;
}

export interface HealthStatus {
  status: string;
  message: string;
  timestamp: string;
}

export interface User {
  id: number;
  username: string;
  email: string;
  createdAt: string;
  role: string;
  isActive: boolean;
}

export interface Company {
  id: number;
  name: string;
  budget: number;
  employeeCount: number;
}