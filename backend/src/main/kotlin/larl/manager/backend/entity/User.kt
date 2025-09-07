package larl.manager.backend.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_username", columnList = "username"),
        Index(name = "idx_email", columnList = "email")
    ]
)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(unique = true, nullable = false, length = 50)
    @field:NotBlank
    @field:Size(min = 3, max = 50)
    val username: String,
    
    @Column(unique = true, nullable = false, length = 100)
    @field:NotBlank
    @field:Email
    val email: String,
    
    @Column(nullable = false, length = 255)
    @field:NotBlank
    val passwordHash: String,
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @UpdateTimestamp
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val lastLoginAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: UserRole = UserRole.PLAYER,
    
    // Meta-progression fields
    @Column(nullable = false)
    val totalRuns: Int = 0,
    
    @Column(nullable = false)
    val bestScore: Int = 0,
    
    @Column(nullable = false)
    val totalQuartersCompleted: Int = 0,
    
    @Column(nullable = false)
    val totalCompaniesCreated: Int = 0,
    
    // Unlocks for meta-progression
    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_unlocked_employee_types", joinColumns = [JoinColumn(name = "user_id")])
    @Column(name = "employee_type")
    val unlockedEmployeeTypes: MutableSet<EmployeeType> = mutableSetOf(
        EmployeeType.ANALYST
    ),
    
    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_unlocked_perks", joinColumns = [JoinColumn(name = "user_id")])
    @Column(name = "perk")
    val unlockedPerks: MutableSet<CompanyPerk> = mutableSetOf(),
    
    // One-to-many relationship with GameSessions (all runs)
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val gameSessions: MutableSet<GameSession> = mutableSetOf()
) {
    // JPA requires no-arg constructor
    constructor() : this(
        username = "",
        email = "",
        passwordHash = ""
    )
    
    // Helper methods for meta-progression
    fun canUnlockPerk(perk: CompanyPerk): Boolean {
        return when (perk) {
            CompanyPerk.BETTER_ONBOARDING -> totalRuns >= 3
            CompanyPerk.CHEAPER_TRAINING -> totalQuartersCompleted >= 5
            CompanyPerk.MORALE_BONUS -> bestScore >= 500
            CompanyPerk.FASTER_HIRING -> totalCompaniesCreated >= 10
            CompanyPerk.BUDGET_BOOST -> totalRuns >= 1
            CompanyPerk.EMPLOYEE_LOYALTY -> totalQuartersCompleted >= 10
            CompanyPerk.CONTRACT_NEGOTIATOR -> bestScore >= 1000
            CompanyPerk.EFFICIENCY_EXPERT -> totalQuartersCompleted >= 20
        }
    }
    
    fun unlockPerk(perk: CompanyPerk): User {
        if (canUnlockPerk(perk) && !unlockedPerks.contains(perk)) {
            return this.copy(unlockedPerks = (unlockedPerks + perk).toMutableSet())
        }
        return this
    }
    
    fun updateStats(gameSession: GameSession): User {
        val newTotalRuns = totalRuns + 1
        val newBestScore = maxOf(bestScore, gameSession.getTotalScore())
        val newQuartersCompleted = totalQuartersCompleted + gameSession.currentQuarter
        val newCompaniesCreated = totalCompaniesCreated + 1
        
        return this.copy(
            totalRuns = newTotalRuns,
            bestScore = newBestScore,
            totalQuartersCompleted = newQuartersCompleted,
            totalCompaniesCreated = newCompaniesCreated
        )
    }
}

// Meta-progression enums
enum class CompanyPerk {
    BETTER_ONBOARDING,     // +10 starting morale for all employees
    CHEAPER_TRAINING,      // 25% off training costs
    MORALE_BONUS,         // +5 weekly morale for all employees
    FASTER_HIRING,        // Reduced hiring costs
    BUDGET_BOOST,         // Start with extra money
    EMPLOYEE_LOYALTY,     // Reduced quit chances
    CONTRACT_NEGOTIATOR,  // Better contract rewards
    EFFICIENCY_EXPERT     // Employees work 10% faster
}

enum class UserRole {
    PLAYER, ADMIN, MODERATOR
}