package larl.manager.backend.controller

import larl.manager.backend.entity.*
import larl.manager.backend.service.*
import larl.manager.backend.dto.request.*
import larl.manager.backend.dto.response.ApiResponse
import larl.manager.backend.dto.response.ContractResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/contracts")
@CrossOrigin(origins = ["http://localhost:5173", "http://localhost:8080"])
class ContractController(
    private val gameSessionService: GameSessionService,
    private val contractService: ContractService
) {



    @GetMapping("/available")
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

    @PostMapping("/{contractId}/accept")
    fun acceptContract(@PathVariable contractId: Long): ResponseEntity<ApiResponse<ContractResponse>> {
        return try {
            val contract = contractService.startContract(contractId)
                ?: return ResponseEntity.badRequest().body(ApiResponse.error("Contract not found"))
            
            ResponseEntity.ok(ApiResponse.success(ContractResponse.from(contract)))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to accept contract"))
        }
    }

    @GetMapping("/active")
    fun getActiveContracts(httpRequest: HttpServletRequest): ResponseEntity<ApiResponse<List<ContractResponse>>> {
        return try {
            val sessionId = getSessionIdFromCookie(httpRequest)
                ?: return ResponseEntity.ok(ApiResponse.success(emptyList(), "No active session"))
            
            val gameSession = gameSessionService.findActiveGameBySession(sessionId)
                ?: return ResponseEntity.ok(ApiResponse.success(emptyList(), "No active game"))
            
            val contracts = contractService.findActiveContracts(gameSession.id)
            ResponseEntity.ok(ApiResponse.success(contracts.map { ContractResponse.from(it) }))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to get active contracts"))
        }
    }

    @PostMapping("/{contractId}/assign/{employeeId}")
    fun assignEmployeeToContract(
        @PathVariable contractId: Long,
        @PathVariable employeeId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            val sessionId = getSessionIdFromCookie(httpRequest)
                ?: return ResponseEntity.badRequest().body(ApiResponse.error("No session found"))
            
            val gameSession = gameSessionService.findActiveGameBySession(sessionId)
                ?: return ResponseEntity.badRequest().body(ApiResponse.error("No active game"))
            
            // Assign employee to contract
            val result = contractService.assignEmployeeToContract(contractId, employeeId, gameSession.currentWeek)
            if (result != null) {
                ResponseEntity.ok(ApiResponse.success("Employee assigned to contract successfully"))
            } else {
                ResponseEntity.badRequest().body(ApiResponse.error("Failed to assign employee"))
            }
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to assign employee"))
        }
    }

    private fun getSessionIdFromCookie(request: HttpServletRequest): String? {
        return request.cookies?.find { it.name == "GAME_SESSION_ID" }?.value
    }
}