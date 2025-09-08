package larl.manager.backend.controller

import larl.manager.backend.entity.*
import larl.manager.backend.service.*
import larl.manager.backend.dto.request.*
import larl.manager.backend.dto.response.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.Cookie
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = ["http://localhost:5173", "http://localhost:8080"])
class GameController(
    private val gameOrchestrationService: GameOrchestrationService,
    private val gameSessionService: GameSessionService,
    private val contractService: ContractService,
    private val employeeService: EmployeeService
) {
    
    // ========== GAME SESSION MANAGEMENT ==========
    
    @PostMapping("/start")
    fun startNewGame(
        @Valid @RequestBody request: StartGameRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ): ResponseEntity<ApiResponse<GameInitializationResponse>> {
        return try {
            // Get or create session ID from cookie (not from request body)
            val sessionId = getOrCreateSessionId(httpRequest, httpResponse)
            
            val result = gameOrchestrationService.initializeNewGame(
                sessionId, // Use cookie-based session ID
                request.companyName
            )
            when (result) {
                is GameInitializationResult.Success -> {
                    val response = GameInitializationResponse(
                        gameSession = GameSessionResponse.from(result.gameSession),
                        availableEmployees = result.availableEmployees.map { GameEmployeeResponse.from(it) }
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

    // Add these helper methods to your controller
    private fun getOrCreateSessionId(request: HttpServletRequest, response: HttpServletResponse): String {
        val existingSessionId = getSessionIdFromCookie(request)
        if (existingSessionId != null) {
            return existingSessionId
        }
        
        val newSessionId = java.util.UUID.randomUUID().toString()
        val cookie = Cookie("GAME_SESSION_ID", newSessionId)
        cookie.maxAge = 60 * 60 * 24 * 30 // 30 days
        cookie.path = "/"
        cookie.isHttpOnly = true
        response.addCookie(cookie)
        
        return newSessionId
    }

    private fun getSessionIdFromCookie(request: HttpServletRequest): String? {
        return request.cookies?.find { it.name == "GAME_SESSION_ID" }?.value
    }

    @GetMapping("/current")
    fun getCurrentGame(httpRequest: HttpServletRequest): ResponseEntity<ApiResponse<GameSessionResponse?>> {
        return try {
            val sessionId = getSessionIdFromCookie(httpRequest)
            if (sessionId == null) {
                return ResponseEntity.ok(ApiResponse.success(null, "No active session"))
            }
            
            val gameSession = gameSessionService.findActiveGameBySession(sessionId)
            if (gameSession != null) {
                ResponseEntity.ok(ApiResponse.success(GameSessionResponse.from(gameSession)))
            } else {
                ResponseEntity.ok(ApiResponse.success(null, "No active game"))
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to get current game"))
        }
    }

    @GetMapping("/employees")
    fun getActiveEmployees(httpRequest: HttpServletRequest): ResponseEntity<ApiResponse<List<GameEmployeeResponse>>> {
        val sessionId = getSessionIdFromCookie(httpRequest) 
            ?: return ResponseEntity.ok(ApiResponse.success(emptyList(), "No active session"))
        
        val gameSession = gameSessionService.findActiveGameBySession(sessionId)
            ?: return ResponseEntity.ok(ApiResponse.success(emptyList(), "No active game"))
        
        val employees = employeeService.getActiveEmployees(gameSession.id)
        return ResponseEntity.ok(ApiResponse.success(employees.map { GameEmployeeResponse.from(it) }))
    }

    @PostMapping("/employees/hire")
    fun hireEmployee(
        @Valid @RequestBody request: HireEmployeeRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<GameEmployeeResponse>> {
        return try {
            val sessionId = getSessionIdFromCookie(httpRequest)
                ?: return ResponseEntity.badRequest().body(ApiResponse.error("No session found"))
            
            val gameSession = gameSessionService.findActiveGameBySession(sessionId)
                ?: return ResponseEntity.badRequest().body(ApiResponse.error("No active game"))
            
            val result = gameOrchestrationService.hireEmployee(gameSession.id, createEmployeeFromRequest(request))
            when (result) {
                is EmployeeHiringResult.Success -> {
                    ResponseEntity.ok(ApiResponse.success(GameEmployeeResponse.from(result.employee), "Employee hired"))
                }
                is EmployeeHiringResult.Failure -> {
                    ResponseEntity.badRequest().body(ApiResponse.error(result.error))
                }
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to hire employee"))
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
        )
    }

    @GetMapping("/contracts")
    fun getContracts(httpRequest: HttpServletRequest): ResponseEntity<ApiResponse<List<ContractResponse>>> {
        return try {
            val sessionId = getSessionIdFromCookie(httpRequest)
                ?: return ResponseEntity.ok(ApiResponse.success(emptyList(), "No active session"))
            
            val gameSession = gameSessionService.findActiveGameBySession(sessionId)
                ?: return ResponseEntity.ok(ApiResponse.success(emptyList(), "No active game"))
            
            val contracts = contractService.getContractsByGameSession(gameSession.id)
            ResponseEntity.ok(ApiResponse.success(contracts.map { ContractResponse.from(it) }))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to get contracts"))
        }
    }

    @GetMapping("/contracts/available")
    fun getAvailableContracts(httpRequest: HttpServletRequest): ResponseEntity<ApiResponse<List<ContractResponse>>> {
        return try {
            val sessionId = getSessionIdFromCookie(httpRequest)
                ?: return ResponseEntity.ok(ApiResponse.success(emptyList(), "No active session"))
            
            val gameSession = gameSessionService.findActiveGameBySession(sessionId)
                ?: return ResponseEntity.ok(ApiResponse.success(emptyList(), "No active game"))
            
            val contracts = contractService.findAvailableContracts(gameSession.id)
            ResponseEntity.ok(ApiResponse.success(contracts.map { ContractResponse.from(it) }))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to get available contracts"))
        }
    }
}