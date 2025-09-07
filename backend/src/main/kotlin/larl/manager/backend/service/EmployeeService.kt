package larl.manager.backend.service

import larl.manager.backend.entity.*
import larl.manager.backend.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
@Transactional
class EmployeeService(
    private val gameEmployeeRepository: GameEmployeeRepository,
    private val gameSessionRepository: GameSessionRepository
) {
    
    /**
     * Generate random employee with available types based on user unlocks
     */
    fun generateRandomEmployee(gameSession: GameSession): GameEmployee {
        val employeeType = EmployeeType.ANALYST
        val level = getRandomLevel()
        val baseStats = generateBaseStats(employeeType, level)
        
        // Apply starting morale bonus from perks
        val startingMorale = 100 + gameSession.getStartingMoraleBonus()
        
        return GameEmployee(
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
    fun hireEmployee(gameSessionId: Long, employeeTemplate: GameEmployee): GameEmployee? {
        val gameSession = gameSessionRepository.findById(gameSessionId).orElse(null) ?: return null
        
        // Calculate hiring cost with perk adjustments
        val baseCost = calculateHiringCost(employeeTemplate)
        val adjustedCost = (baseCost * gameSession.getHiringCostMultiplier()).toLong()
        
        if (!gameSession.canAfford(adjustedCost)) {
            throw IllegalArgumentException("Insufficient funds to hire employee")
        }
        
        // Apply starting morale bonus from perks
        val startingMoraleBonus = gameSession.getStartingMoraleBonus()
        val finalMorale = (employeeTemplate.morale + startingMoraleBonus).coerceIn(0, 100)
        
        val employee = employeeTemplate.copy(
            gameSession = gameSession,
            morale = finalMorale,
            isActive = true
        )
        
        val savedEmployee = gameEmployeeRepository.save(employee)
        
        // Deduct hiring cost from game session budget
        val updatedSession = gameSession.copy(budget = gameSession.budget - adjustedCost)
        gameSessionRepository.save(updatedSession)
        
        return savedEmployee
    }
    
    /**
     * Get active employees for a game session
     */
    fun getActiveEmployees(gameSessionId: Long): List<GameEmployee> {
        return gameEmployeeRepository.findByGameSessionIdAndIsActive(gameSessionId, true)
    }
    
    /**
     * Get available employees for assignment (not currently assigned)
     */
    fun getAvailableEmployees(gameSessionId: Long): List<GameEmployee> {
        return gameEmployeeRepository.findUnassignedEmployees(gameSessionId)
    }
    
    /**
     * Fire employee
     */
    fun fireEmployee(employeeId: Long): GameEmployee? {
        val employee = gameEmployeeRepository.findById(employeeId).orElse(null) ?: return null
        val updatedEmployee = employee.copy(isActive = false)
        return gameEmployeeRepository.save(updatedEmployee)
    }
    
    /**
     * Update employee morale
     */
    fun updateMorale(employeeId: Long, moraleChange: Int): GameEmployee? {
        val employee = gameEmployeeRepository.findById(employeeId).orElse(null) ?: return null
        val newMorale = (employee.morale + moraleChange).coerceIn(0, 100)
        val updatedEmployee = employee.copy(morale = newMorale)
        return gameEmployeeRepository.save(updatedEmployee)
    }
    
    /**
     * Apply weekly morale bonus from perks to all employees
     */
    fun applyWeeklyMoraleBonus(gameSessionId: Long, bonusAmount: Int): List<GameEmployee> {
        val employees = getActiveEmployees(gameSessionId)
        return employees.map { employee ->
            val newMorale = (employee.morale + bonusAmount).coerceIn(0, 100)
            val updatedEmployee = employee.copy(morale = newMorale)
            gameEmployeeRepository.save(updatedEmployee)
        }
    }
    
    /**
     * Update all employee morale (for events)
     */
    fun updateAllEmployeeMorale(gameSessionId: Long, moraleChange: Int): List<GameEmployee> {
        val employees = getActiveEmployees(gameSessionId)
        return employees.map { employee ->
            val newMorale = (employee.morale + moraleChange).coerceIn(0, 100)
            val updatedEmployee = employee.copy(morale = newMorale)
            gameEmployeeRepository.save(updatedEmployee)
        }
    }

    fun calculateTotalSalaries(gameSessionId: Long): Long {
        val employees = getActiveEmployees(gameSessionId)
        return employees.sumOf { it.salary }
    }
    
    /**
     * Check employee retention with perk bonuses
     */
    fun checkEmployeeRetention(gameSessionId: Long): List<GameEmployee> {
        val employees = getActiveEmployees(gameSessionId)
        val quitEmployees = mutableListOf<GameEmployee>()
        
        employees.forEach { employee ->
            val quitChance = employee.getQuitChance() // Already includes perk bonuses
            if (Random.nextInt(1, 101) <= quitChance) {
                val firedEmployee = employee.copy(isActive = false)
                gameEmployeeRepository.save(firedEmployee)
                quitEmployees.add(firedEmployee)
            }
        }
        
        return quitEmployees
    }
    
    /**
     * Find employee by ID
     */
    fun findById(employeeId: Long): GameEmployee? {
        return gameEmployeeRepository.findById(employeeId).orElse(null)
    }
    
    /**
     * Update employee (for level-ups, etc.)
     */
    fun updateEmployee(employee: GameEmployee): GameEmployee {
        return gameEmployeeRepository.save(employee)
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
    
    private fun calculateHiringCost(employee: GameEmployee): Long {
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