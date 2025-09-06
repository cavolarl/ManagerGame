package larl.manager.backend.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "game_employees")
data class GameEmployee(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    val gameSession: GameSession,
    
    @Column(nullable = false, length = 50)
    val name: String,
    
    @Column(nullable = false)
    val speed: Int, // Work points per week (1-10)
    
    @Column(nullable = false)
    val accuracy: Int, // Quality of work percentage (50-95)
    
    @Column(nullable = false)
    val weeklySalary: Long,
    
    @Column(nullable = false)
    val hireCost: Long,
    
    @Column(nullable = false)
    val morale: Int = 100, // Affects performance (0-100)
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val employeeType: EmployeeType = EmployeeType.ANALYST,
    
    @Column(nullable = false)
    val weekHired: Int, // Which week they were hired
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @OneToMany(mappedBy = "employee", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val assignments: MutableSet<ContractAssignment> = mutableSetOf()
) {
    fun getEffectiveSpeed(): Int = ((speed * (morale / 100.0)) * when(employeeType) {
        EmployeeType.ANALYST -> 1.0
        EmployeeType.SENIOR_ANALYST -> 1.2
        EmployeeType.MANAGER -> 0.8 // Managers are slower but provide team bonuses
    }).toInt()
    
    fun getEffectiveAccuracy(): Int = (accuracy * (morale / 100.0)).toInt()
}

enum class EmployeeType {
    ANALYST, SENIOR_ANALYST, MANAGER // Start simple, expand later
}