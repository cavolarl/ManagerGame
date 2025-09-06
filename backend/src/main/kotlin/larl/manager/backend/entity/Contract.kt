package larl.manager.backend.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "contracts")
data class Contract(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    val gameSession: GameSession,
    
    @Column(nullable = false, length = 100)
    val title: String,
    
    @Column(length = 500)
    val description: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val difficulty: ContractDifficulty,
    
    @Column(nullable = false)
    val totalWorkRequired: Int, // Total work points needed
    
    @Column(nullable = false)
    val currentProgress: Int = 0,
    
    @Column(nullable = false)
    val deadlineWeeks: Int, // How many weeks to complete
    
    @Column(nullable = false)
    val weeksRemaining: Int, // Countdown
    
    @Column(nullable = false)
    val baseReward: Long, // Money reward
    
    @Column(nullable = false)
    val stakeholderPoints: Int, // Points toward quarterly score
    
    @Column(nullable = false)
    val bonusMultiplier: Double = 1.0, // For early completion
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ContractStatus = ContractStatus.AVAILABLE,
    
    @Column(nullable = false)
    val accuracyRequirement: Int = 70, // Minimum accuracy needed (percentage)
    
    @Column(nullable = false)
    val currentAccuracy: Int = 100, // Current quality level
    
    @OneToMany(mappedBy = "contract", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val assignments: MutableSet<ContractAssignment> = mutableSetOf()
) {
    fun isComplete(): Boolean = currentProgress >= totalWorkRequired
    fun isOverdue(): Boolean = weeksRemaining <= 0 && status == ContractStatus.IN_PROGRESS
    fun getCompletionPercentage(): Double = (currentProgress.toDouble() / totalWorkRequired) * 100
    fun getEffectiveReward(): Long = if (currentAccuracy >= accuracyRequirement) 
        (baseReward * bonusMultiplier).toLong() else baseReward / 2
}

enum class ContractDifficulty {
    EASY, MEDIUM, HARD
}

enum class ContractStatus {
    AVAILABLE, IN_PROGRESS, COMPLETED, FAILED, OVERDUE
}