package larl.manager.backend.dto.response

import larl.manager.backend.entity.*
import larl.manager.backend.service.UserProgressionSummary
import larl.manager.backend.service.UnlockProgress
import java.time.LocalDateTime

// Updated: GameSession now contains company info
data class GameSessionResponse(
    val id: Long,
    val userId: Long,
    val username: String,
    val companyName: String, // Now part of GameSession
    val currentQuarter: Int,
    val currentWeek: Int,
    val budget: Long,
    val stakeholderValue: Int,
    val errorPenalties: Int,
    val status: String,
    val appliedPerks: List<String>, // New: Show active perks
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val totalScore: Int,
    val isQuarterEnd: Boolean
) {
    companion object {
        fun from(gameSession: GameSession) = GameSessionResponse(
            id = gameSession.id,
            userId = gameSession.user.id,
            username = gameSession.user.username,
            companyName = gameSession.companyName,
            currentQuarter = gameSession.currentQuarter,
            currentWeek = gameSession.currentWeek,
            budget = gameSession.budget,
            stakeholderValue = gameSession.stakeholderValue,
            errorPenalties = gameSession.errorPenalties,
            status = gameSession.status.name,
            appliedPerks = gameSession.appliedPerks.map { it.name },
            startedAt = gameSession.startedAt,
            endedAt = gameSession.endedAt,
            totalScore = gameSession.getTotalScore(),
            isQuarterEnd = gameSession.isQuarterEnd()
        )
    }
}

// Updated: Remove companyId reference
data class GameEmployeeResponse(
    val id: Long,
    val name: String,
    val employeeType: String,
    val level: Int,
    val speed: Int,
    val accuracy: Int,
    val salary: Long,
    val morale: Int,
    val isActive: Boolean,
    val gameSessionId: Long,
    val effectiveSpeed: Int, // New: Show perk-adjusted stats
    val effectiveAccuracy: Int,
    val quitChance: Int
) {
    companion object {
        fun from(employee: GameEmployee) = GameEmployeeResponse(
            id = employee.id,
            name = employee.name,
            employeeType = employee.employeeType.name,
            level = employee.level,
            speed = employee.speed,
            accuracy = employee.accuracy,
            salary = employee.salary,
            morale = employee.morale,
            isActive = employee.isActive,
            gameSessionId = employee.gameSession.id,
            effectiveSpeed = employee.getEffectiveSpeed(),
            effectiveAccuracy = employee.getEffectiveAccuracy(),
            quitChance = employee.getQuitChance()
        )
    }
}

// Updated: Enhanced with perk info
data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val createdAt: LocalDateTime,
    val role: String,
    val isActive: Boolean,
    val totalRuns: Int, // New: Meta-progression stats
    val bestScore: Int,
    val totalQuartersCompleted: Int,
    val totalCompaniesCreated: Int,
    val unlockedPerks: List<String>,
    val unlockedEmployeeTypes: List<String>
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            createdAt = user.createdAt,
            role = user.role.name,
            isActive = user.isActive,
            totalRuns = user.totalRuns,
            bestScore = user.bestScore,
            totalQuartersCompleted = user.totalQuartersCompleted,
            totalCompaniesCreated = user.totalCompaniesCreated,
            unlockedPerks = user.unlockedPerks.map { it.name },
            unlockedEmployeeTypes = user.unlockedEmployeeTypes.map { it.name }
        )
    }
}

