package larl.manager.backend.controller

import larl.manager.backend.entity.*
import larl.manager.backend.service.*
import larl.manager.backend.dto.request.*
import larl.manager.backend.dto.response.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = ["http://localhost:5173", "http://localhost:8080"])
class EmployeeController(
    private val gameOrchestrationService: GameOrchestrationService,
    private val gameSessionService: GameSessionService,
    private val employeeService: EmployeeService
) {

    @GetMapping
    fun getActiveEmployees(httpRequest: HttpServletRequest): ResponseEntity<ApiResponse<List<EmployeeResponse>>> {
        val sessionId = getSessionIdFromCookie(httpRequest) 
            ?: return ResponseEntity.ok(ApiResponse.success(emptyList(), "No active session"))
        
        val gameSession = gameSessionService.findActiveGameBySession(sessionId)
            ?: return ResponseEntity.ok(ApiResponse.success(emptyList(), "No active game"))
        
        val employees = employeeService.getActiveEmployees(gameSession.id)
        return ResponseEntity.ok(ApiResponse.success(employees.map { EmployeeResponse.from(it) }))
    }

    @PostMapping("/hire")
    fun hireEmployee(
        @Valid @RequestBody request: HireEmployeeRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<EmployeeResponse>> {
        return try {
            val sessionId = getSessionIdFromCookie(httpRequest)
                ?: return ResponseEntity.badRequest().body(ApiResponse.error("No session found"))
            
            val gameSession = gameSessionService.findActiveGameBySession(sessionId)
                ?: return ResponseEntity.badRequest().body(ApiResponse.error("No active game"))
            
            val result = gameOrchestrationService.hireEmployee(gameSession.id, createEmployeeFromRequest(request))
            when (result) {
                is EmployeeHiringResult.Success -> {
                    ResponseEntity.ok(ApiResponse.success(EmployeeResponse.from(result.employee), "Employee hired"))
                }
                is EmployeeHiringResult.Failure -> {
                    ResponseEntity.badRequest().body(ApiResponse.error(result.error))
                }
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to hire employee"))
        }
    }

    private fun createEmployeeFromRequest(request: HireEmployeeRequest): Employee {
        return Employee().copy(
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

    private fun getSessionIdFromCookie(request: HttpServletRequest): String? {
        return request.cookies?.find { it.name == "GAME_SESSION_ID" }?.value
    }
}