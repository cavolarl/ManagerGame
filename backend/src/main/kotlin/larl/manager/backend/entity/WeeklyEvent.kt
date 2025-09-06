package larl.manager.backend.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "weekly_events")
data class WeeklyEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    val gameSession: GameSession,
    
    @Column(nullable = false)
    val week: Int,
    
    @Column(nullable = false)
    val quarter: Int,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val eventType: EventType,
    
    @Column(nullable = false, length = 100)
    val title: String,
    
    @Column(nullable = false, length = 500)
    val description: String,
    
    @Column(nullable = false)
    val budgetImpact: Long = 0,
    
    @Column(nullable = false)
    val moraleImpact: Int = 0, // Applied to all employees
    
    @Column(nullable = false)
    val hasBeenProcessed: Boolean = false,
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val occurredAt: LocalDateTime = LocalDateTime.now()
)

enum class EventType {
    POSITIVE, NEGATIVE, NEUTRAL, STRATEGIC
}