package larl.manager.backend.exception

sealed class GameException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class UserAlreadyExistsException(username: String) : GameException("User with username '$username' already exists")
    class UserNotFoundException(id: Long) : GameException("User with id $id not found")
    class CompanyNotFoundException(id: Long) : GameException("Company with id $id not found")
    class InsufficientFundsException(required: Long, available: Long) : 
        GameException("Insufficient funds. Required: $required, Available: $available")
    class EmployeeNotAvailableException(employeeId: Long) : 
        GameException("Employee with id $employeeId is not available for hiring")
    class ReportNotInProgressException(reportId: Long) : 
        GameException("Report with id $reportId is not in progress")
}