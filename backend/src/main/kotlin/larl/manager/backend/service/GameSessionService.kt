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
    private val userRepository: UserRepository,
    private val contractService: ContractService,
    private val employeeService: EmployeeService
) {
    
    /**
     * Start a new game session for a user (replaces company-based creation)
     */
    fun startNewGame(userId: Long, companyName: String, selectedPerks: Set<CompanyPerk> = emptySet()): GameSession? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        
        // Check if user already has an active game
        val existingGame = gameSessionRepository.findActiveGameByUser(userId)
        if (existingGame.isPresent) {
            throw IllegalArgumentException("User already has an active game session")
        }
        
        // Validate selected perks against user's unlocked perks
        val validPerks = selectedPerks.filter { user.unlockedPerks.contains(it) }.toMutableSet()
        
        // Calculate starting budget with perks
        val startingBudget = if (validPerks.contains(CompanyPerk.BUDGET_BOOST)) {
            55000L // +5k bonus
        } else {
            50000L
        }
        
        val gameSession = GameSession(
            user = user,
            companyName = companyName,
            budget = startingBudget,
            appliedPerks = validPerks
        )
        
        val savedGame = gameSessionRepository.save(gameSession)
        
        // Generate initial contracts (2-3 as per game design)
        contractService.generateInitialContracts(savedGame)
        
        return savedGame
    }
    
    /**
     * Find active game session by user ID (replaces company-based lookup)
     */
    fun findActiveGameByUser(userId: Long): GameSession? {
        return gameSessionRepository.findActiveGameByUser(userId).orElse(null)
    }
    
    /**
     * Find game session by ID
     */
    fun findById(id: Long): GameSession? {
        return gameSessionRepository.findById(id).orElse(null)
    }
    
    /**
     * Get all game sessions for a user (replaces company-based history)
     */
    fun getUserGameHistory(userId: Long): List<GameSession> {
        return gameSessionRepository.findByUserIdOrderByStartedAtDesc(userId)
    }
    
    /**
     * Get completed games for a user (for statistics)
     */
    fun getCompletedGames(userId: Long): List<GameSession> {
        return gameSessionRepository.findByUserIdAndStatusOrderByStartedAtDesc(userId, GameStatus.COMPLETED)
    }
    
    /**
     * Advance to next week with perk-aware salary calculations
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
        
        // Apply weekly morale bonus from perks
        if (gameSession.appliedPerks.contains(CompanyPerk.MORALE_BONUS)) {
            employeeService.applyWeeklyMoraleBonus(gameSessionId, 5)
        }
        
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
     * Update stakeholder value with perk bonuses
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
     * End game and update user progression
     */
    fun endGame(gameSession: GameSession, status: GameStatus): GameSession {
        val updatedSession = gameSession.copy(
            status = status,
            endedAt = LocalDateTime.now()
        )
        
        val savedSession = gameSessionRepository.save(updatedSession)
        
        // Update user meta-progression stats
        val user = gameSession.user
        val updatedUser = user.updateStats(savedSession)
        userRepository.save(updatedUser)
        
        return savedSession
    }
    
    /**
     * Check if user can afford an expense (with perk adjustments)
     */
    fun canAfford(gameSessionId: Long, expense: ExpenseType, baseAmount: Long): Boolean {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return false
        
        val adjustedAmount = when (expense) {
            ExpenseType.HIRING -> (baseAmount * gameSession.getHiringCostMultiplier()).toLong()
            ExpenseType.TRAINING -> (baseAmount * gameSession.getTrainingCostMultiplier()).toLong()
            ExpenseType.OTHER -> baseAmount
        }
        
        return gameSession.canAfford(adjustedAmount)
    }
    
    /**
     * Spend money with perk adjustments
     */
    fun spendMoney(gameSessionId: Long, expense: ExpenseType, baseAmount: Long): GameSession? {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return null
        
        val adjustedAmount = when (expense) {
            ExpenseType.HIRING -> (baseAmount * gameSession.getHiringCostMultiplier()).toLong()
            ExpenseType.TRAINING -> (baseAmount * gameSession.getTrainingCostMultiplier()).toLong()
            ExpenseType.OTHER -> baseAmount
        }
        
        if (!gameSession.canAfford(adjustedAmount)) {
            throw IllegalArgumentException("Insufficient funds")
        }
        
        val updatedSession = gameSession.copy(budget = gameSession.budget - adjustedAmount)
        return gameSessionRepository.save(updatedSession)
    }
    
    /**
     * Add money with perk bonuses (for contract rewards)
     */
    fun earnMoney(gameSessionId: Long, baseAmount: Long): GameSession? {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return null
        
        // Apply contract negotiator perk bonus
        val adjustedAmount = (baseAmount * gameSession.getContractRewardMultiplier()).toLong()
        
        val updatedSession = gameSession.copy(budget = gameSession.budget + adjustedAmount)
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

// Helper enums and data classes
enum class ExpenseType {
    HIRING, TRAINING, OTHER
}

data class GameStatistics(
    val totalGamesPlayed: Long,
    val activeGames: Long,
    val averageStakeholderValue: Double,
    val averageQuartersCompleted: Double
)