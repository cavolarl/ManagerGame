package larl.manager.backend.dto.response

import larl.manager.backend.entity.Contract
import larl.manager.backend.entity.ContractDifficulty
import larl.manager.backend.entity.ContractStatus

data class ContractResponse(
    val id: Long,
    val title: String,
    val description: String,
    val difficulty: ContractDifficulty,
    val totalWorkRequired: Int,
    val currentProgress: Int,
    val baseReward: Long,
    val stakeholderPoints: Int,
    val deadlineWeeks: Int,
    val weeksRemaining: Int,
    val status: ContractStatus
) {
    companion object {
        fun from(contract: Contract): ContractResponse {
            return ContractResponse(
                id = contract.id,
                title = contract.title,
                description = contract.description ?: "",
                difficulty = contract.difficulty,
                totalWorkRequired = contract.totalWorkRequired,
                currentProgress = contract.currentProgress,
                baseReward = contract.baseReward,
                stakeholderPoints = contract.stakeholderPoints,
                deadlineWeeks = contract.deadlineWeeks,
                weeksRemaining = contract.weeksRemaining,
                status = contract.status
            )
        }
    }
}