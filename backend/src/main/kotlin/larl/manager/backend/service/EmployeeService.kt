package larl.manager.backend.service

import larl.manager.backend.entity.*
import larl.manager.backend.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
@Transactional
class EmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val gameSessionRepository: GameSessionRepository
) {
    
    /**
     * Generate random employee with available types based on user unlocks
     */
    fun generateRandomEmployee(gameSession: GameSession): Employee {
        val employeeType = EmployeeType.ANALYST
        val level = getRandomLevel()
        val baseStats = generateBaseStats(employeeType, level)
        
        // Apply starting morale bonus from perks
        val startingMorale = 100
        
        return Employee(
            gameSession = gameSession,
            name = generateRandomName(),
            employeeType = employeeType,
            level = level,
            speed = baseStats.speed,
            accuracy = baseStats.accuracy,
            salary = baseStats.salary,
            morale = startingMorale.coerceIn(0, 100),
            isActive = false // Not active until hired
        )
    }
    
    /**
     * Hire employee with perk-adjusted costs and bonuses
     */
    fun hireEmployee(gameSessionId: Long, employeeTemplate: Employee): Employee? {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return null
        
        // Calculate hiring cost with perk adjustments
        val Cost = calculateHiringCost(employeeTemplate)
        
        if (!gameSession.canAfford(Cost)) {
            throw IllegalArgumentException("Insufficient funds to hire employee")
        }
        
        val finalMorale = (employeeTemplate.morale).coerceIn(0, 100)
        
        val employee = employeeTemplate.copy(
            gameSession = gameSession,
            morale = finalMorale,
            isActive = true
        )
        
        val savedEmployee = employeeRepository.save(employee)
        
        // Deduct hiring cost from game session budget
        val updatedSession = gameSession.copy(budget = gameSession.budget - Cost)
        gameSessionRepository.save(updatedSession)
        
        return savedEmployee
    }
    
    /**
     * Get active employees for a game session
     */
    fun getActiveEmployees(gameSessionId: Long): List<Employee> {
        return employeeRepository.findByGameSessionIdAndIsActive(gameSessionId, true)
    }
    
    /**
     * Get available employees for assignment (not currently assigned)
     */
    fun getAvailableEmployees(gameSessionId: Long): List<Employee> {
        return employeeRepository.findUnassignedEmployees(gameSessionId)
    }
    
    /**
     * Fire employee
     */
    fun fireEmployee(employeeId: Long): Employee? {
        val employee = employeeRepository.findById(employeeId).orElse(null) ?: return null
        val updatedEmployee = employee.copy(isActive = false)
        return employeeRepository.save(updatedEmployee)
    }
    
    /**
     * Update employee morale
     */
    fun updateMorale(employeeId: Long, moraleChange: Int): Employee? {
        val employee = employeeRepository.findById(employeeId).orElse(null) ?: return null
        val newMorale = (employee.morale + moraleChange).coerceIn(0, 100)
        val updatedEmployee = employee.copy(morale = newMorale)
        return employeeRepository.save(updatedEmployee)
    }
    
    /**
     * Update all employee morale (for events)
     */
    fun updateAllEmployeeMorale(gameSessionId: Long, moraleChange: Int): List<Employee> {
        val employees = getActiveEmployees(gameSessionId)
        return employees.map { employee ->
            val newMorale = (employee.morale + moraleChange).coerceIn(0, 100)
            val updatedEmployee = employee.copy(morale = newMorale)
            employeeRepository.save(updatedEmployee)
        }
    }

    fun calculateTotalSalaries(gameSessionId: Long): Long {
        val employees = getActiveEmployees(gameSessionId)
        return employees.sumOf { it.salary }
    }
    
    /**
     * Check employee retention with perk bonuses
     */
    fun checkEmployeeRetention(gameSessionId: Long): List<Employee> {
        val employees = getActiveEmployees(gameSessionId)
        val quitEmployees = mutableListOf<Employee>()
        
        employees.forEach { employee ->
            val quitChance = employee.getQuitChance() // Already includes perk bonuses
            if (Random.nextInt(1, 101) <= quitChance) {
                val firedEmployee = employee.copy(isActive = false)
                employeeRepository.save(firedEmployee)
                quitEmployees.add(firedEmployee)
            }
        }
        
        return quitEmployees
    }
    
    /**
     * Find employee by ID
     */
    fun findById(employeeId: Long): Employee? {
        return employeeRepository.findById(employeeId).orElse(null)
    }
    
    /**
     * Update employee (for level-ups, etc.)
     */
    fun updateEmployee(employee: Employee): Employee {
        return employeeRepository.save(employee)
    }
    
    // Helper methods
    private fun getRandomLevel(): Int {
        return Random.nextInt(1, 4) // Levels 1-3 for new hires
    }
    
    private fun generateBaseStats(type: EmployeeType, level: Int): EmployeeStats {
        val baseSpeed = when (type) {
            EmployeeType.ANALYST -> Random.nextInt(15, 30)
        }
        
        val baseAccuracy = when (type) {
            EmployeeType.ANALYST -> Random.nextInt(70, 90)
        }
        
        val baseSalary = when (type) {
            EmployeeType.ANALYST -> Random.nextLong(600, 900)
        }
        
        return EmployeeStats(
            speed = baseSpeed + (level * Random.nextInt(3, 8)),
            accuracy = (baseAccuracy + (level * Random.nextInt(2, 6))).coerceAtMost(100),
            salary = baseSalary + (level * Random.nextLong(100, 300))
        )
    }
    
    private fun calculateHiringCost(employee: Employee): Long {
        return employee.salary * 2 // 2 weeks salary as hiring cost
    }
    
    private fun generateRandomName(): String {
        val firstNames = listOf(
            "Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Avery", "Cameron",
            "Quinn", "Sage", "River", "Rowan", "Emery", "Dakota", "Phoenix", "Skyler",
            "Blake", "Parker", "Drew", "Kai", "Reese", "Charlie", "Hayden", "Remy"
        )
        val lastNames = listOf(
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
            "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White"
        )
        return "${firstNames.random()} ${lastNames.random()}"
    }
}

data class EmployeeStats(
    val speed: Int,
    val accuracy: Int,
    val salary: Long
)