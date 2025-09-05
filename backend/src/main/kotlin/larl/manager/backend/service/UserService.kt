package larl.manager.backend.service

import larl.manager.backend.entity.User
import larl.manager.backend.entity.UserRole
import larl.manager.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
) {
    // Add passwordHash before using in production
    fun createUser(username: String, email: String, passwordHash: String): User {
        if (userRepository.existsByUsername(username)) {
            throw IllegalArgumentException("Username already exists")
        }
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("Email already exists")
        }
        
        val user = User(
            username = username,
            email = email,
            passwordHash = passwordHash
        )
        
        return userRepository.save(user)
    }
    
    fun findById(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }
    
    fun findByUsername(username: String): User? {
        return userRepository.findByUsername(username).orElse(null)
    }
    
    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email).orElse(null)
    }
    
    fun updateLastLogin(userId: Long): User? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        val updatedUser = user.copy(lastLoginAt = LocalDateTime.now())
        return userRepository.save(updatedUser)
    }
}