package larl.manager.backend.repository

import larl.manager.backend.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
}

@Repository
interface GameSessionRepository : JpaRepository<GameSession, Long> {
    
    // Find active game session for a user (simplified - no Company)
    fun findByUserIdAndStatus(userId: Long, status: GameStatus): Optional<GameSession>
    
    // Get current active game for a user
    @Query("SELECT gs FROM GameSession gs WHERE gs.user.id = :userId AND gs.status = 'ACTIVE'")
    fun findActiveGameByUser(userId: Long): Optional<GameSession>
    
    // Get all completed games for a user (for stats/history)
    fun findByUserIdAndStatusOrderByStartedAtDesc(userId: Long, status: GameStatus): List<GameSession>
    
    // Get all games for a user
    fun findByUserIdOrderByStartedAtDesc(userId: Long): List<GameSession>
    
    @Query("SELECT gs FROM GameSession gs WHERE gs.status = 'COMPLETED' ORDER BY gs.stakeholderValue + (gs.budget / 1000) DESC")
    fun findTopGamesByTotalScore(): List<GameSession>
}

@Repository
interface ContractRepository : JpaRepository<Contract, Long> {
    
    // Get all contracts for a specific game session
    fun findByGameSessionIdOrderByDeadlineWeeksAsc(gameSessionId: Long): List<Contract>
    
    // Get contracts by status for a game session
    fun findByGameSessionIdAndStatus(gameSessionId: Long, status: ContractStatus): List<Contract>
    
    // Get active contracts (in progress)
    @Query("SELECT c FROM Contract c WHERE c.gameSession.id = :gameSessionId AND c.status IN ('AVAILABLE', 'IN_PROGRESS')")
    fun findActiveContractsByGameSession(gameSessionId: Long): List<Contract>
}

@Repository
interface GameEmployeeRepository : JpaRepository<GameEmployee, Long> {
    
    // Get all employees for a game session
    fun findByGameSessionIdAndIsActive(gameSessionId: Long, isActive: Boolean): List<GameEmployee>
    
    // Get employees available for assignment (not assigned to contracts)
    @Query("""
        SELECT ge FROM GameEmployee ge 
        WHERE ge.gameSession.id = :gameSessionId 
        AND ge.isActive = true 
        AND ge.id NOT IN (
            SELECT DISTINCT ca.employee.id FROM ContractAssignment ca 
            WHERE ca.contract.gameSession.id = :gameSessionId 
            AND ca.isActive = true
        )
    """)
    fun findUnassignedEmployees(gameSessionId: Long): List<GameEmployee>
    
    // Get employees by type for hiring UI
    fun findByGameSessionIdAndEmployeeTypeAndIsActive(
        gameSessionId: Long, 
        employeeType: EmployeeType, 
        isActive: Boolean
    ): List<GameEmployee>
}

@Repository
interface ContractAssignmentRepository : JpaRepository<ContractAssignment, Long> {
    
    // Find assignment by contract, employee and week
    fun findByContractIdAndEmployeeIdAndWeekAssigned(
        contractId: Long, 
        employeeId: Long, 
        weekAssigned: Int
    ): Optional<ContractAssignment>
    
    // Get all assignments for a contract in a specific week
    fun findByContractIdAndWeekAssignedAndIsActive(
        contractId: Long, 
        weekAssigned: Int, 
        isActive: Boolean
    ): List<ContractAssignment>
    
    // Get all assignments for an employee
    fun findByEmployeeIdAndIsActive(employeeId: Long, isActive: Boolean): List<ContractAssignment>
    
    // Get all assignments for a contract
    fun findByContractIdAndIsActive(contractId: Long, isActive: Boolean): List<ContractAssignment>
    
    // Get all active assignments for a game session
    @Query("""
        SELECT ca FROM ContractAssignment ca 
        WHERE ca.contract.gameSession.id = :gameSessionId 
        AND ca.isActive = true
    """)
    fun findActiveAssignmentsByGameSession(gameSessionId: Long): List<ContractAssignment>
}