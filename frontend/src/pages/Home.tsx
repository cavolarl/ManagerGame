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
          Welcome to Manager Game
        </h1>
        <p className="text-xl text-gray-600 mb-8 max-w-2xl mx-auto">
          Build your empire, manage your team, and become the ultimate business manager. 
          Start your journey to success today!
        </p>
        <div className="space-x-4">
          <Link 
            to="/register"
            className="inline-block px-8 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors"
          >
            Start Playing
          </Link>
          <Link 
            to="/login"
            className="inline-block px-8 py-3 border border-blue-600 text-blue-600 font-semibold rounded-lg hover:bg-blue-50 transition-colors"
          >
            Login
          </Link>
        </div>
      </div>

      {/* Features Section */}
      <div className="grid md:grid-cols-3 gap-8 py-16">
        <div className="text-center p-6 bg-white rounded-lg shadow-md">
          <div className="text-4xl mb-4">👥</div>
          <h3 className="text-xl font-semibold mb-2">Manage Your Team</h3>
          <p className="text-gray-600">
            Hire talented employees with unique skills and build the perfect team for your company.
          </p>
        </div>
        <div className="text-center p-6 bg-white rounded-lg shadow-md">
          <div className="text-4xl mb-4">💰</div>
          <h3 className="text-xl font-semibold mb-2">Grow Your Business</h3>
          <p className="text-gray-600">
            Make strategic decisions to increase your revenue and expand your business empire.
          </p>
        </div>
        <div className="text-center p-6 bg-white rounded-lg shadow-md">
          <div className="text-4xl mb-4">📊</div>
          <h3 className="text-xl font-semibold mb-2">Track Progress</h3>
          <p className="text-gray-600">
            Monitor your company's performance with detailed reports and analytics.
          </p>
        </div>
      </div>

      {/* Game Info Section */}
      {gameInfo && (
        <div className="bg-white rounded-lg shadow-md p-6 mb-8">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Game Information</h2>
          <div className="grid md:grid-cols-2 gap-4">
            <div>
              <p className="text-gray-600"><strong>Name:</strong> {gameInfo.name}</p>
              <p className="text-gray-600"><strong>Version:</strong> {gameInfo.version}</p>
            </div>
            <div>
              <p className="text-gray-600"><strong>Description:</strong> {gameInfo.description}</p>
            </div>
          </div>
        </div>
      )}

      {/* Backend Status */}
      {healthStatus && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <div className="w-3 h-3 bg-green-500 rounded-full"></div>
            </div>
            <div className="ml-3">
              <p className="text-sm font-medium text-green-800">
                Backend Status: {healthStatus.status}
              </p>
              <p className="text-sm text-green-700">{healthStatus.message}</p>
            </div>
          </div>
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-800">⚠️ {error}</p>
        </div>
      )}
    </div>
  )
}

export default Home