package larl.manager.backend.controller

import larl.manager.backend.service.UserService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
class AuthController(private val userService: UserService) {

    @GetMapping("/")
    fun home(): String {
        return "home"
    }

    @GetMapping("/home")
    fun homeAlias(): String {
        return "home"
    }

    @GetMapping("/login")
    fun login(
        @RequestParam(value = "error", required = false) error: String?,
        @RequestParam(value = "logout", required = false) logout: String?,
        model: Model
    ): String {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password")
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully")
        }
        return "login"
    }

    @GetMapping("/register")
    fun register(model: Model): String {
        return "register"
    }

    @PostMapping("/register")
    fun registerUser(
        @RequestParam username: String,
        @RequestParam password: String,
        @RequestParam email: String,
        redirectAttributes: RedirectAttributes
    ): String {
        return try {
            userService.createUser(username, password, email)
            redirectAttributes.addFlashAttribute("message", "Registration successful! You can now log in.")
            "redirect:/login"
        } catch (e: IllegalArgumentException) {
            redirectAttributes.addFlashAttribute("error", e.message)
            "redirect:/register"
        }
    }

    @GetMapping("/hello")
    fun hello(): String {
        return "hello"
    }
}