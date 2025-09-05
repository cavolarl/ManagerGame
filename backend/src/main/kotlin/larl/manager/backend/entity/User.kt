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
    
    // One-to-one relationship with Company
    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val company: Company? = null
) {
    // JPA requires no-arg constructor
    constructor() : this(
        username = "",
        email = "",
        passwordHash = ""
    )
}

enum class UserRole {
    PLAYER, ADMIN, MODERATOR
}