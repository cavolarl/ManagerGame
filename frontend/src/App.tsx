import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { ErrorBoundary } from './components/ErrorBoundary'
import GameManager from './components/GameManager'
import Home from './pages/Home'
import './App.css'

function App() {
  return (
    <ErrorBoundary>
      <Router>
        <div className="min-h-screen bg-gray-50">
          <main className="px-4 py-8">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/game" element={<GameManager />} />
              {/* Redirect dashboard to game for now */}
              <Route path="/dashboard" element={<GameManager />} />
            </Routes>
          </main>
        </div>
      </Router>
    </ErrorBoundary>
  )
}

export default App