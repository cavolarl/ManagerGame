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