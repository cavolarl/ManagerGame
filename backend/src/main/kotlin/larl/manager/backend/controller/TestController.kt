package larl.manager.backend.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TestController {

    @GetMapping("/health")
    fun health(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "message" to "Backend is running!",
            "timestamp" to System.currentTimeMillis().toString()
        )
    }

    @GetMapping("/game/info")
    fun gameInfo(): Map<String, Any> {
        return mapOf(
            "name" to "Manager Game",
            "version" to "0.1.0",
            "description" to "A cool manager game backend"
        )
    }
}