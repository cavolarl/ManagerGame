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
    
    // Direct relationship to User (no Company entity)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
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
    
    // Applied perks for this run
    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "game_session_perks", joinColumns = [JoinColumn(name = "game_session_id")])
    @Column(name = "perk")
    val appliedPerks: MutableSet<CompanyPerk> = mutableSetOf(),
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column
    val endedAt: LocalDateTime? = null,
    
    // Direct relationships (no Company intermediary)
    @OneToMany(mappedBy = "gameSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val contracts: MutableSet<Contract> = mutableSetOf(),
    
    @OneToMany(mappedBy = "gameSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val employees: MutableSet<GameEmployee> = mutableSetOf(),
    
    @OneToMany(mappedBy = "gameSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val weeklyEvents: MutableSet<WeeklyEvent> = mutableSetOf()
) {
    fun canAfford(amount: Long): Boolean = budget >= amount
    fun isQuarterEnd(): Boolean = currentWeek >= 13
    fun getTotalScore(): Int = stakeholderValue + (budget / 1000).toInt() - errorPenalties
    fun getEmployeeCount(): Int = employees.size
    
    // Perk-based calculations
    fun getStartingBudget(): Long {
        return if (appliedPerks.contains(CompanyPerk.BUDGET_BOOST)) {
            55000L // +5k bonus
        } else {
            50000L
        }
    }
    
    fun getTrainingCostMultiplier(): Double {
        return if (appliedPerks.contains(CompanyPerk.CHEAPER_TRAINING)) {
            0.75 // 25% off
        } else {
            1.0
        }
    }
    
    fun getHiringCostMultiplier(): Double {
        return if (appliedPerks.contains(CompanyPerk.FASTER_HIRING)) {
            0.8 // 20% off
        } else {
            1.0
        }
    }
    
    fun getWeeklyMoraleBonus(): Int {
        return if (appliedPerks.contains(CompanyPerk.MORALE_BONUS)) {
            5
        } else {
            0
        }
    }
    
    fun getStartingMoraleBonus(): Int {
        return if (appliedPerks.contains(CompanyPerk.BETTER_ONBOARDING)) {
            10
        } else {
            0
        }
    }
    
    fun getContractRewardMultiplier(): Double {
        return if (appliedPerks.contains(CompanyPerk.CONTRACT_NEGOTIATOR)) {
            1.15 // 15% bonus
        } else {
            1.0
        }
    }
    
    fun getEmployeeEfficiencyMultiplier(): Double {
        return if (appliedPerks.contains(CompanyPerk.EFFICIENCY_EXPERT)) {
            1.1 // 10% faster
        } else {
            1.0
        }
    }
    
    fun getEmployeeLoyaltyBonus(): Int {
        return if (appliedPerks.contains(CompanyPerk.EMPLOYEE_LOYALTY)) {
            -10 // -10% quit chance
        } else {
            0
        }
    }
}

enum class GameStatus {
    ACTIVE, COMPLETED, FAILED, PAUSED
}