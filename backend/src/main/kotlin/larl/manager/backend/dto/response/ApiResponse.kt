package larl.manager.backend.dto.response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errors: List<String>? = null
) {
    companion object {
        fun <T> success(data: T, message: String? = null) = ApiResponse(true, data, message)
        fun <T> error(message: String, errors: List<String>? = null) = 
            ApiResponse<T>(false, null, message, errors)
    }
}