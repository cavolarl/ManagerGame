// Base API Response wrapper
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  errors?: string[];
}

// Test endpoints (existing)
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

// Legacy types (keeping for compatibility)
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

// Game Session
export interface GameSessionResponse {
  id: number;
  companyName: string;
  currentQuarter: number;
  currentWeek: number;
  budget: number;
  stakeholderValue: number;
  errorPenalties: number;
  status: string;
  startedAt: string;
  endedAt: string | null;
  totalScore: number;
  isQuarterEnd: boolean;
}

// Game Initialization
export interface GameInitializationResponse {
  gameSession: GameSessionResponse;
  availableEmployees: EmployeeResponse[];
  availableContracts: ContractResponse[];
}

// Employee
export interface EmployeeResponse {
  id: number;
  name: string;
  employeeType: string;
  level: number;
  speed: number;
  accuracy: number;
  salary: number;
  morale: number;
  isActive: boolean;
  gameSessionId: number;
  effectiveSpeed: number;
  effectiveAccuracy: number;
  quitChance: number;
}

// Contract
export interface ContractResponse {
  id: number;
  title: string;
  description: string;
  difficulty: string; // EASY, MEDIUM, HARD
  totalWorkRequired: number;
  currentProgress: number;
  baseReward: number;
  stakeholderPoints: number;
  deadlineWeeks: number;
  weeksRemaining: number;
  status: string; // AVAILABLE, ACTIVE, COMPLETED, FAILED
}

// Week Turn Results
export interface WeekTurnResponse {
  gameSession: GameSessionResponse;
  contractResults: ContractResponse[];
  quitEmployees: EmployeeResponse[];
  completedContracts: ContractResponse[];
}

// Request DTOs
export interface StartGameRequest {
  companyName: string;
}

export interface HireEmployeeRequest {
  name: string;
  employeeType: string;
  level: number;
  speed: number;
  accuracy: number;
  salary: number;
  morale: number;
}