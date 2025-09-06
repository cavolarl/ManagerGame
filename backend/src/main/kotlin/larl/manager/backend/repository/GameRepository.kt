package larl.manager.backend.repository

import larl.manager.backend.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.Modifying
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
interface CompanyRepository : JpaRepository<Company, Long> {
    fun findByUserId(userId: Long): Optional<Company>
    fun findByUser(user: User): Optional<Company>
}

@Repository
interface GameSessionRepository : JpaRepository<GameSession, Long> {
    
    // Find active game session for a company
    fun findByCompanyIdAndStatus(companyId: Long, status: GameStatus): Optional<GameSession>
    
    // Get current active game for a company
    @Query("SELECT gs FROM GameSession gs WHERE gs.company.id = :companyId AND gs.status = 'ACTIVE'")
    fun findActiveGameByCompany(companyId: Long): Optional<GameSession>
    
    // Get all completed games for a company (for stats/meta-progression)
    fun findByCompanyIdAndStatusOrderByStartedAtDesc(companyId: Long, status: GameStatus): List<GameSession>
    
    // Find games by quarter for leaderboards
    fun findByCurrentQuarterGreaterThanEqualOrderByStakeholderValueDesc(quarter: Int): List<GameSession>
    
    // Get game sessions that ended this week (for cleanup/processing)
    fun findByStatusAndEndedAtIsNotNull(status: GameStatus): List<GameSession>
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
    
    // Get overdue contracts
    @Query("SELECT c FROM Contract c WHERE c.gameSession.id = :gameSessionId AND c.weeksRemaining <= 0 AND c.status = 'IN_PROGRESS'")
    fun findOverdueContractsByGameSession(gameSessionId: Long): List<Contract>
    
    // Get completed contracts for scoring
    fun findByGameSessionIdAndStatusOrderByCompletionDateDesc(gameSessionId: Long, status: ContractStatus): List<Contract>
    
    // Find contracts by difficulty for generation balancing
    fun findByGameSessionIdAndDifficulty(gameSessionId: Long, difficulty: ContractDifficulty): List<Contract>
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
    
    // Get employees with low morale (for events/warnings)
    @Query("SELECT ge FROM GameEmployee ge WHERE ge.gameSession.id = :gameSessionId AND ge.morale < :threshold AND ge.isActive = true")
    fun findLowMoraleEmployees(gameSessionId: Long, threshold: Int): List<GameEmployee>
    
    // Calculate total weekly salary cost 
    @Query("SELECT COALESCE(SUM(ge.weeklySalary), 0) FROM GameEmployee ge WHERE ge.gameSession.id = :gameSessionId AND ge.isActive = true")
    fun calculateTotalWeeklySalary(gameSessionId: Long): Long
}

@Repository
interface ContractAssignmentRepository : JpaRepository<ContractAssignment, Long> {
    
    // Get current assignments for a contract
    fun findByContractIdAndIsActive(contractId: Long, isActive: Boolean): List<ContractAssignment>
    
    // Get current assignments for an employee
    fun findByEmployeeIdAndIsActive(employeeId: Long, isActive: Boolean): List<ContractAssignment>
    
    // Get all active assignments for a game session
    @Query("""
        SELECT ca FROM ContractAssignment ca 
        WHERE ca.contract.gameSession.id = :gameSessionId 
        AND ca.isActive = true
    """)
    fun findActiveAssignmentsByGameSession(gameSessionId: Long): List<ContractAssignment>
    
    // Check if employee is already assigned to this contract
    fun findByContractIdAndEmployeeIdAndIsActive(
        contractId: Long, 
        employeeId: Long, 
        isActive: Boolean
    ): Optional<ContractAssignment>
    
    // Get assignments for a specific week (for historical tracking)
    fun findByWeekAssignedAndIsActive(week: Int, isActive: Boolean): List<ContractAssignment>
}

@Repository
interface WeeklyEventRepository : JpaRepository<WeeklyEvent, Long> {
    
    // Get events for a specific game session and week
    fun findByGameSessionIdAndWeek(gameSessionId: Long, week: Int): List<WeeklyEvent>
    
    // Get unprocessed events
    fun findByGameSessionIdAndHasBeenProcessed(gameSessionId: Long, hasBeenProcessed: Boolean): List<WeeklyEvent>
    
    // Get events by type for a game session
    fun findByGameSessionIdAndEventType(gameSessionId: Long, eventType: EventType): List<WeeklyEvent>
    
    // Get recent events for UI display
    @Query("SELECT we FROM WeeklyEvent we WHERE we.gameSession.id = :gameSessionId ORDER BY we.week DESC, we.occurredAt DESC")
    fun findRecentEventsByGameSession(gameSessionId: Long): List<WeeklyEvent>
    
    // Get events for current quarter
    fun findByGameSessionIdAndQuarter(gameSessionId: Long, quarter: Int): List<WeeklyEvent>
}