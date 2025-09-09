import React, { useState, useEffect } from 'react';
import { ApiService } from '../services/api';
import { LoadingSpinner } from './LoadingSpinner';
import type { 
  GameSessionResponse, 
  EmployeeResponse, 
  ContractResponse, 
  GameInitializationResponse,
  WeekTurnResponse 
} from '../types/api';

interface GameState {
  gameSession: GameSessionResponse | null;
  employees: EmployeeResponse[];
  availableContracts: ContractResponse[];
  activeContracts: ContractResponse[];
  availableEmployees: EmployeeResponse[];
  weekTurnResults: WeekTurnResponse | null;
}

const GameManager: React.FC = () => {
  const [gameState, setGameState] = useState<GameState>({
    gameSession: null,
    employees: [],
    availableContracts: [],
    activeContracts: [],
    availableEmployees: [],
    weekTurnResults: null
  });
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [companyName, setCompanyName] = useState('');
  const [showStartForm, setShowStartForm] = useState(false);
  const [processingWeek, setProcessingWeek] = useState(false);

  // Load existing game on component mount
  useEffect(() => {
    loadCurrentGame();
  }, []);

  const loadCurrentGame = async () => {
    try {
      setLoading(true);
      const response = await ApiService.getCurrentGame();
      
      if (response.success && response.data) {
        setGameState(prev => ({ ...prev, gameSession: response.data! }));
        await loadGameData();
      } else {
        setShowStartForm(true);
      }
    } catch (err) {
      setError('Failed to load game');
    } finally {
      setLoading(false);
    }
  };

  const loadGameData = async () => {
    try {
      const [employeesResponse, contractsResponse] = await Promise.all([
        ApiService.getEmployees(),
        ApiService.getAvailableContracts()
      ]);

      const employees = employeesResponse.success ? employeesResponse.data || [] : [];
      const allContracts = contractsResponse.success ? contractsResponse.data || [] : [];
      const availableContracts = allContracts.filter(contract => contract.status === 'AVAILABLE');

      setGameState(prev => ({
        ...prev,
        employees,
        availableContracts
      }));
    } catch (err) {
      setError('Failed to load game data');
    }
  };

  const startNewGame = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!companyName.trim()) return;

    try {
      setLoading(true);
      const response = await ApiService.startGame(companyName);
      
      if (response.success && response.data) {
        const initData: GameInitializationResponse = response.data;
        setGameState({
          gameSession: initData.gameSession,
          employees: [],
          availableContracts: initData.availableContracts,
          activeContracts: [],
          availableEmployees: initData.availableEmployees,
          weekTurnResults: null
        });
        setShowStartForm(false);
        await loadGameData();
      } else {
        setError(response.message || 'Failed to start game');
      }
    } catch (err) {
      setError('Failed to start game');
    } finally {
      setLoading(false);
    }
  };

  const nextWeek = async () => {
    if (!gameState.gameSession) return;
    
    try {
      setProcessingWeek(true);
      const response = await ApiService.endWeek();
      
      if (response.success && response.data) {
        setGameState(prev => ({
          ...prev,
          gameSession: response.data!.gameSession,
          weekTurnResults: response.data!
        }));
        await loadGameData();
      } else {
        setError(response.message || 'Failed to process week');
      }
    } catch (err) {
      setError('Failed to process week');
    } finally {
      setProcessingWeek(false);
    }
  };

  const endGame = async () => {
    if (!confirm('Are you sure you want to end this game?')) return;

    try {
      setLoading(true);
      const response = await ApiService.endGame();
      
      if (response.success) {
        alert(`Game ended! ${response.data}`);
        setGameState({
          gameSession: null,
          employees: [],
          availableContracts: [],
          activeContracts: [],
          availableEmployees: [],
          weekTurnResults: null
        });
        setShowStartForm(true);
      } else {
        setError(response.message || 'Failed to end game');
      }
    } catch (err) {
      setError('Failed to end game');
    } finally {
      setLoading(false);
    }
  };

  const acceptContract = async (contractId: number) => {
    try {
      console.log('=== ACCEPTING CONTRACT ===');
      console.log('Contract ID:', contractId);
      
      const response = await ApiService.acceptContract(contractId);
      console.log('Accept contract response:', response);
      
      if (response.success) {
        console.log('Contract accepted successfully, refreshing data...');
        // Refresh contracts to move the accepted contract from available to active
        await loadGameData();
      } else {
        console.error('Failed to accept contract:', response.message);
        setError(response.message || 'Failed to accept contract');
      }
    } catch (err) {
      console.error('Error accepting contract:', err);
      setError('Failed to accept contract');
    }
  };

  const hireEmployee = async (employee: EmployeeResponse) => {
    try {
      const response = await ApiService.hireEmployee({
        name: employee.name,
        employeeType: employee.employeeType,
        level: employee.level,
        speed: employee.speed,
        accuracy: employee.accuracy,
        salary: employee.salary,
        morale: employee.morale
      });

      if (response.success) {
        // Remove from available pool and refresh
        setGameState(prev => ({
          ...prev,
          availableEmployees: prev.availableEmployees.filter(emp => emp.id !== employee.id)
        }));
        await Promise.all([loadCurrentGame(), loadGameData()]);
      } else {
        setError(response.message || 'Failed to hire employee');
      }
    } catch (err) {
      setError('Failed to hire employee');
    }
  };

  const assignEmployee = async (contractId: number, employeeId: number) => {
    try {
      console.log(`Assigning employee ${employeeId} to contract ${contractId}`);
      const response = await ApiService.assignEmployeeToContract(contractId, employeeId);
      if (response.success) {
        console.log('Employee assigned successfully, refreshing data...');
        // Refresh game data to reflect assignment (e.g., employee status, contract progress)
        await loadGameData();
        // Optionally, you might want to specifically update the assigned employee's status or the contract's assigned employees list
      } else {
        console.error('Failed to assign employee:', response.message);
        setError(response.message || 'Failed to assign employee');
      }
    } catch (err) {
      setError('Failed to assign employee');
    }
  };

  const dismissResults = () => {
    setGameState(prev => ({ ...prev, weekTurnResults: null }));
  };

  const calculateWeekProgress = () => {
    if (!gameState.gameSession) return 0;
    const { currentWeek } = gameState.gameSession;
    return Math.round((currentWeek / 13) * 100); // 13 weeks per quarter
  };

  // Loading state
  if (loading && !gameState.gameSession) {
    return <LoadingSpinner size="lg" message="Loading game..." />;
  }

  // Start game form
  if (showStartForm) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-lg p-8 max-w-md w-full">
          <h1 className="text-3xl font-bold text-center mb-6 text-gray-900">
            Start New Game
          </h1>
          <form onSubmit={startNewGame} className="space-y-6">
            <div>
              <label htmlFor="companyName" className="block text-sm font-medium text-gray-700 mb-2">
                Company Name
              </label>
              <input
                type="text"
                id="companyName"
                value={companyName}
                onChange={(e) => setCompanyName(e.target.value)}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Enter your company name"
                required
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-blue-600 text-white py-3 px-4 rounded-lg font-semibold hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {loading ? 'Starting...' : 'Start Game'}
            </button>
          </form>
          {error && (
            <div className="mt-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-sm">
              {error}
            </div>
          )}
        </div>
      </div>
    );
  }

  if (!gameState.gameSession) {
    return <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="text-gray-500">Loading game state...</div>
    </div>;
  }

  // Week turn results modal
  if (gameState.weekTurnResults) {
    const results = gameState.weekTurnResults;
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
        <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-96 overflow-y-auto">
          <div className="p-6">
            <h2 className="text-2xl font-bold mb-4">Week {gameState.gameSession.currentWeek - 1} Results</h2>
            
            {results.completedContracts.length > 0 && (
              <div className="mb-4">
                <h3 className="font-semibold text-green-600 mb-2">Completed Contracts:</h3>
                {results.completedContracts.map((contract, index) => (
                  <div key={`completed-${contract.id}-${index}`} className="bg-green-50 border border-green-200 p-3 rounded mb-2">
                    <div className="font-medium">{contract.title}</div>
                    <div className="text-sm text-gray-600">
                      Reward: ${contract.baseReward.toLocaleString()} | 
                      Stakeholder Points: {contract.stakeholderPoints}
                    </div>
                  </div>
                ))}
              </div>
            )}

            {results.quitEmployees.length > 0 && (
              <div className="mb-4">
                <h3 className="font-semibold text-red-600 mb-2">Employees Quit:</h3>
                {results.quitEmployees.map((employee, index) => (
                  <div key={`quit-${employee.id}-${index}`} className="bg-red-50 border border-red-200 p-3 rounded mb-2">
                    <div className="font-medium">{employee.name}</div>
                    <div className="text-sm text-gray-600">
                      {employee.employeeType} | Morale: {employee.morale}%
                    </div>
                  </div>
                ))}
              </div>
            )}

            <button
              onClick={dismissResults}
              className="w-full bg-blue-600 text-white py-3 px-4 rounded-lg font-semibold hover:bg-blue-700 transition-colors"
            >
              Continue
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Main dashboard layout
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Error Banner */}
      {error && (
        <div className="bg-red-50 border-b border-red-200">
          <div className="max-w-7xl mx-auto px-4 py-3">
            <div className="flex items-center justify-between">
              <span className="text-red-700">{error}</span>
              <button 
                onClick={() => setError(null)}
                className="text-red-500 hover:text-red-700"
              >
                ✕
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Header Section - Company Stats */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-6 py-6">
          <div className="text-center mb-6">
            <h1 className="text-3xl font-bold text-gray-900">{gameState.gameSession.companyName}</h1>
            <p className="text-gray-600 mt-1">Quarter {gameState.gameSession.currentQuarter}</p>
          </div>

          {/* Company Stats Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">
                ${gameState.gameSession.budget.toLocaleString()}
              </div>
              <div className="text-sm font-medium text-gray-500 uppercase tracking-wide">
                Budget
              </div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">
                {gameState.gameSession.stakeholderValue}
              </div>
              <div className="text-sm font-medium text-gray-500 uppercase tracking-wide">
                Stakeholder Value
              </div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-purple-600">
                {gameState.employees.length}
              </div>
              <div className="text-sm font-medium text-gray-500 uppercase tracking-wide">
                Employees
              </div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-orange-600">
                {gameState.gameSession.totalScore}
              </div>
              <div className="text-sm font-medium text-gray-500 uppercase tracking-wide">
                Total Score
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content Section - 3 Column Grid */}
      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 min-h-[600px]">
          
          {/* Left Column */}
          <div className="space-y-6">
            {/* Next Week Button */}
            <button
              onClick={nextWeek}
              disabled={processingWeek}
              className="w-full bg-blue-600 text-white py-4 px-6 rounded-lg font-semibold text-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {processingWeek ? 'Processing...' : 'Next Week'}
            </button>

            {/* Current Employees */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Current Employees</h2>
              <div className="space-y-3 max-h-64 overflow-y-auto">
                {gameState.employees.length === 0 ? (
                  <p className="text-gray-500 italic">No employees hired yet</p>
                ) : (
                  gameState.employees.map((employee, index) => (
                    <div key={`employee-${employee.id}-${index}`} className="flex items-center justify-between py-2 border-b border-gray-100 last:border-b-0">
                      <div>
                        <div className="font-medium text-gray-900">{employee.name}</div>
                        <div className="text-sm text-gray-500">
                          {employee.employeeType} • Level {employee.level}
                        </div>
                        <div className="text-xs text-gray-400">
                          Speed: {employee.effectiveSpeed} • Accuracy: {employee.effectiveAccuracy}%
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-sm font-medium text-gray-900">
                          ${employee.salary.toLocaleString()}/wk
                        </div>
                        <div className="text-xs text-red-500">
                          {employee.quitChance}% quit risk
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Active Contracts (Priority) or Available Contracts */}
            {gameState.activeContracts.length > 0 ? (
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4">Active Contracts</h2>
                <div className="space-y-4 max-h-80 overflow-y-auto">
                  {gameState.activeContracts.map((contract, index) => (
                    <div key={`active-contract-${contract.id}-${index}`} className="border border-blue-200 rounded-lg p-4 bg-blue-50">
                      <div className="mb-3">
                        <h3 className="font-medium text-gray-900 mb-1">{contract.title}</h3>
                        <div className="flex items-center gap-2 text-xs text-gray-600 mb-2">
                          <span className={`px-2 py-1 rounded ${
                            contract.difficulty === 'EASY' ? 'bg-green-100 text-green-700' :
                            contract.difficulty === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                            'bg-red-100 text-red-700'
                          }`}>
                            {contract.difficulty}
                          </span>
                          <span>{contract.weeksRemaining} weeks left</span>
                        </div>
                        
                        {/* Progress Bar */}
                        <div className="mb-2">
                          <div className="flex justify-between text-xs text-gray-600 mb-1">
                            <span>Progress: {contract.currentProgress}/{contract.totalWorkRequired}</span>
                            <span>{Math.round((contract.currentProgress / contract.totalWorkRequired) * 100)}%</span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div 
                              className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                              style={{ width: `${Math.min(100, (contract.currentProgress / contract.totalWorkRequired) * 100)}%` }}
                            ></div>
                          </div>
                        </div>

                        <div className="text-sm font-medium text-green-600">
                          ${contract.baseReward.toLocaleString()} • {contract.stakeholderPoints} SP
                        </div>
                      </div>

                      {/* Employee Assignment */}
                      <div className="border-t border-blue-200 pt-3">
                        <h4 className="text-sm font-medium text-gray-700 mb-2">Assign Employee:</h4>
                        <div className="space-y-1">
                          {gameState.employees.length === 0 ? (
                            <p className="text-xs text-gray-500 italic">No employees to assign</p>
                          ) : (
                            gameState.employees.map((employee, empIndex) => (
                              <button
                                key={`assign-${employee.id}-${empIndex}`}
                                onClick={() => assignEmployee(contract.id, employee.id)}
                                className="w-full text-left px-2 py-1 text-sm bg-white border border-gray-200 rounded hover:bg-gray-50 hover:border-blue-300 transition-colors"
                              >
                                <span className="font-medium">{employee.name}</span>
                                <span className="text-gray-500 ml-2">({employee.employeeType})</span>
                              </button>
                            ))
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              /* Available Contracts (when no active contracts) */
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4">Available Contracts</h2>
                <div className="space-y-3 max-h-64 overflow-y-auto">
                  {gameState.availableContracts.length === 0 ? (
                    <p className="text-gray-500 italic">No contracts available</p>
                  ) : (
                    gameState.availableContracts.map((contract, index) => (
                      <div key={`contract-${contract.id}-${index}`} className="border border-gray-200 rounded-lg p-3">
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <h3 className="font-medium text-gray-900 mb-1">{contract.title}</h3>
                            <div className="flex items-center gap-2 text-xs text-gray-500 mb-2">
                              <span className={`px-2 py-1 rounded ${
                                contract.difficulty === 'EASY' ? 'bg-green-100 text-green-700' :
                                contract.difficulty === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                                'bg-red-100 text-red-700'
                              }`}>
                                {contract.difficulty}
                              </span>
                              <span>{contract.deadlineWeeks} weeks</span>
                            </div>
                            <div className="text-sm font-medium text-green-600">
                              ${contract.baseReward.toLocaleString()} • {contract.stakeholderPoints} SP
                            </div>
                          </div>
                          <button
                            onClick={() => acceptContract(contract.id)}
                            className="ml-3 bg-blue-600 text-white px-3 py-1 rounded text-sm hover:bg-blue-700 transition-colors"
                          >
                            Accept
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Center Column - Week & Progress */}
          <div className="flex flex-col items-center justify-center">
            <div className="text-center">
              <div className="text-6xl font-bold text-gray-900 mb-4">
                Week {gameState.gameSession.currentWeek}
              </div>
              
              {/* Progress Bar */}
              <div className="w-80 bg-gray-200 rounded-full h-4 mb-6">
                <div 
                  className="bg-blue-600 h-4 rounded-full transition-all duration-500"
                  style={{ width: `${calculateWeekProgress()}%` }}
                ></div>
              </div>
              
              <div className="text-xl text-gray-600 mb-8">
                {calculateWeekProgress()}% through quarter
              </div>

              {/* Key Metrics */}
              <div className="space-y-4">
                <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
                  <div className="text-lg font-semibold text-gray-900">Weekly Expenses</div>
                  <div className="text-2xl font-bold text-red-600">
                    ${gameState.employees.reduce((sum, emp) => sum + emp.salary, 0).toLocaleString()}
                  </div>
                </div>
                <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
                  <div className="text-lg font-semibold text-gray-900">Active Projects</div>
                  <div className="text-2xl font-bold text-blue-600">
                    {gameState.activeContracts.length}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right Column - End Game Button at Bottom */}
          <div className="flex flex-col justify-end">
            {/* Available Employees for Hiring */}
            {gameState.availableEmployees.length > 0 && (
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4">Available for Hire</h2>
                <div className="space-y-3 max-h-48 overflow-y-auto">
                  {gameState.availableEmployees.map((employee, index) => (
                    <div key={`hire-${employee.id}-${index}`} className="flex items-center justify-between py-2 border-b border-gray-100 last:border-b-0">
                      <div>
                        <div className="font-medium text-gray-900">{employee.name}</div>
                        <div className="text-sm text-gray-500">
                          Level {employee.level} • ${employee.salary.toLocaleString()}/wk
                        </div>
                      </div>
                      <button
                        onClick={() => hireEmployee(employee)}
                        className="bg-green-600 text-white px-3 py-1 rounded text-sm hover:bg-green-700 transition-colors"
                      >
                        Hire
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* End Game Button */}
            <button
              onClick={endGame}
              disabled={loading}
              className="w-full bg-red-600 text-white py-4 px-6 rounded-lg font-semibold text-lg hover:bg-red-700 disabled:opacity-50 transition-colors"
            >
              End Game
            </button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default GameManager;