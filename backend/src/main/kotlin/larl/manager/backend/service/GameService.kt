package larl.manager.backend.service

import larl.manager.backend.entity.*
import larl.manager.backend.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Service
@Transactional
class CompanyService(
    private val companyRepository: CompanyRepository,
    private val userRepository: UserRepository
) {
    
    fun createCompany(userId: Long, companyName: String): Company? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        
        // Check if user already has a company
        if (companyRepository.findByUserId(userId).isPresent) {
            throw IllegalArgumentException("User already has a company")
        }
        
        val company = Company(
            user = user,
            name = companyName
        )
        
        return companyRepository.save(company)
    }
    
    fun getCompanyByUserId(userId: Long): Company? {
        return companyRepository.findByUserId(userId).orElse(null)
    }
    
    fun spendMoney(companyId: Long, amount: Long): Company? {
        val company = companyRepository.findById(companyId).orElse(null) ?: return null
        if (company.budget < amount) {
            throw IllegalArgumentException("Insufficient budget")
        }
        
        val updatedCompany = company.copy(budget = company.budget - amount)
        return companyRepository.save(updatedCompany)
    }
    
    fun earnMoney(companyId: Long, amount: Long): Company? {
        val company = companyRepository.findById(companyId).orElse(null) ?: return null
        val updatedCompany = company.copy(budget = company.budget + amount)
        return companyRepository.save(updatedCompany)
    }
}

@Service
@Transactional
class EmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val companyService: CompanyService
) {
    
    fun hireEmployee(companyId: Long, employeeId: Long): Employee? {
        val employee = employeeRepository.findById(employeeId).orElse(null) ?: return null
        
        if (!employee.isAvailableForHiring) {
            throw IllegalArgumentException("Employee not available for hiring")
        }
        
        // Spend money to hire
        val company = companyService.spendMoney(companyId, employee.hireCost) ?: return null
        
        val hiredEmployee = employee.copy(
            company = company,
            isAvailableForHiring = false
        )
        
        return employeeRepository.save(hiredEmployee)
    }
    
    fun getAvailableEmployees(): List<Employee> {
        return employeeRepository.findAvailableForHiring()
    }
    
    fun getCompanyEmployees(companyId: Long): List<Employee> {
        return employeeRepository.findByCompanyId(companyId)
    }
    
    fun generateRandomEmployee(): Employee {
        val names = listOf("Alice", "Beata", "Calle", "Danne", "Erik", "Johan", "Jakob", "Henrik", "Isabelle", "Kim")
        val name = names[Random.nextInt(names.size)]
        
        val employee = Employee(
            name = name,
            communication = Random.nextInt(20, 81), // 20-80
            problemSolving = Random.nextInt(20, 81),
            creativity = Random.nextInt(20, 81),
            luck = Random.nextInt(20, 81),
            teamwork = Random.nextInt(20, 81),
            salary = Random.nextLong(3000, 8001), // $3k-8k monthly
            hireCost = Random.nextLong(5000, 15001) // $5k-15k to hire
        )
        
        return employeeRepository.save(employee)
    }
}

@Service
@Transactional
class ReportService(
    private val reportRepository: ReportRepository,
    private val employeeRepository: EmployeeRepository,
    private val companyService: CompanyService
) {
    
    fun startReport(companyId: Long, employeeId: Long): Report? {
        val employee = employeeRepository.findById(employeeId).orElse(null) ?: return null
        
        if (employee.company?.id != companyId) {
            throw IllegalArgumentException("Employee doesn't work for this company")
        }
        
        // Check if employee is already working on a report
        val activeReports = reportRepository.findByEmployeeId(employeeId)
            .filter { it.status == ReportStatus.IN_PROGRESS }
        
        if (activeReports.isNotEmpty()) {
            throw IllegalArgumentException("Employee is already working on a report")
        }
        
        val report = Report(
            company = employee.company!!,
            employee = employee,
            title = "Report by ${employee.name}",
            timeToComplete = calculateTimeToComplete(employee)
        )
        
        return reportRepository.save(report)
    }
    
    fun completeReport(reportId: Long): Report? {
        val report = reportRepository.findById(reportId).orElse(null) ?: return null
        
        if (report.status != ReportStatus.IN_PROGRESS) {
            throw IllegalArgumentException("Report is not in progress")
        }
        
        val quality = calculateReportQuality(report.employee)
        val reward = calculateReward(quality)
        
        val completedReport = report.copy(
            status = ReportStatus.COMPLETED,
            completedAt = LocalDateTime.now(),
            quality = quality,
            reward = reward
        )
        
        // Pay the company
        companyService.earnMoney(report.company.id, reward)
        
        return reportRepository.save(completedReport)
    }
    
    fun getActiveReports(companyId: Long): List<Report> {
        return reportRepository.findActiveReportsByCompany(companyId)
    }
    
    private fun calculateTimeToComplete(employee: Employee): Int {
        // Base time 60 minutes, reduced by problem solving and teamwork
        val efficiency = (employee.problemSolving + employee.teamwork) / 2
        return maxOf(30, 120 - efficiency) // 30-120 minutes
    }
    
    private fun calculateReportQuality(employee: Employee): Int {
        // Quality based on all skills with some randomness
        val baseQuality = (employee.communication + employee.problemSolving + 
                          employee.creativity + employee.teamwork) / 4
        val luckFactor = Random.nextInt(-10, 11) * (employee.luck / 100.0)
        
        return (baseQuality + luckFactor).toInt().coerceIn(1, 100)
    }
    
    private fun calculateReward(quality: Int): Long {
        // $100-1000 based on quality
        return (100 + (quality * 9)).toLong()
    }
}