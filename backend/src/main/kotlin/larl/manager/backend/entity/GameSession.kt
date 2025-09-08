package larl.manager.backend.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "game_sessions")
data class GameSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 255)
    val sessionId: String,
    
    // Company information (embedded in GameSession)
    @Column(nullable = false, length = 100)
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val companyName: String,
    
    // Game state
    @Column(nullable = false)
    val currentQuarter: Int = 1,
    
    @Column(nullable = false)
    val currentWeek: Int = 1, // Week within current quarter (1-13)
    
    @Column(nullable = false)
    val budget: Long = 50000, // Starting budget $50k
    
    @Column(nullable = false)
    val stakeholderValue: Int = 0, // Main score
    
    @Column(nullable = false)
    val errorPenalties: Int = 0,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: GameStatus = GameStatus.ACTIVE,
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column
    val endedAt: LocalDateTime? = null,
    
    // Direct relationships (no Company intermediary)
    @OneToMany(mappedBy = "gameSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val contracts: MutableSet<Contract> = mutableSetOf(),
    
    @OneToMany(mappedBy = "gameSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val employees: MutableSet<GameEmployee> = mutableSetOf()
) {
    fun canAfford(amount: Long): Boolean = budget >= amount
    fun isQuarterEnd(): Boolean = currentWeek >= 13
    fun getTotalScore(): Int = stakeholderValue + (budget / 1000).toInt() - errorPenalties
    fun getEmployeeCount(): Int = employees.size
}

enum class GameStatus {
    ACTIVE, COMPLETED, FAILED, PAUSED
}