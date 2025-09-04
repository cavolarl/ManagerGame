package larl.manager.backend.service

import larl.manager.backend.entity.User
import larl.manager.backend.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        return userRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("User not found: $username") }
    }

    fun createUser(username: String, password: String, email: String): User {
        if (userRepository.existsByUsername(username)) {
            throw IllegalArgumentException("Username '$username' already exists")
        }
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("Email '$email' already exists")
        }

        val user = User(
            username = username,
            password = passwordEncoder.encode(password),
            email = email
        )
        
        return userRepository.save(user)
    }

    fun findByUsername(username: String): User? {
        return userRepository.findByUsername(username).orElse(null)
    }
}