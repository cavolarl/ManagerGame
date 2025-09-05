import { useState, useEffect } from 'react'
import axios from 'axios'
import './App.css'

const API_BASE_URL = 'http://localhost:8080/api'

interface GameInfo {
  name: string
  version: string
  description: string
}

interface HealthStatus {
  status: string
  message: string
  timestamp: string
}

function App() {
  const [gameInfo, setGameInfo] = useState<GameInfo | null>(null)
  const [healthStatus, setHealthStatus] = useState<HealthStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true)
        
        // Test health endpoint
        const healthResponse = await axios.get(`${API_BASE_URL}/health`)
        setHealthStatus(healthResponse.data)
        
        // Test game info endpoint
        const gameResponse = await axios.get(`${API_BASE_URL}/game/info`)
        setGameInfo(gameResponse.data)
        
        setError(null)
      } catch (err) {
        console.error('Error fetching data:', err)
        setError('Failed to connect to backend. Make sure your Spring Boot server is running on port 8080.')
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [])

  if (loading) {
    return (
      <div className="App">
        <h1>Loading...</h1>
      </div>
    )
  }

  if (error) {
    return (
      <div className="App">
        <h1>❌ Connection Error</h1>
        <p style={{ color: 'red' }}>{error}</p>
        <button onClick={() => window.location.reload()}>
          Retry Connection
        </button>
      </div>
    )
  }

  return (
    <div className="App">
      <h1>🎮 Manager Game Frontend</h1>
      
      {healthStatus && (
        <div style={{ background: '#e8f5e8', padding: '20px', margin: '20px', borderRadius: '8px' }}>
          <h2>✅ Backend Health</h2>
          <p><strong>Status:</strong> {healthStatus.status}</p>
          <p><strong>Message:</strong> {healthStatus.message}</p>
          <p><strong>Timestamp:</strong> {new Date(parseInt(healthStatus.timestamp)).toLocaleString()}</p>
        </div>
      )}

      {gameInfo && (
        <div style={{ background: '#e8f0ff', padding: '20px', margin: '20px', borderRadius: '8px' }}>
          <h2>🎯 Game Information</h2>
          <p><strong>Name:</strong> {gameInfo.name}</p>
          <p><strong>Version:</strong> {gameInfo.version}</p>
          <p><strong>Description:</strong> {gameInfo.description}</p>
        </div>
      )}

      <div style={{ marginTop: '40px' }}>
        <h3>🚀 Next Steps</h3>
        <ul style={{ textAlign: 'left', maxWidth: '500px', margin: '0 auto' }}>
          <li>Frontend and backend are successfully connected!</li>
          <li>You can now start building your game features</li>
          <li>Add authentication, game entities, and business logic</li>
          <li>Consider adding state management (Redux, Zustand, etc.)</li>
        </ul>
      </div>
    </div>
  )
}

export default App