// Keep existing DTOs that are still relevant
data class ContractResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val difficulty: String,
    val totalWorkRequired: Int,
    val currentProgress: Int,
    val progressPercentage: Double,
    val baseReward: Long,
    val bonusReward: Long,
    val actualReward: Long,
    val stakeholderValue: Int,
    val deadlineWeeks: Int,
    val weeksRemaining: Int,
    val status: String,
    val quarter: Int,
    val isOverdue: Boolean,
    val isBonusEligible: Boolean
) {
    companion object {
        fun from(contract: Contract) = ContractResponse(
            id = contract.id,
            title = contract.title,
            description = contract.description,
            difficulty = contract.difficulty.name,
            totalWorkRequired = contract.totalWorkRequired,
            currentProgress = contract.currentProgress,
            progressPercentage = if (contract.totalWorkRequired > 0)
                contract.getCompletionPercentage().coerceAtMost(100.0) else 0.0,
            baseReward = contract.baseReward,
            bonusReward = (contract.baseReward * contract.bonusMultiplier).toLong(),
            actualReward = contract.getEffectiveReward(),
            stakeholderValue = contract.stakeholderPoints,
            deadlineWeeks = contract.deadlineWeeks,
            weeksRemaining = contract.weeksRemaining,
            status = contract.status.name,
            quarter = contract.gameSession.currentQuarter, // assuming GameSession tracks this
            isOverdue = contract.isOverdue(),
            isBonusEligible = contract.weeksRemaining > 0 && contract.isComplete()
        )
    }
}


data class ContractAssignmentResponse(
    val id: Long,
    val contractId: Long,
    val contractTitle: String,
    val employeeId: Long,
    val employeeName: String,
    val weekAssigned: Int,
    val isActive: Boolean
) {
    companion object {
        fun from(assignment: ContractAssignment) = ContractAssignmentResponse(
            id = assignment.id,
            contractId = assignment.contract.id,
            contractTitle = assignment.contract.title,
            employeeId = assignment.employee.id,
            employeeName = assignment.employee.name,
            weekAssigned = assignment.weekAssigned,
            isActive = assignment.isActive
        )
    }
}

// New: Meta-progression DTOs
data class UserProgressionResponse(
    val totalRuns: Int,
    val bestScore: Int,
    val totalQuartersCompleted: Int,
    val totalCompaniesCreated: Int,
    val unlockedPerks: List<PerkResponse>,
    val unlockedEmployeeTypes: List<String>,
    val nextUnlockProgress: List<UnlockProgressResponse>
) {
    companion object {
        fun from(summary: UserProgressionSummary) = UserProgressionResponse(
            totalRuns = summary.totalRuns,
            bestScore = summary.bestScore,
            totalQuartersCompleted = summary.totalQuartersCompleted,
            totalCompaniesCreated = summary.totalCompaniesCreated,
            unlockedPerks = summary.unlockedPerks.map { PerkResponse.from(it) },
            unlockedEmployeeTypes = summary.unlockedEmployeeTypes.map { it.name },
            nextUnlockProgress = summary.nextUnlockProgress.map { UnlockProgressResponse.from(it) }
        )
    }
}

data class PerkResponse(
    val name: String,
    val description: String,
    val isActive: Boolean = false
) {
    companion object {
        fun from(perk: CompanyPerk, isActive: Boolean = false) = PerkResponse(
            name = perk.name,
            description = getPerkDescription(perk),
            isActive = isActive
        )
        
        private fun getPerkDescription(perk: CompanyPerk): String = when (perk) {
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

data class UnlockProgressResponse(
    val type: String,
    val name: String,
    val description: String,
    val requirement: Int,
    val currentProgress: Int,
    val progressPercentage: Int
) {
    companion object {
        fun from(progress: UnlockProgress) = UnlockProgressResponse(
            type = progress.type,
            name = progress.name,
            description = progress.description,
            requirement = progress.requirement,
            currentProgress = progress.currentProgress,
            progressPercentage = progress.progressPercentage
        )
    }
}

// Composite response DTOs for complex operations
data class GameInitializationResponse(
    val gameSession: GameSessionResponse,
    val availableEmployees: List<GameEmployeeResponse>,
    val availablePerks: List<PerkResponse>
)

data class GameStateResponse(
    val gameSession: GameSessionResponse,
    val activeEmployees: List<GameEmployeeResponse>,
    val availableContracts: List<ContractResponse>,
    val activeContracts: List<ContractResponse>
)

data class WeekTurnResponse(
    val gameSession: GameSessionResponse,
    val contractResults: List<ContractResponse>,
    val quitEmployees: List<GameEmployeeResponse>,
    val completedContracts: List<ContractResponse>
)

data class GameEndResponse(
    val gameSession: GameSessionResponse,
    val finalScore: Int,
    val newUnlocks: List<PerkResponse>,
    val userProgression: UserProgressionResponse
)