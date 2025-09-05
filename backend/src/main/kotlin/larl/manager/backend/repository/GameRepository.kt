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
interface CompanyRepository : JpaRepository<Company, Long> {
    fun findByUserId(userId: Long): Optional<Company>
    fun findByUser(user: User): Optional<Company>
}

@Repository
interface EmployeeRepository : JpaRepository<Employee, Long> {
    fun findByCompanyId(companyId: Long): List<Employee>
    fun findByIsAvailableForHiringTrue(): List<Employee>
    
    @Query("SELECT e FROM Employee e WHERE e.company IS NULL AND e.isAvailableForHiring = true")
    fun findAvailableForHiring(): List<Employee>
}

@Repository
interface ReportRepository : JpaRepository<Report, Long> {
    fun findByCompanyId(companyId: Long): List<Report>
    fun findByEmployeeId(employeeId: Long): List<Report>
    fun findByStatus(status: ReportStatus): List<Report>
    
    @Query("SELECT r FROM Report r WHERE r.company.id = :companyId AND r.status = 'IN_PROGRESS'")
    fun findActiveReportsByCompany(companyId: Long): List<Report>
}