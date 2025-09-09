import { Link } from 'react-router-dom'
import { useApi } from '../hooks/useApi'
import { ApiService } from '../services/api'
import { LoadingSpinner } from '../components/LoadingSpinner'

const Home = () => {
  const { data: gameInfo, loading, error } = useApi(() => ApiService.getGameInfo())
  const { data: healthStatus } = useApi(() => ApiService.getHealth())

  if (loading) {
    return <LoadingSpinner size="lg" message="Loading game information..." />
  }

  return (
    <div className="max-w-4xl mx-auto">
      {/* Hero Section */}
      <div className="text-center py-16">
        <h1 className="text-5xl font-bold text-gray-900 mb-6">
          {gameInfo?.name || 'Spreadsheet Manager 2025'}
        </h1>
        <p className="text-xl text-gray-600 mb-8 max-w-2xl mx-auto">
          {gameInfo?.description || 'Build your business empire, manage your team, hire employees, and complete contracts to become the ultimate spreadsheet manager!'}
        </p>
        <div className="space-x-4">
          <Link 
            to="/game"
            className="inline-block px-8 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors"
          >
            Start Playing
          </Link>
        </div>
      </div>

      {/* Game Flow Explanation */}
      <div className="bg-white rounded-lg shadow-md p-8 mb-8">
        <h2 className="text-2xl font-bold text-center mb-6">How to Play</h2>
        <div className="flex flex-col md:flex-row items-center justify-center space-y-4 md:space-y-0 md:space-x-8">
          <div className="flex items-center">
            <div className="w-10 h-10 bg-blue-600 text-white rounded-full flex items-center justify-center font-bold mr-3">1</div>
            <span className="font-medium">Start/Load Game</span>
          </div>
          <div className="text-gray-400">→</div>
          <div className="flex items-center">
            <div className="w-10 h-10 bg-green-600 text-white rounded-full flex items-center justify-center font-bold mr-3">2</div>
            <span className="font-medium">Hire Employees</span>
          </div>
          <div className="text-gray-400">→</div>
          <div className="flex items-center">
            <div className="w-10 h-10 bg-purple-600 text-white rounded-full flex items-center justify-center font-bold mr-3">3</div>
            <span className="font-medium">Accept Contracts</span>
          </div>
          <div className="text-gray-400">→</div>
          <div className="flex items-center">
            <div className="w-10 h-10 bg-yellow-600 text-white rounded-full flex items-center justify-center font-bold mr-3">4</div>
            <span className="font-medium">End Week</span>
          </div>
        </div>
      </div>

      {/* Features Section */}
      <div className="grid md:grid-cols-3 gap-8 py-16">
        <div className="text-center p-6 bg-white rounded-lg shadow-md">
          <div className="text-4xl mb-4">👥</div>
          <h3 className="text-xl font-semibold mb-2">Manage Your Team</h3>
          <p className="text-gray-600">
            Hire talented employees with unique skills, speed, and accuracy. Keep morale high to prevent them from quitting!
          </p>
        </div>
        <div className="text-center p-6 bg-white rounded-lg shadow-md">
          <div className="text-4xl mb-4">📋</div>
          <h3 className="text-xl font-semibold mb-2">Complete Contracts</h3>
          <p className="text-gray-600">
            Accept contracts of varying difficulty and deadlines. Complete them on time to earn money and stakeholder points.
          </p>
        </div>
        <div className="text-center p-6 bg-white rounded-lg shadow-md">
          <div className="text-4xl mb-4">📊</div>
          <h3 className="text-xl font-semibold mb-2">Quarterly Reviews</h3>
          <p className="text-gray-600">
            Survive quarterly performance reviews by maintaining high stakeholder value and managing your budget effectively.
          </p>
        </div>
      </div>

      {/* Game Status */}
      <div className="text-center py-8">
        <div className="inline-flex items-center px-4 py-2 rounded-lg bg-gray-100">
          <div className={`w-3 h-3 rounded-full mr-2 ${healthStatus?.status === 'UP' ? 'bg-green-500' : 'bg-red-500'}`}></div>
          <span className="text-sm font-medium">
            Game Server: {healthStatus?.status === 'UP' ? 'Online' : 'Offline'}
          </span>
        </div>
        {gameInfo && (
          <div className="mt-2 text-xs text-gray-500">
            Version {gameInfo.version}
          </div>
        )}
      </div>

      {error && (
        <div className="mt-4 text-center text-red-600 text-sm">
          {error}
        </div>
      )}
    </div>
  )
}

export default Home