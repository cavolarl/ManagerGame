package larl.manager.backend.service

import larl.manager.backend.entity.*
import larl.manager.backend.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class GameSessionService(
    private val gameSessionRepository: GameSessionRepository,
    private val contractService: ContractService,
    private val employeeService: EmployeeService
) {
    
    /**
     * Start a new game session for a user (replaces company-based creation)
     */
    fun startNewGame(companyName: String): GameSession? {
        
        val gameSession = GameSession(
            sessionId = java.util.UUID.randomUUID().toString(),
            companyName = companyName,
        )
        
        val savedGame = gameSessionRepository.save(gameSession)
        
        // Generate initial contracts (2-3 as per game design)
        contractService.generateInitialContracts(savedGame)
        
        return savedGame
    }
    
    /**
     * Find game session by ID
     */
    fun findById(id: Long): GameSession? {
        return gameSessionRepository.findById(id).orElse(null)
    }

    /**
     * Find active game session by session ID
     */
    fun findActiveGameBySession(sessionId: String): GameSession? {
        return gameSessionRepository.findActiveGameBySession(sessionId).orElse(null)
    }
    
    /**
     * Advance to next week with salary calculations
     */
    fun advanceWeek(gameSessionId: Long): GameSession? {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return null
        
        if (gameSession.status != GameStatus.ACTIVE) {
            throw IllegalArgumentException("Game session is not active")
        }
        
        val newWeek = gameSession.currentWeek + 1
        val newQuarter = if (newWeek > 13) gameSession.currentQuarter + 1 else gameSession.currentQuarter
        val adjustedWeek = if (newWeek > 13) 1 else newWeek
        
        // Calculate salary costs (employees get perk bonuses automatically)
        val totalSalaries = employeeService.calculateTotalSalaries(gameSessionId)
        val newBudget = gameSession.budget - totalSalaries
        
        val updatedSession = gameSession.copy(
            currentWeek = adjustedWeek,
            currentQuarter = newQuarter,
            budget = newBudget
        )
        
        val savedSession = gameSessionRepository.save(updatedSession)
        
        // Check for quarter end
        if (newWeek > 13) {
            return handleQuarterEnd(savedSession)
        }
        
        return savedSession
    }
    
    /**
     * Update stakeholder value
     */
    fun updateStakeholderValue(gameSessionId: Long, value: Int): GameSession? {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return null
        val updatedSession = gameSession.copy(stakeholderValue = gameSession.stakeholderValue + value)
        return gameSessionRepository.save(updatedSession)
    }
    
    /**
     * Update budget
     */
    fun updateBudget(gameSessionId: Long, amount: Long): GameSession? {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return null
        val updatedSession = gameSession.copy(budget = gameSession.budget + amount)
        return gameSessionRepository.save(updatedSession)
    }
    
    /**
     * End game
     */
    fun endGame(gameSession: GameSession, status: GameStatus): GameSession {
        val updatedSession = gameSession.copy(
            status = status,
            endedAt = LocalDateTime.now()
        )
        
        val savedSession = gameSessionRepository.save(updatedSession)
        
        return savedSession
    }
    
    /**
     * Check if user can afford an expense
     */
    fun canAfford(gameSessionId: Long, baseAmount: Long): Boolean {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return false
        val cost = baseAmount
        
        return gameSession.canAfford(cost)
    }
    
    /**
     * Spend money
     */
    fun spendMoney(gameSessionId: Long, baseAmount: Long): GameSession? {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return null
        val cost = baseAmount
        
        val updatedSession = gameSession.copy(budget = gameSession.budget - cost)
        return gameSessionRepository.save(updatedSession)
    }
    
    /**
     * Add money
     */
    fun earnMoney(gameSessionId: Long, baseAmount: Long): GameSession? {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return null
        
        // Apply contract negotiator perk bonus
        val earnings = baseAmount
        
        val updatedSession = gameSession.copy(budget = gameSession.budget + earnings)
        return gameSessionRepository.save(updatedSession)
    }
    
    private fun handleQuarterEnd(gameSession: GameSession): GameSession {
        // Calculate performance score
        val score = calculateQuarterScore(gameSession)
        
        // Check if game should end
        if (score < getMinimumScoreThreshold(gameSession.currentQuarter)) {
            return endGame(gameSession, GameStatus.FAILED)
        }
        
        // Generate new contracts for next quarter
        contractService.generateQuarterlyContracts(gameSession)
        
        return gameSession
    }
    
    private fun calculateQuarterScore(gameSession: GameSession): Int {
        return gameSession.stakeholderValue + (gameSession.budget / 1000).toInt()
    }
    
    private fun getMinimumScoreThreshold(quarter: Int): Int {
        return when (quarter) {
            1 -> 100
            2 -> 200
            3 -> 350
            4 -> 500
            else -> 500 + (quarter - 4) * 150
        }
    }
}