package larl.manager.backend.dto.response

import larl.manager.backend.entity.User
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val createdAt: LocalDateTime,
    val role: String,
    val isActive: Boolean
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            createdAt = user.createdAt,
            role = user.role.name,
            isActive = user.isActive
        )
    }
}