package larl.manager.backend.entity

import jakarta.persistence.*

@Entity
@Table(name = "employees")
data class Employee(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(nullable = false)
    val communication: Int, // 1-100
    
    @Column(nullable = false)
    val problemSolving: Int, // 1-100
    
    @Column(nullable = false)
    val creativity: Int, // 1-100
    
    @Column(nullable = false)
    val luck: Int, // 1-100
    
    @Column(nullable = false)
    val teamwork: Int, // 1-100
    
    @Column(nullable = false)
    val salary: Long, // Monthly cost
    
    @Column(nullable = false)
    val hireCost: Long, // One-time cost to hire
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    val company: Company? = null,
    
    @Column(nullable = false)
    val isAvailableForHiring: Boolean = true
)