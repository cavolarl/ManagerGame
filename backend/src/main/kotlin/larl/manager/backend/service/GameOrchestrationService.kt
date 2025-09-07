package larl.manager.backend.service

import larl.manager.backend.entity.*
import larl.manager.backend.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Refactored orchestration service that works directly with Users and GameSessions
 * (no Company entity needed)
 */
@Service
@Transactional
class GameOrchestrationService(
    private val gameSessionRepository: GameSessionRepository,
    private val userRepository: UserRepository,
    private val contractService: ContractService,
    private val employeeService: EmployeeService,
    private val metaProgressionService: MetaProgressionService
) {
    
    /**
     * Initialize a complete new game (new company run) for a user
     */
    fun initializeNewGame(userId: Long, companyName: String, selectedPerks: Set<CompanyPerk> = emptySet()): GameInitializationResult {
        try {
            val user = userRepository.findById(userId).orElse(null)
                ?: return GameInitializationResult.failure("User not found")
            
            // Check if user already has an active game
            val existingGame = gameSessionRepository.findActiveGameByUser(userId)
            if (existingGame.isPresent) {
                return GameInitializationResult.failure("User already has an active game session")
            }
            
            // Validate selected perks
            val validPerks = selectedPerks.filter { user.unlockedPerks.contains(it) }.toMutableSet()
            
            // Create new game session (this IS the company for this run)
            val startingBudget = if (validPerks.contains(CompanyPerk.BUDGET_BOOST)) 55000L else 50000L
            
            val gameSession = GameSession(
                user = user,
                companyName = companyName,
                budget = startingBudget,
                appliedPerks = validPerks
            )
            
            val savedGame = gameSessionRepository.save(gameSession)
            
            // Generate initial contracts (2-3 as per game design)
            contractService.generateInitialContracts(savedGame)
            
            // Generate initial employee pool for hiring
            val availableEmployees = generateInitialEmployeePool(savedGame)
            
            return GameInitializationResult.success(savedGame, availableEmployees)
            
        } catch (e: Exception) {
            return GameInitializationResult.failure(e.message ?: "Unknown error during game initialization")
        }
    }
    
    /**
     * Process a complete week turn including all mechanics
     */
    fun processWeekTurn(gameSessionId: Long): WeekTurnResult {
        try {
            val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null)
                ?: return WeekTurnResult.failure("Game session not found")
            
            if (gameSession.status != GameStatus.ACTIVE) {
                return WeekTurnResult.failure("Game session is not active")
            }
            
            // 1. Apply weekly morale bonuses from perks
            val employees = employeeService.getActiveEmployees(gameSessionId)
            employees.forEach { employee ->
                val updatedEmployee = employee.applyWeeklyMoraleBonus()
                if (updatedEmployee != employee) {
                    employeeService.updateEmployee(updatedEmployee)
                }
            }
            
            // 2. Process contract progress for current week
            val activeContracts = contractService.findActiveContracts(gameSessionId)
            val contractResults = activeContracts.map { contract ->
                contractService.processWeeklyProgress(contract.id, gameSession.currentWeek)
            }
            
            // 4. Check employee retention (low morale employees might quit)
            val quitEmployees = employeeService.checkEmployeeRetention(gameSessionId)
            
            // 5. Pay salaries (with perk adjustments if applicable)
            val totalSalaries = employeeService.calculateTotalSalaries(gameSessionId)
            val updatedBudget = gameSession.budget - totalSalaries
            
            // 6. Advance to next week
            val newWeek = gameSession.currentWeek + 1
            val newQuarter = if (newWeek > 13) gameSession.currentQuarter + 1 else gameSession.currentQuarter
            val adjustedWeek = if (newWeek > 13) 1 else newWeek
            
            val updatedGameSession = gameSession.copy(
                currentWeek = adjustedWeek,
                currentQuarter = newQuarter,
                budget = updatedBudget
            )
            
            val savedSession = gameSessionRepository.save(updatedGameSession)
            
            // 7. Check for completed contracts and update rewards (with perk bonuses)
            val completedContracts = contractResults.filter { it?.status == ContractStatus.COMPLETED }
            completedContracts.forEach { contract ->
                contract?.let {
                    val rewardMultiplier = gameSession.getContractRewardMultiplier()
                    val adjustedReward = (it.getEffectiveReward() * rewardMultiplier).toLong()
                    
                    // Update game session with rewards
                    updateBudget(gameSessionId, adjustedReward)
                    updateStakeholderValue(gameSessionId, it.stakeholderPoints)
                }
            }
            
            // 8. Check for quarter end
            val finalSession = if (newWeek > 13) {
                handleQuarterEnd(savedSession)
            } else {
                savedSession
            }
            
            return WeekTurnResult.success(
                gameSession = finalSession,
                contractResults = contractResults.filterNotNull(),
                quitEmployees = quitEmployees,
                completedContracts = completedContracts.filterNotNull()
            )
            
        } catch (e: Exception) {
            return WeekTurnResult.failure(e.message ?: "Unknown error during week processing")
        }
    }
    
    /**
     * Handle employee hiring with perk-based cost adjustments
     */
    fun hireEmployee(gameSessionId: Long, employeeTemplate: GameEmployee): EmployeeHiringResult {
        try {
            val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null)
                ?: return EmployeeHiringResult.failure("Game session not found")
            
            // Apply perk-based hiring cost adjustment
            val baseCost = calculateHiringCost(employeeTemplate)
            val adjustedCost = (baseCost * gameSession.getHiringCostMultiplier()).toLong()
            
            if (!gameSession.canAfford(adjustedCost)) {
                return EmployeeHiringResult.failure("Insufficient funds to hire employee")
            }
            
            // Apply starting morale bonus from perks
            val startingMoraleBonus = gameSession.getStartingMoraleBonus()
            val adjustedMorale = (employeeTemplate.morale + startingMoraleBonus).coerceIn(0, 100)
            
            val adjustedEmployee = employeeTemplate.copy(
                gameSession = gameSession,
                morale = adjustedMorale,
                isActive = true
            )
            
            val hiredEmployee = employeeService.hireEmployee(gameSessionId, adjustedEmployee)
                ?: return EmployeeHiringResult.failure("Failed to hire employee")
            
            // Deduct adjusted hiring cost
            updateBudget(gameSessionId, -adjustedCost)
            
            return EmployeeHiringResult.success(hiredEmployee)
            
        } catch (e: Exception) {
            return EmployeeHiringResult.failure(e.message ?: "Unknown error during employee hiring")
        }
    }
    

    /**
     * End game and update user meta-progression
     */
    fun endGame(gameSessionId: Long, status: GameStatus): GameEndResult {
        try {
            val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null)
                ?: return GameEndResult.failure("Game session not found")
            
            val endedSession = gameSession.copy(
                status = status,
                endedAt = java.time.LocalDateTime.now()
            )
            
            val savedSession = gameSessionRepository.save(endedSession)
            
            // Update user meta-progression
            val updatedUser = metaProgressionService.updateUserStats(gameSession.user, savedSession)
            userRepository.save(updatedUser)
            
            // Check for new unlocks
            val newUnlocks = metaProgressionService.checkForNewUnlocks(updatedUser)
            
            return GameEndResult.success(savedSession, newUnlocks)
            
        } catch (e: Exception) {
            return GameEndResult.failure(e.message ?: "Unknown error ending game")
        }
    }
    
    /**
     * Get comprehensive game state for UI display
     */
    fun getGameState(gameSessionId: Long): GameStateResult {
        try {
            val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null)
                ?: return GameStateResult.failure("Game session not found")
            
            val activeEmployees = employeeService.getActiveEmployees(gameSessionId)
            val availableContracts = contractService.findAvailableContracts(gameSessionId)
            val activeContracts = contractService.findActiveContracts(gameSessionId)
            
            return GameStateResult.success(
                gameSession = gameSession,
                activeEmployees = activeEmployees,
                availableContracts = availableContracts,
                activeContracts = activeContracts,
            )
            
        } catch (e: Exception) {
            return GameStateResult.failure(e.message ?: "Unknown error retrieving game state")
        }
    }
    
    // Helper methods
    private fun generateInitialEmployeePool(gameSession: GameSession): List<GameEmployee> {
        return (1..5).map { 
            employeeService.generateRandomEmployee(gameSession)
        }
    }
    
    private fun handleQuarterEnd(gameSession: GameSession): GameSession {
        // Calculate performance score
        val score = gameSession.getTotalScore()
        
        // Check if game should end
        if (score < getMinimumScoreThreshold(gameSession.currentQuarter)) {
            return endGame(gameSession.id, GameStatus.FAILED).let { result ->
                when (result) {
                    is GameEndResult.Success -> result.gameSession
                    is GameEndResult.Failure -> gameSession // Keep original if end fails
                }
            }
        }
        
        // Generate new contracts for next quarter
        contractService.generateQuarterlyContracts(gameSession)
        
        return gameSession
    }
    
    private fun updateBudget(gameSessionId: Long, amount: Long): GameSession? {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return null
        val updatedSession = gameSession.copy(budget = gameSession.budget + amount)
        return gameSessionRepository.save(updatedSession)
    }
    
    private fun updateStakeholderValue(gameSessionId: Long, value: Int): GameSession? {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return null
        val updatedSession = gameSession.copy(stakeholderValue = gameSession.stakeholderValue + value)
        return gameSessionRepository.save(updatedSession)
    }
    
    private fun calculateHiringCost(employee: GameEmployee): Long {
        return employee.salary * 2 // 2 weeks salary as hiring cost
    }
    
    private fun calculateTrainingCost(employee: GameEmployee): Long {
        return 2000L * employee.level // Scales with level
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

// Updated result classes for refactored structure
sealed class GameInitializationResult {
    data class Success(
        val gameSession: GameSession,
        val availableEmployees: List<GameEmployee>
    ) : GameInitializationResult()
    
    data class Failure(val error: String) : GameInitializationResult()
    
    companion object {
        fun success(gameSession: GameSession, employees: List<GameEmployee>) = 
            Success(gameSession, employees)
        fun failure(error: String) = Failure(error)
    }
}

sealed class GameEndResult {
    data class Success(val gameSession: GameSession, val newUnlocks: List<CompanyPerk>) : GameEndResult()
    data class Failure(val error: String) : GameEndResult()
    
    companion object {
        fun success(gameSession: GameSession, unlocks: List<CompanyPerk>) = Success(gameSession, unlocks)
        fun failure(error: String) = Failure(error)
    }
}

// Keep existing result classes that are still relevant
sealed class WeekTurnResult {
    data class Success(
        val gameSession: GameSession,
        val contractResults: List<Contract>,
        val quitEmployees: List<GameEmployee>,
        val completedContracts: List<Contract>
    ) : WeekTurnResult()
    
    data class Failure(val error: String) : WeekTurnResult()
    
    companion object {
        fun success(
            gameSession: GameSession,
            contractResults: List<Contract>,
            quitEmployees: List<GameEmployee>,
            completedContracts: List<Contract>
        ) = Success(gameSession, contractResults, quitEmployees, completedContracts)
        
        fun failure(error: String) = Failure(error)
    }
}

sealed class EmployeeHiringResult {
    data class Success(val employee: GameEmployee) : EmployeeHiringResult()
    data class Failure(val error: String) : EmployeeHiringResult()
    
    companion object {
        fun success(employee: GameEmployee) = Success(employee)
        fun failure(error: String) = Failure(error)
    }
}

sealed class GameStateResult {
    data class Success(
        val gameSession: GameSession,
        val activeEmployees: List<GameEmployee>,
        val availableContracts: List<Contract>,
        val activeContracts: List<Contract>
    ) : GameStateResult()
    
    data class Failure(val error: String) : GameStateResult()
    
    companion object {
        fun success(
            gameSession: GameSession,
            activeEmployees: List<GameEmployee>,
            availableContracts: List<Contract>,
            activeContracts: List<Contract>,
        ) = Success(gameSession, activeEmployees, availableContracts, activeContracts)
        
        fun failure(error: String) = Failure(error)
    }
}