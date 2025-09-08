package larl.manager.backend.dto.response

import larl.manager.backend.entity.*
import java.time.LocalDateTime

// Updated: GameSession now contains company info
data class GameSessionResponse(
    val id: Long,
    val companyName: String, // Now part of GameSession
    val currentQuarter: Int,
    val currentWeek: Int,
    val budget: Long,
    val stakeholderValue: Int,
    val errorPenalties: Int,
    val status: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val totalScore: Int,
    val isQuarterEnd: Boolean
) {
    companion object {
        fun from(gameSession: GameSession) = GameSessionResponse(
            id = gameSession.id,
            companyName = gameSession.companyName,
            currentQuarter = gameSession.currentQuarter,
            currentWeek = gameSession.currentWeek,
            budget = gameSession.budget,
            stakeholderValue = gameSession.stakeholderValue,
            errorPenalties = gameSession.errorPenalties,
            status = gameSession.status.name,
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

// Composite response DTOs for complex operations
data class GameInitializationResponse(
    val gameSession: GameSessionResponse,
    val availableEmployees: List<GameEmployeeResponse>
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
)