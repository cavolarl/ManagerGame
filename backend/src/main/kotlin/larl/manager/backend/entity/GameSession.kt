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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    val company: Company,
    
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
    
    // One-to-many relationships
    @OneToMany(mappedBy = "gameSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val contracts: MutableSet<Contract> = mutableSetOf(),
    
    @OneToMany(mappedBy = "gameSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val gameEmployees: MutableSet<GameEmployee> = mutableSetOf(),
    
    @OneToMany(mappedBy = "gameSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val weeklyEvents: MutableSet<WeeklyEvent> = mutableSetOf()
) {
    fun canAfford(amount: Long): Boolean = budget >= amount
    fun isQuarterEnd(): Boolean = currentWeek >= 13
    fun getTotalScore(): Int = stakeholderValue + (budget / 1000).toInt() - errorPenalties
}

enum class GameStatus {
    ACTIVE, COMPLETED, FAILED, PAUSED
}