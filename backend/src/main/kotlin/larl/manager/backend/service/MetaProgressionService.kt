package larl.manager.backend.service

import larl.manager.backend.entity.*
import larl.manager.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for handling meta-progression features between game runs
 */
@Service
@Transactional
class MetaProgressionService(
    private val userRepository: UserRepository
) {
    
    /**
     * Update user statistics after a game session ends
     */
    fun updateUserStats(user: User, gameSession: GameSession): User {
        val updatedUser = user.copy(
            totalRuns = user.totalRuns + 1,
            bestScore = maxOf(user.bestScore, gameSession.getTotalScore()),
            totalQuartersCompleted = user.totalQuartersCompleted + gameSession.currentQuarter,
            totalCompaniesCreated = user.totalCompaniesCreated + 1
        )
        
        return userRepository.save(updatedUser)
    }
    
    /**
     * Check for newly unlocked perks based on user stats
     */
    fun checkForNewUnlocks(user: User): List<CompanyPerk> {
        val newUnlocks = mutableListOf<CompanyPerk>()
        
        CompanyPerk.values().forEach { perk ->
            if (!user.unlockedPerks.contains(perk) && user.canUnlockPerk(perk)) {
                newUnlocks.add(perk)
            }
        }
        
        // Unlock the perks
        if (newUnlocks.isNotEmpty()) {
            val updatedUser = user.copy(
                unlockedPerks = (user.unlockedPerks + newUnlocks).toMutableSet()
            )
            userRepository.save(updatedUser)
        }
        
        return newUnlocks
    }
    
    /**
     * Get available perks for game start (user's unlocked perks)
     */
    fun getAvailablePerks(userId: Long): List<CompanyPerk> {
        val user = userRepository.findById(userId).orElse(null) ?: return emptyList()
        return user.unlockedPerks.toList()
    }
    
    /**
     * Get available employee types for hiring
     */
    fun getAvailableEmployeeTypes(userId: Long): List<EmployeeType> {
        val user = userRepository.findById(userId).orElse(null) ?: return listOf(EmployeeType.ANALYST)
        return user.unlockedEmployeeTypes.toList()
    }
    
    /**
     * Get user's progression summary
     */
    fun getUserProgression(userId: Long): UserProgressionSummary? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        
        return UserProgressionSummary(
            totalRuns = user.totalRuns,
            bestScore = user.bestScore,
            totalQuartersCompleted = user.totalQuartersCompleted,
            totalCompaniesCreated = user.totalCompaniesCreated,
            unlockedPerks = user.unlockedPerks.toList(),
            unlockedEmployeeTypes = user.unlockedEmployeeTypes.toList(),
            nextUnlockProgress = calculateNextUnlockProgress(user)
        )
    }
    
    /**
     * Calculate progress toward next unlocks
     */
    private fun calculateNextUnlockProgress(user: User): List<UnlockProgress> {
        val progress = mutableListOf<UnlockProgress>()
        
        CompanyPerk.values().forEach { perk ->
            if (!user.unlockedPerks.contains(perk)) {
                val requirement = getPerkRequirement(perk)
                val currentProgress = getCurrentProgress(user, perk)
                
                progress.add(
                    UnlockProgress(
                        type = "perk",
                        name = perk.name,
                        description = getPerkDescription(perk),
                        requirement = requirement,
                        currentProgress = currentProgress,
                        progressPercentage = (currentProgress.toDouble() / requirement * 100).toInt().coerceAtMost(100)
                    )
                )
            }
        }
        
        return progress.sortedBy { it.progressPercentage }.reversed().take(3) // Show top 3 closest unlocks
    }
    
    private fun getPerkRequirement(perk: CompanyPerk): Int {
        return when (perk) {
            CompanyPerk.BETTER_ONBOARDING -> 3 // 3 runs
            CompanyPerk.CHEAPER_TRAINING -> 5 // 5 quarters
            CompanyPerk.MORALE_BONUS -> 500 // 500 score
            CompanyPerk.FASTER_HIRING -> 10 // 10 companies
            CompanyPerk.BUDGET_BOOST -> 1 // 1 run
            CompanyPerk.EMPLOYEE_LOYALTY -> 10 // 10 quarters
            CompanyPerk.CONTRACT_NEGOTIATOR -> 1000 // 1000 score
            CompanyPerk.EFFICIENCY_EXPERT -> 20 // 20 quarters
        }
    }
    
    private fun getCurrentProgress(user: User, perk: CompanyPerk): Int {
        return when (perk) {
            CompanyPerk.BETTER_ONBOARDING -> user.totalRuns
            CompanyPerk.CHEAPER_TRAINING -> user.totalQuartersCompleted
            CompanyPerk.MORALE_BONUS -> user.bestScore
            CompanyPerk.FASTER_HIRING -> user.totalCompaniesCreated
            CompanyPerk.BUDGET_BOOST -> user.totalRuns
            CompanyPerk.EMPLOYEE_LOYALTY -> user.totalQuartersCompleted
            CompanyPerk.CONTRACT_NEGOTIATOR -> user.bestScore
            CompanyPerk.EFFICIENCY_EXPERT -> user.totalQuartersCompleted
        }
    }
    
    private fun getPerkDescription(perk: CompanyPerk): String {
        return when (perk) {
            CompanyPerk.BETTER_ONBOARDING -> "New employees start with +10 morale"
            CompanyPerk.CHEAPER_TRAINING -> "Training costs 25% less"
            CompanyPerk.MORALE_BONUS -> "All employees gain +5 morale weekly"
            CompanyPerk.FASTER_HIRING -> "Hiring costs 20% less"
            CompanyPerk.BUDGET_BOOST -> "Start each run with +$5,000"
            CompanyPerk.EMPLOYEE_LOYALTY -> "Employees 10% less likely to quit"
            CompanyPerk.CONTRACT_NEGOTIATOR -> "Contract rewards 15% higher"
            CompanyPerk.EFFICIENCY_EXPERT -> "Employees work 10% faster"
        }
    }
}

data class UserProgressionSummary(
    val totalRuns: Int,
    val bestScore: Int,
    val totalQuartersCompleted: Int,
    val totalCompaniesCreated: Int,
    val unlockedPerks: List<CompanyPerk>,
    val unlockedEmployeeTypes: List<EmployeeType>,
    val nextUnlockProgress: List<UnlockProgress>
)

data class UnlockProgress(
    val type: String, // "perk" or "employee_type"
    val name: String,
    val description: String,
    val requirement: Int,
    val currentProgress: Int,
    val progressPercentage: Int
)