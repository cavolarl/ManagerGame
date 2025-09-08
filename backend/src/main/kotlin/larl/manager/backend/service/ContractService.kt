package larl.manager.backend.service

import larl.manager.backend.entity.*
import larl.manager.backend.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random
import kotlin.contracts.contract

@Service
@Transactional
class ContractService(
    private val contractRepository: ContractRepository,
    private val contractAssignmentRepository: ContractAssignmentRepository,
    private val gameEmployeeRepository: GameEmployeeRepository
) {
    
    fun generateInitialContracts(gameSession: GameSession): List<Contract> {
        val contracts = mutableListOf<Contract>()
        
        // Generate 2-3 starting contracts as per game design
        repeat(Random.nextInt(2, 4)) {
            val contract = generateRandomContract(gameSession, 1) // Quarter 1
            contracts.add(contractRepository.save(contract))
        }
        
        return contracts
    }
    
    fun generateQuarterlyContracts(gameSession: GameSession): List<Contract> {
        val contracts = mutableListOf<Contract>()
        val contractCount = Random.nextInt(3, 6) // More contracts in later quarters
        
        repeat(contractCount) {
            val contract = generateRandomContract(gameSession, gameSession.currentQuarter)
            contracts.add(contractRepository.save(contract))
        }
        
        return contracts
    }
    
    private fun generateRandomContract(gameSession: GameSession, quarter: Int): Contract {
        val difficulty = getRandomDifficulty()
        val baseReward = getBaseReward(difficulty, quarter)
        val deadline = getRandomDeadline(difficulty)
        
        return Contract(
            gameSession = gameSession,
            title = generateContractTitle(difficulty),
            description = generateContractDescription(difficulty),
            difficulty = difficulty,
            totalWorkRequired = getTotalWorkRequired(difficulty),
            currentProgress = 0,
            baseReward = baseReward,
            bonusMultiplier = 1.5,
            stakeholderPoints = getStakeholderValue(difficulty),
            deadlineWeeks = deadline,
            weeksRemaining = deadline,
            status = ContractStatus.AVAILABLE
        )
    }
    
    fun findAvailableContracts(gameSessionId: Long): List<Contract> {
        return contractRepository.findByGameSessionIdAndStatus(gameSessionId, ContractStatus.AVAILABLE)
    }
    
    fun findActiveContracts(gameSessionId: Long): List<Contract> {
        return contractRepository.findActiveContractsByGameSession(gameSessionId)
    }

    fun findById(contractId: Long): Contract? {
        return contractRepository.findById(contractId).orElse(null)
    }
    
    fun startContract(contractId: Long): Contract? {
        val contract = contractRepository.findById(contractId).orElse(null) ?: return null
        
        if (contract.status != ContractStatus.AVAILABLE) {
            throw IllegalArgumentException("Contract is not available to start")
        }
        
        val updatedContract = contract.copy(status = ContractStatus.IN_PROGRESS)
        return contractRepository.save(updatedContract)
    }
    
    fun assignEmployeeToContract(contractId: Long, employeeId: Long, week: Int): ContractAssignment? {
        val contract = contractRepository.findById(contractId).orElse(null) ?: return null
        val employee = gameEmployeeRepository.findById(employeeId).orElse(null) ?: return null
        
        if (contract.status != ContractStatus.IN_PROGRESS) {
            throw IllegalArgumentException("Contract must be in progress to assign employees")
        }
        
        // Check if employee is already assigned to this contract this week
        val existingAssignment = contractAssignmentRepository
            .findByContractIdAndEmployeeIdAndWeekAssigned(contractId, employeeId, week)
        
        if (existingAssignment.isPresent) {
            throw IllegalArgumentException("Employee already assigned to this contract this week")
        }
        
        val assignment = ContractAssignment(
            contract = contract,
            employee = employee,
            weekAssigned = week,
            isActive = true
        )
        
        return contractAssignmentRepository.save(assignment)
    }
    
    fun processWeeklyProgress(contractId: Long, week: Int): Contract? {
        val contract = contractRepository.findById(contractId).orElse(null) ?: return null
        
        if (contract.status != ContractStatus.IN_PROGRESS) {
            return contract
        }
        
        // Get all active assignments for this contract this week
        val assignments = contractAssignmentRepository
            .findByContractIdAndWeekAssignedAndIsActive(contractId, week, true)
        
        // Calculate total work points from assigned employees
        val totalWorkPoints = assignments.sumOf { assignment ->
            assignment.employee.speed
        }
        
        // Update contract progress
        val newProgress = (contract.currentProgress + totalWorkPoints).coerceAtMost(contract.totalWorkRequired)
        val updatedContract = contract.copy(
            currentProgress = newProgress,
            weeksRemaining = contract.weeksRemaining - 1
        )
        
        val savedContract = contractRepository.save(updatedContract)
        
        // Check if contract is completed
        if (newProgress >= contract.totalWorkRequired) {
            return completeContract(savedContract)
        }
        
        // Check if contract is overdue
        if (savedContract.weeksRemaining <= 0 && savedContract.currentProgress < savedContract.totalWorkRequired) {
            return failContract(savedContract)
        }
        
        return savedContract
    }
    
    private fun completeContract(contract: Contract): Contract {
        val completedContract = contract.copy(status = ContractStatus.COMPLETED)
        val savedContract = contractRepository.save(completedContract)

        /* TODO: Handle balance and rewards in the gamesession
        val reward = savedContract.getEffectiveReward()
        val gameSession = savedContract.gameSession
        */
        return savedContract
    }
    
    private fun failContract(contract: Contract): Contract {
        val failedContract = contract.copy(
            status = ContractStatus.FAILED,
        )
        
        return contractRepository.save(failedContract)
    }
    
    fun getContractsByGameSession(gameSessionId: Long): List<Contract> {
        return contractRepository.findByGameSessionIdOrderByDeadlineWeeksAsc(gameSessionId)
    }
    
    private fun getRandomDifficulty(): ContractDifficulty {
        return when (Random.nextInt(1, 101)) {
            in 1..50 -> ContractDifficulty.EASY
            in 51..80 -> ContractDifficulty.MEDIUM
            else -> ContractDifficulty.HARD
        }
    }
    
    private fun getBaseReward(difficulty: ContractDifficulty, quarter: Int): Long {
        val base = when (difficulty) {
            ContractDifficulty.EASY -> 5000L
            ContractDifficulty.MEDIUM -> 10000L
            ContractDifficulty.HARD -> 18000L
        }
        return base + (quarter * 1000L) // Increase rewards with quarters
    }
    
    private fun getTotalWorkRequired(difficulty: ContractDifficulty): Int {
        return when (difficulty) {
            ContractDifficulty.EASY -> Random.nextInt(50, 100)
            ContractDifficulty.MEDIUM -> Random.nextInt(100, 200)
            ContractDifficulty.HARD -> Random.nextInt(200, 350)
        }
    }
    
    private fun getStakeholderValue(difficulty: ContractDifficulty): Int {
        return when (difficulty) {
            ContractDifficulty.EASY -> Random.nextInt(10, 25)
            ContractDifficulty.MEDIUM -> Random.nextInt(25, 50)
            ContractDifficulty.HARD -> Random.nextInt(50, 100)
        }
    }
    
    private fun getRandomDeadline(difficulty: ContractDifficulty): Int {
        return when (difficulty) {
            ContractDifficulty.EASY -> Random.nextInt(2, 4) // 2-3 weeks
            ContractDifficulty.MEDIUM -> Random.nextInt(3, 6) // 3-5 weeks
            ContractDifficulty.HARD -> Random.nextInt(5, 9) // 5-8 weeks
        }
    }
    
    private fun generateContractTitle(difficulty: ContractDifficulty): String {
        val titles = when (difficulty) {
            ContractDifficulty.EASY -> listOf(
                "Simple Data Entry Project",
                "Basic Website Update",
                "Customer Survey Analysis",
                "Social Media Content Creation"
            )
            ContractDifficulty.MEDIUM -> listOf(
                "E-commerce Platform Development",
                "Marketing Campaign Strategy",
                "Database Migration Project",
                "Mobile App Prototype"
            )
            ContractDifficulty.HARD -> listOf(
                "Enterprise Software Solution",
                "AI Implementation Project",
                "Complete System Overhaul",
                "International Market Expansion"
            )
        }
        return titles.random()
    }
    
    private fun generateContractDescription(difficulty: ContractDifficulty): String {
        return when (difficulty) {
            ContractDifficulty.EASY -> "A straightforward project that requires basic skills and minimal complexity."
            ContractDifficulty.MEDIUM -> "A moderately complex project requiring coordination and specialized knowledge."
            ContractDifficulty.HARD -> "A challenging project demanding expertise, innovation, and careful execution."
        }
    }
}