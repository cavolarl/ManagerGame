import { useApi } from '../hooks/useApi'
import { ApiService } from '../services/api'
import { LoadingSpinner } from '../components/LoadingSpinner'

const Dashboard = () => {
  const { data: gameInfo, loading: gameLoading } = useApi(() => ApiService.getGameInfo())
  const { data: healthStatus, loading: healthLoading } = useApi(() => ApiService.getHealth())

  if (gameLoading || healthLoading) {
    return <LoadingSpinner size="lg" message="Loading dashboard..." />
  }

  return (
    <div className="max-w-6xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Manager Dashboard</h1>
        <p className="text-gray-600 mt-2">Welcome to your company management center</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <div className="text-2xl">💰</div>
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-500">Budget</p>
              <p className="text-2xl font-bold text-gray-900">$50,000</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <div className="text-2xl">👥</div>
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-500">Employees</p>
              <p className="text-2xl font-bold text-gray-900">0</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <div className="text-2xl">📊</div>
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-500">Active Reports</p>
              <p className="text-2xl font-bold text-gray-900">0</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <div className="text-2xl">🏆</div>
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-500">Level</p>
              <p className="text-2xl font-bold text-gray-900">1</p>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-8">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <button className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left">
            <div className="text-2xl mb-2">👤</div>
            <h3 className="font-semibold text-gray-900">Hire Employees</h3>
            <p className="text-sm text-gray-600">Expand your team with talented individuals</p>
          </button>

          <button className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left">
            <div className="text-2xl mb-2">📝</div>
            <h3 className="font-semibold text-gray-900">Start Report</h3>
            <p className="text-sm text-gray-600">Assign work to your employees</p>
          </button>

          <button className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left">
            <div className="text-2xl mb-2">⚙️</div>
            <h3 className="font-semibold text-gray-900">Company Settings</h3>
            <p className="text-sm text-gray-600">Manage your company configuration</p>
          </button>
        </div>
      </div>

      {/* Recent Activity */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-8">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Recent Activity</h2>
        <div className="text-center py-8 text-gray-500">
          <div className="text-4xl mb-4">📭</div>
          <p>No recent activity</p>
          <p className="text-sm">Start by hiring your first employee!</p>
        </div>
      </div>

      {/* System Status */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {gameInfo && (
          <div className="bg-blue-50 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-blue-900 mb-2">Game Info</h3>
            <div className="space-y-1">
              <p className="text-blue-800"><strong>Name:</strong> {gameInfo.name}</p>
              <p className="text-blue-800"><strong>Version:</strong> {gameInfo.version}</p>
              <p className="text-blue-800"><strong>Description:</strong> {gameInfo.description}</p>
            </div>
          </div>
        )}

        {healthStatus && (
          <div className="bg-green-50 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-green-900 mb-2">System Status</h3>
            <div className="space-y-1">
              <p className="text-green-800"><strong>Status:</strong> {healthStatus.status}</p>
              <p className="text-green-800"><strong>Message:</strong> {healthStatus.message}</p>
              <p className="text-green-800">
                <strong>Last Check:</strong> {new Date(parseInt(healthStatus.timestamp)).toLocaleString()}
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default Dashboard