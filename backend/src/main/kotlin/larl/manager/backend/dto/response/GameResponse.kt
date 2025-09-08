package larl.manager.backend.dto.response

import larl.manager.backend.entity.*
import larl.manager.backend.dto.response.ContractResponse
import larl.manager.backend.dto.response.EmployeeResponse
import larl.manager.backend.dto.response.GameSessionResponse

data class GameInitializationResponse(
    val gameSession: GameSessionResponse,
    val availableEmployees: List<EmployeeResponse>
)

data class GameStateResponse(
    val gameSession: GameSessionResponse,
    val activeEmployees: List<EmployeeResponse>,
    val availableContracts: List<ContractResponse>,
    val activeContracts: List<ContractResponse>
)

data class WeekTurnResponse(
    val gameSession: GameSessionResponse,
    val contractResults: List<ContractResponse>,
    val quitEmployees: List<EmployeeResponse>,
    val completedContracts: List<ContractResponse>
)

data class GameEndResponse(
    val gameSession: GameSessionResponse,
    val finalScore: Int,
)