package larl.manager.backend.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "companies")
data class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,
    
    @Column(nullable = false, length = 100)
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val name: String,
    
    @Column(nullable = false)
    @field:Min(0)
    val budget: Long = 50000, // Starting money
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    // One-to-many relationship with Employee
    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val employees: MutableSet<Employee> = mutableSetOf(),
    
    // One-to-many relationship with Report
    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val reports: MutableSet<Report> = mutableSetOf()
) {
    constructor() : this(user = User(), name = "")
    
    // Helper methods
    fun addEmployee(employee: Employee) {
        employees.add(employee)
    }
    
    fun canAfford(amount: Long): Boolean = budget >= amount
    
    fun getEmployeeCount(): Int = employees.size
}