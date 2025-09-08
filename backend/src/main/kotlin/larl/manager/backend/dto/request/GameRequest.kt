package larl.manager.backend.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class StartGameRequest(
    @field:NotBlank(message = "Company name is required")
    @field:Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    val companyName: String
)

data class AssignEmployeeRequest(
    @field:NotNull(message = "Employee ID is required")
    @field:Positive(message = "Employee ID must be positive")
    val employeeId: Long,
    
    @field:NotNull(message = "Game session ID is required")
    @field:Positive(message = "Game session ID must be positive")
    val gameSessionId: Long
)

data class HireEmployeeRequest(
    
    @field:NotBlank(message = "Employee name is required")
    @field:Size(min = 2, max = 50, message = "Employee name must be between 2 and 50 characters")
    val name: String,
    
    @field:NotBlank(message = "Employee type is required")
    val employeeType: String,
    
    @field:NotNull(message = "Level is required")
    @field:Positive(message = "Level must be positive")
    val level: Int,
    
    @field:NotNull(message = "Speed is required")
    @field:Positive(message = "Speed must be positive")
    val speed: Int,
    
    @field:NotNull(message = "Accuracy is required")
    @field:Positive(message = "Accuracy must be positive")
    val accuracy: Int,
    
    @field:NotNull(message = "Salary is required")
    @field:Positive(message = "Salary must be positive")
    val salary: Long,
    
    @field:NotNull(message = "Morale is required")
    val morale: Int
)

// New: End game request
data class EndGameRequest(
    @field:NotNull(message = "Game session ID is required")
    @field:Positive(message = "Game session ID must be positive")
    val gameSessionId: Long,
    
    val reason: String = "Manual end" // Optional reason for ending
)