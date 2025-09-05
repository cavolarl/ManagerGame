package larl.manager.backend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "reports")
data class Report(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    val company: Company,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    val employee: Employee,
    
    @Column(nullable = false)
    val title: String,
    
    @Column(nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column
    val completedAt: LocalDateTime? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ReportStatus = ReportStatus.IN_PROGRESS,
    
    @Column(nullable = false)
    val quality: Int = 0, // 0-100, calculated when completed
    
    @Column(nullable = false)
    val reward: Long = 0, // Money earned when completed
    
    @Column(nullable = false)
    val timeToComplete: Int = 60 // minutes required
)

enum class ReportStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED
}