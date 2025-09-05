package larl.manager.backend.controller

import larl.manager.backend.entity.*
import larl.manager.backend.service.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8080"])
class UserController(
    private val userService: UserService
) {
    
    @PostMapping("/register")
    fun registerUser(@RequestBody request: UserRegistrationRequest): ResponseEntity<ApiResponse<UserResponse>> {
        return try {
            val user = userService.createUser(
                username = request.username,
                email = request.email,
                passwordHash = request.password
            )
            ResponseEntity.ok(ApiResponse.success(UserResponse.from(user), "User registered successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Registration failed"))
        }
    }
    
    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.findById(id)
        return if (user != null) {
            ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)))
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

@RestController
@RequestMapping("/api/companies")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8080"])
class CompanyController(
    private val companyService: CompanyService
) {
    
    @PostMapping("/create")
    fun createCompany(@RequestBody request: CreateCompanyRequest): ResponseEntity<ApiResponse<CompanyResponse>> {
        return try {
            val company = companyService.createCompany(request.userId, request.companyName)
            if (company != null) {
                ResponseEntity.ok(ApiResponse.success(CompanyResponse.from(company), "Company created successfully"))
            } else {
                ResponseEntity.badRequest().body(ApiResponse.error("Failed to create company"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to create company"))
        }
    }
    
    @GetMapping("/user/{userId}")
    fun getCompanyByUser(@PathVariable userId: Long): ResponseEntity<ApiResponse<CompanyResponse>> {
        val company = companyService.getCompanyByUserId(userId)
        return if (company != null) {
            ResponseEntity.ok(ApiResponse.success(CompanyResponse.from(company)))
        } else {
            ResponseEntity.notFound().build()
        }
    }
}

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8080"])
class EmployeeController(
    private val employeeService: EmployeeService
) {
    
    @GetMapping("/available")
    fun getAvailableEmployees(): ResponseEntity<ApiResponse<List<EmployeeResponse>>> {
        val employees = employeeService.getAvailableEmployees()
        return ResponseEntity.ok(ApiResponse.success(employees.map { EmployeeResponse.from(it) }))
    }
    
    @GetMapping("/company/{companyId}")
    fun getCompanyEmployees(@PathVariable companyId: Long): ResponseEntity<ApiResponse<List<EmployeeResponse>>> {
        val employees = employeeService.getCompanyEmployees(companyId)
        return ResponseEntity.ok(ApiResponse.success(employees.map { EmployeeResponse.from(it) }))
    }
    
    @PostMapping("/hire")
    fun hireEmployee(@RequestBody request: HireEmployeeRequest): ResponseEntity<ApiResponse<EmployeeResponse>> {
        return try {
            val employee = employeeService.hireEmployee(request.companyId, request.employeeId)
            if (employee != null) {
                ResponseEntity.ok(ApiResponse.success(EmployeeResponse.from(employee), "Employee hired successfully"))
            } else {
                ResponseEntity.badRequest().body(ApiResponse.error("Failed to hire employee"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to hire employee"))
        }
    }
    
    @PostMapping("/generate")
    fun generateRandomEmployee(): ResponseEntity<ApiResponse<EmployeeResponse>> {
        val employee = employeeService.generateRandomEmployee()
        return ResponseEntity.ok(ApiResponse.success(EmployeeResponse.from(employee), "Random employee generated"))
    }
}

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8080"])
class ReportController(
    private val reportService: ReportService
) {
    
    @PostMapping("/start")
    fun startReport(@RequestBody request: StartReportRequest): ResponseEntity<ApiResponse<ReportResponse>> {
        return try {
            val report = reportService.startReport(request.companyId, request.employeeId)
            if (report != null) {
                ResponseEntity.ok(ApiResponse.success(ReportResponse.from(report), "Report started"))
            } else {
                ResponseEntity.badRequest().body(ApiResponse.error("Failed to start report"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to start report"))
        }
    }
    
    @PostMapping("/{reportId}/complete")
    fun completeReport(@PathVariable reportId: Long): ResponseEntity<ApiResponse<ReportResponse>> {
        return try {
            val report = reportService.completeReport(reportId)
            if (report != null) {
                ResponseEntity.ok(ApiResponse.success(ReportResponse.from(report), "Report completed"))
            } else {
                ResponseEntity.badRequest().body(ApiResponse.error("Failed to complete report"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to complete report"))
        }
    }
    
    @GetMapping("/company/{companyId}/active")
    fun getActiveReports(@PathVariable companyId: Long): ResponseEntity<ApiResponse<List<ReportResponse>>> {
        val reports = reportService.getActiveReports(companyId)
        return ResponseEntity.ok(ApiResponse.success(reports.map { ReportResponse.from(it) }))
    }
}

// Request DTOs
data class UserRegistrationRequest(
    val username: String,
    val email: String,
    val password: String
)

data class CreateCompanyRequest(
    val userId: Long,
    val companyName: String
)

data class HireEmployeeRequest(
    val companyId: Long,
    val employeeId: Long
)

data class StartReportRequest(
    val companyId: Long,
    val employeeId: Long
)

// Response DTOs
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
) {
    companion object {
        fun <T> success(data: T, message: String? = null) = ApiResponse(true, data, message)
        fun <T> error(message: String) = ApiResponse<T>(false, null, message)
    }
}

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val createdAt: String
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            createdAt = user.createdAt.toString()
        )
    }
}

data class CompanyResponse(
    val id: Long,
    val name: String,
    val budget: Long,
    val employeeCount: Int
) {
    companion object {
        fun from(company: Company) = CompanyResponse(
            id = company.id,
            name = company.name,
            budget = company.budget,
            employeeCount = company.employees.size
        )
    }
}

data class EmployeeResponse(
    val id: Long,
    val name: String,
    val communication: Int,
    val problemSolving: Int,
    val creativity: Int,
    val luck: Int,
    val teamwork: Int,
    val salary: Long,
    val hireCost: Long,
    val isAvailableForHiring: Boolean,
    val companyId: Long?
) {
    companion object {
        fun from(employee: Employee) = EmployeeResponse(
            id = employee.id,
            name = employee.name,
            communication = employee.communication,
            problemSolving = employee.problemSolving,
            creativity = employee.creativity,
            luck = employee.luck,
            teamwork = employee.teamwork,
            salary = employee.salary,
            hireCost = employee.hireCost,
            isAvailableForHiring = employee.isAvailableForHiring,
            companyId = employee.company?.id
        )
    }
}

data class ReportResponse(
    val id: Long,
    val title: String,
    val employeeName: String,
    val status: String,
    val quality: Int,
    val reward: Long,
    val timeToComplete: Int,
    val startedAt: String,
    val completedAt: String?
) {
    companion object {
        fun from(report: Report) = ReportResponse(
            id = report.id,
            title = report.title,
            employeeName = report.employee.name,
            status = report.status.name,
            quality = report.quality,
            reward = report.reward,
            timeToComplete = report.timeToComplete,
            startedAt = report.startedAt.toString(),
            completedAt = report.completedAt?.toString()
        )
    }
}