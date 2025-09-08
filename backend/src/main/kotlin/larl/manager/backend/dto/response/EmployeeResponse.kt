package larl.manager.backend.dto.response

import larl.manager.backend.entity.*
import java.time.LocalDateTime

data class EmployeeResponse(
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
        fun from(employee: Employee) = EmployeeResponse(
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