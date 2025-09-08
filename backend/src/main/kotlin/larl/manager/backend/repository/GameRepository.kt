package larl.manager.backend.repository

import larl.manager.backend.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GameSessionRepository : JpaRepository<GameSession, Long> {
    
    // Find active game session for a session ID (replaces user-based lookup)
    fun findBySessionIdAndStatus(sessionId: String, status: GameStatus): Optional<GameSession>
    
    // Get current active game for a session
    @Query("SELECT gs FROM GameSession gs WHERE gs.sessionId = :sessionId AND gs.status = 'ACTIVE'")
    fun findActiveGameBySession(sessionId: String): Optional<GameSession>
    
    // Get all games for a session
    fun findBySessionIdOrderByStartedAtDesc(sessionId: String): List<GameSession>
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
interface EmployeeRepository : JpaRepository<Employee, Long> {
    
    // Get all employees for a game session
    fun findByGameSessionIdAndIsActive(gameSessionId: Long, isActive: Boolean): List<Employee>
    
    // Get employees available for assignment (not assigned to contracts)
    @Query("""
        SELECT ge FROM Employee ge 
        WHERE ge.gameSession.id = :gameSessionId 
        AND ge.isActive = true 
        AND ge.id NOT IN (
            SELECT DISTINCT ca.employee.id FROM ContractAssignment ca 
            WHERE ca.contract.gameSession.id = :gameSessionId 
            AND ca.isActive = true
        )
    """)
    fun findUnassignedEmployees(gameSessionId: Long): List<Employee>
    
    // Get employees by type for hiring UI
    fun findByGameSessionIdAndEmployeeTypeAndIsActive(
        gameSessionId: Long, 
        employeeType: EmployeeType, 
        isActive: Boolean
    ): List<Employee>
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