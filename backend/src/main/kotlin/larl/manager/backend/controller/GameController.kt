package larl.manager.backend.controller

import larl.manager.backend.entity.*
import larl.manager.backend.service.*
import larl.manager.backend.dto.request.*
import larl.manager.backend.dto.response.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = ["http://localhost:5173", "http://localhost:8080"])
class GameController(
    private val gameOrchestrationService: GameOrchestrationService,
    private val gameSessionService: GameSessionService,
    private val contractService: ContractService,
    private val employeeService: EmployeeService,
    private val metaProgressionService: MetaProgressionService
) {
    
    // ========== GAME SESSION MANAGEMENT ==========
    
    @PostMapping("/start")
    fun startNewGame(@Valid @RequestBody request: StartGameRequest): ResponseEntity<ApiResponse<GameInitializationResponse>> {
        return try {
            val result = gameOrchestrationService.initializeNewGame(
                request.userId, 
                request.companyName, 
                request.selectedPerks
            )
            when (result) {
                is GameInitializationResult.Success -> {
                    val response = GameInitializationResponse(
                        gameSession = GameSessionResponse.from(result.gameSession),
                        availableEmployees = result.availableEmployees.map { GameEmployeeResponse.from(it) },
                        availablePerks = metaProgressionService.getAvailablePerks(request.userId)
                            .map { PerkResponse.from(it, request.selectedPerks.contains(it)) }
                    )
                    ResponseEntity.ok(ApiResponse.success(response, "Game started successfully"))
                }
                is GameInitializationResult.Failure -> {
                    ResponseEntity.badRequest().body(ApiResponse.error(result.error))
                }
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to start game"))
        }
    }
    
    @GetMapping("/session/{gameSessionId}")
    fun getGameSession(@PathVariable gameSessionId: Long): ResponseEntity<ApiResponse<GameSessionResponse>> {
        val gameSession = gameSessionService.findById(gameSessionId)
        return if (gameSession != null) {
            ResponseEntity.ok(ApiResponse.success(GameSessionResponse.from(gameSession)))
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/state/{gameSessionId}")
    fun getGameState(@PathVariable gameSessionId: Long): ResponseEntity<ApiResponse<GameStateResponse>> {
        return try {
            val result = gameOrchestrationService.getGameState(gameSessionId)
            when (result) {
                is GameStateResult.Success -> {
                    val response = GameStateResponse(
                        gameSession = GameSessionResponse.from(result.gameSession),
                        activeEmployees = result.activeEmployees.map { GameEmployeeResponse.from(it) },
                        availableContracts = result.availableContracts.map { ContractResponse.from(it) },
                        activeContracts = result.activeContracts.map { ContractResponse.from(it) }
                    )
                    ResponseEntity.ok(ApiResponse.success(response))
                }
                is GameStateResult.Failure -> {
                    ResponseEntity.badRequest().body(ApiResponse.error(result.error))
                }
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to get game state"))
        }
    }
    
    // ========== TURN MANAGEMENT ==========
    
    @PostMapping("/turn/{gameSessionId}")
    fun processWeekTurn(@PathVariable gameSessionId: Long): ResponseEntity<ApiResponse<WeekTurnResponse>> {
        return try {
            val result = gameOrchestrationService.processWeekTurn(gameSessionId)
            when (result) {
                is WeekTurnResult.Success -> {
                    val response = WeekTurnResponse(
                        gameSession = GameSessionResponse.from(result.gameSession),
                        contractResults = result.contractResults.map { ContractResponse.from(it) },
                        quitEmployees = result.quitEmployees.map { GameEmployeeResponse.from(it) },
                        completedContracts = result.completedContracts.map { ContractResponse.from(it) }
                    )
                    ResponseEntity.ok(ApiResponse.success(response, "Week turn processed successfully"))
                }
                is WeekTurnResult.Failure -> {
                    ResponseEntity.badRequest().body(ApiResponse.error(result.error))
                }
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to process week turn"))
        }
    }
    
    // ========== CONTRACT MANAGEMENT ==========
    
    @GetMapping("/contracts/{gameSessionId}")
    fun getContracts(@PathVariable gameSessionId: Long): ResponseEntity<ApiResponse<List<ContractResponse>>> {
        val contracts = contractService.getContractsByGameSession(gameSessionId)
        return ResponseEntity.ok(ApiResponse.success(contracts.map { ContractResponse.from(it) }))
    }
    
    @GetMapping("/contracts/{gameSessionId}/available")
    fun getAvailableContracts(@PathVariable gameSessionId: Long): ResponseEntity<ApiResponse<List<ContractResponse>>> {
        val contracts = contractService.findAvailableContracts(gameSessionId)
        return ResponseEntity.ok(ApiResponse.success(contracts.map { ContractResponse.from(it) }))
    }
    
    @GetMapping("/contracts/{gameSessionId}/active")
    fun getActiveContracts(@PathVariable gameSessionId: Long): ResponseEntity<ApiResponse<List<ContractResponse>>> {
        val contracts = contractService.findActiveContracts(gameSessionId)
        return ResponseEntity.ok(ApiResponse.success(contracts.map { ContractResponse.from(it) }))
    }
    
    @PostMapping("/contracts/{contractId}/start")
    fun startContract(@PathVariable contractId: Long): ResponseEntity<ApiResponse<ContractResponse>> {
        return try {
            val contract = contractService.startContract(contractId)
            if (contract != null) {
                    ResponseEntity.ok(ApiResponse.success(ContractResponse.from(contract), "Contract started"))
            } else {
                ResponseEntity.badRequest().body(ApiResponse.error("Failed to start contract or contract not found"))
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to start contract"))
        }
    }
    
    @PostMapping("/contracts/{contractId}/assign")
    fun assignEmployeeToContract(
        @PathVariable contractId: Long,
        @Valid @RequestBody request: AssignEmployeeRequest
    ): ResponseEntity<ApiResponse<ContractAssignmentResponse>> {
        return try {
            val result = contractService.assignEmployeeToContract(
                contractId, 
                request.employeeId, 
                gameSessionService.findById(request.gameSessionId)?.currentWeek ?: 1 // Pass current week
            )
            when (result) {
                is ContractAssignment -> {
                    ResponseEntity.ok(ApiResponse.success(
                        ContractAssignmentResponse.from(result), 
                        "Employee assigned to contract"
                    ))
                }
                else -> {
                    ResponseEntity.badRequest().body(ApiResponse.error("Unknown assignment result"))
                }
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to assign employee"))
        }
    }
    
    // ========== EMPLOYEE MANAGEMENT ==========
    
    @GetMapping("/employees/{gameSessionId}")
    fun getActiveEmployees(@PathVariable gameSessionId: Long): ResponseEntity<ApiResponse<List<GameEmployeeResponse>>> {
        val employees = employeeService.getActiveEmployees(gameSessionId)
        return ResponseEntity.ok(ApiResponse.success(employees.map { GameEmployeeResponse.from(it) }))
    }
    
    @GetMapping("/employees/{gameSessionId}/available")
    fun getAvailableEmployees(@PathVariable gameSessionId: Long): ResponseEntity<ApiResponse<List<GameEmployeeResponse>>> {
        val employees = employeeService.getAvailableEmployees(gameSessionId)
        return ResponseEntity.ok(ApiResponse.success(employees.map { GameEmployeeResponse.from(it) }))
    }
    
    @PostMapping("/employees/{gameSessionId}/generate")
    fun generateRandomEmployee(@PathVariable gameSessionId: Long): ResponseEntity<ApiResponse<GameEmployeeResponse>> {
        val gameSession = gameSessionService.findById(gameSessionId)
            ?: return ResponseEntity.badRequest().body(ApiResponse.error("Game session not found"))
        
        val employee = employeeService.generateRandomEmployee(
            gameSession
        )
        
        return ResponseEntity.ok(ApiResponse.success(GameEmployeeResponse.from(employee), "Random employee generated"))
    }
    
    @PostMapping("/employees/hire")
    fun hireEmployee(@Valid @RequestBody request: HireEmployeeRequest): ResponseEntity<ApiResponse<GameEmployeeResponse>> {
        return try {
            val result = gameOrchestrationService.hireEmployee(request.gameSessionId, createEmployeeFromRequest(request))
            when (result) {
                is EmployeeHiringResult.Success -> {
                    ResponseEntity.ok(ApiResponse.success(
                        GameEmployeeResponse.from(result.employee), 
                        "Employee hired successfully"
                    ))
                }
                is EmployeeHiringResult.Failure -> {
                    ResponseEntity.badRequest().body(ApiResponse.error(result.error))
                }
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to hire employee"))
        }
    }
    
    @PostMapping("/employees/{employeeId}/fire")
    fun fireEmployee(@PathVariable employeeId: Long): ResponseEntity<ApiResponse<GameEmployeeResponse>> {
        return try {
            val employee = employeeService.fireEmployee(employeeId)
            if (employee != null) {
                ResponseEntity.ok(ApiResponse.success(GameEmployeeResponse.from(employee), "Employee fired"))
            } else {
                ResponseEntity.badRequest().body(ApiResponse.error("Failed to fire employee"))
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to fire employee"))
        }
    }

    private fun createEmployeeFromRequest(request: HireEmployeeRequest): GameEmployee {
    return GameEmployee().copy(
        name = request.name,
        employeeType = EmployeeType.valueOf(request.employeeType.uppercase()),
        level = request.level,
        speed = request.speed,
        accuracy = request.accuracy,
        salary = request.salary,
        morale = request.morale,
        isActive = false
        // gameSession will be replaced in service layer, no need to set it here
    )
}


}