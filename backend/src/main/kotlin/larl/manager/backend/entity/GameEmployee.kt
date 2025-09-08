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
    
    // Direct relationship to GameSession (no Company)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    val gameSession: GameSession,
    
    @Column(nullable = false, length = 50)
    @field:NotBlank
    @field:Size(min = 2, max = 50)
    val name: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val employeeType: EmployeeType = EmployeeType.ANALYST,
    
    @Column(nullable = false)
    @field:Min(1)
    val level: Int = 1,
    
    @Column(nullable = false)
    @field:Min(1)
    val speed: Int, // Work points per week
    
    @Column(nullable = false)
    @field:Min(1)
    val accuracy: Int, // Quality percentage (1-100)
    
    @Column(nullable = false)
    @field:Min(1)
    val salary: Long, // Weekly salary
    
    @Column(nullable = false)
    val morale: Int = 100, // Current morale (0-100)
    
    @Column(nullable = false)
    val isActive: Boolean = true, // Currently employed
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val hiredAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "employee", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val assignments: MutableSet<ContractAssignment> = mutableSetOf()
) {
    constructor() : this(
        gameSession = GameSession(
            sessionId = "",
            companyName = ""
        ),
        name = "",
        employeeType = EmployeeType.ANALYST,
        level = 1,
        speed = 10,
        accuracy = 70,
        salary = 1000L,
        morale = 100,
        isActive = true
    )
    
    fun getEffectiveSpeed(): Int {
        val baseMorale = morale / 100.0
        val levelBonus = 1.0 + (level - 1) * 0.1 // 10% per level
        
        return ((speed * baseMorale * levelBonus).toInt()).coerceAtLeast(1)
    }
    
    fun getEffectiveAccuracy(): Int {
        val baseMorale = morale / 100.0
        val levelBonus = 1.0 + (level - 1) * 0.05 // 5% per level
        
        return ((accuracy * baseMorale * levelBonus).toInt()).coerceIn(1, 100)
    }
    
    fun getQuitChance(): Int {
        val baseMorale = morale
        
        val baseChance = when {
            baseMorale >= 80 -> 2
            baseMorale >= 60 -> 5
            baseMorale >= 40 -> 15
            baseMorale >= 20 -> 30
            else -> 50
        }
        
        return (baseChance).coerceAtLeast(0)
    }
    
    fun levelUp(): GameEmployee {
        return this.copy(
            level = level + 1,
            speed = speed + kotlin.random.Random.nextInt(3, 8),
            accuracy = (accuracy + kotlin.random.Random.nextInt(2, 6)).coerceAtMost(100),
            salary = (salary * 1.15).toLong() // 15% raise
        )
    }
}

enum class EmployeeType {
    ANALYST
}