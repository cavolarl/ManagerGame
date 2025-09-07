package larl.manager.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.EnableTransactionManagement
import kotlin.random.Random

/**
 * Configuration for service layer dependencies and utilities
 */
@Configuration
@EnableTransactionManagement
class ServiceConfiguration {
    
    /**
     * Provides a Random instance for services that need randomization.
     * Can be replaced with a seeded random for testing.
     */
    @Bean
    fun gameRandom(): Random {
        return Random.Default
    }
}

/**
 * Game constants and configuration values
 */
object GameConstants {
    
    // Starting game values
    const val STARTING_BUDGET = 50000L
    const val WEEKS_PER_QUARTER = 13
    const val VACATION_WEEKS = 1
    
    // Contract generation
    const val INITIAL_CONTRACTS_MIN = 2
    const val INITIAL_CONTRACTS_MAX = 4
    const val QUARTERLY_CONTRACTS_MIN = 3
    const val QUARTERLY_CONTRACTS_MAX = 6
    
    // Employee generation
    const val EMPLOYEE_POOL_SIZE = 5
    const val MIN_EMPLOYEE_LEVEL = 1
    const val MAX_EMPLOYEE_LEVEL = 5
    const val STARTING_MORALE_MIN = 70
    const val STARTING_MORALE_MAX = 101
    
    // Weekly event chances
    const val WEEKLY_EVENT_CHANCE = 70 // 70% chance
    
    // Morale thresholds for quit chances
    const val EXCELLENT_MORALE_THRESHOLD = 80
    const val GOOD_MORALE_THRESHOLD = 60
    const val AVERAGE_MORALE_THRESHOLD = 40
    const val POOR_MORALE_THRESHOLD = 20
    
    // Quit chances by morale level
    const val EXCELLENT_MORALE_QUIT_CHANCE = 2
    const val GOOD_MORALE_QUIT_CHANCE = 5
    const val AVERAGE_MORALE_QUIT_CHANCE = 15
    const val POOR_MORALE_QUIT_CHANCE = 30
    const val TERRIBLE_MORALE_QUIT_CHANCE = 50
    
    // Scoring thresholds by quarter
    fun getMinimumScoreThreshold(quarter: Int): Int {
        return when (quarter) {
            1 -> 100
            2 -> 200
            3 -> 350
            4 -> 500
            else -> 500 + (quarter - 4) * 150
        }
    }
    
    // Salary ranges by employee type
    object SalaryRanges {
        val DEVELOPER = 800L to 1200L
        val DESIGNER = 700L to 1000L
        val ANALYST = 600L to 900L
        val MANAGER = 1000L to 1500L
        val MARKETING = 650L to 950L
        val SALES = 700L to 1100L
    }
    
    // Speed ranges by employee type
    object SpeedRanges {
        val DEVELOPER = 25 to 40
        val DESIGNER = 20 to 35
        val ANALYST = 15 to 30
        val MANAGER = 10 to 25
        val MARKETING = 20 to 35
        val SALES = 15 to 30
    }
    
    // Accuracy ranges by employee type
    object AccuracyRanges {
        val DEVELOPER = 60 to 80
        val DESIGNER = 65 to 85
        val ANALYST = 70 to 90
        val MANAGER = 55 to 75
        val MARKETING = 60 to 80
        val SALES = 50 to 70
    }
    
    // Contract work requirements by difficulty
    object WorkRequirements {
        val EASY = 50 to 100
        val MEDIUM = 100 to 200
        val HARD = 200 to 350
    }
    
    // Contract deadline ranges by difficulty (in weeks)
    object DeadlineRanges {
        val EASY = 2 to 4
        val MEDIUM = 3 to 6
        val HARD = 5 to 9
    }
    
    // Contract reward multipliers
    const val BONUS_REWARD_PERCENTAGE = 0.2 // 20% bonus
    const val QUARTERLY_REWARD_MULTIPLIER = 1000L // Extra per quarter
    
    // Training costs and benefits
    const val TRAINING_COST_BASE = 2000L
    const val TRAINING_SALARY_INCREASE = 1.1 // 10% increase
    const val TRAINING_SPEED_INCREASE_MIN = 5
    const val TRAINING_SPEED_INCREASE_MAX = 15
    const val TRAINING_ACCURACY_INCREASE_MIN = 2
    const val TRAINING_ACCURACY_INCREASE_MAX = 8
    
    // Hiring costs
    const val HIRING_COST_MULTIPLIER = 2 // 2 weeks salary
    
    // Event impact ranges
    object EventImpacts {
        val POSITIVE_BUDGET = 1000L to 7000L
        val NEGATIVE_BUDGET = -8000L to -500L
        val NEUTRAL_BUDGET = -2500L to 0L
        val STRATEGIC_BUDGET = -10000L to 6000L
        
        val POSITIVE_MORALE = 5 to 25
        val NEGATIVE_MORALE = -18 to -3
        val NEUTRAL_MORALE = 1 to 10
        val STRATEGIC_MORALE = 5 to 25
    }
}

/**
 * Utility functions for game calculations
 */
object GameUtils {
    
    /**
     * Calculate compound salary increase for multiple training sessions
     */
    fun calculateTrainedSalary(baseSalary: Long, trainingLevels: Int): Long {
        var salary = baseSalary
        repeat(trainingLevels) {
            salary = (salary * GameConstants.TRAINING_SALARY_INCREASE).toLong()
        }
        return salary
    }
    
    /**
     * Calculate hiring cost based on employee salary
     */
    fun calculateHiringCost(employeeSalary: Long): Long {
        return employeeSalary * GameConstants.HIRING_COST_MULTIPLIER
    }
    
    /**
     * Calculate training cost based on employee level
     */
    fun calculateTrainingCost(employeeLevel: Int): Long {
        return GameConstants.TRAINING_COST_BASE * employeeLevel
    }
    
    /**
     * Calculate contract completion percentage
     */
    fun calculateCompletionPercentage(currentProgress: Int, totalRequired: Int): Double {
        return if (totalRequired == 0) 0.0 else (currentProgress.toDouble() / totalRequired * 100).coerceAtMost(100.0)
    }
    
    /**
     * Calculate weeks until deadline warning threshold
     */
    fun isDeadlineWarning(weeksRemaining: Int, originalDeadline: Int): Boolean {
        val warningThreshold = originalDeadline / 3 // Warning when 1/3 time remaining
        return weeksRemaining <= warningThreshold && weeksRemaining > 0
    }
    
    /**
     * Calculate performance score for quarter evaluation
     */
    fun calculateQuarterScore(stakeholderValue: Int, budget: Long, errorPenalties: Int = 0): Int {
        return stakeholderValue + (budget / 1000).toInt() - errorPenalties
    }
    
    /**
     * Determine employee quit chance based on morale
     */
    fun calculateQuitChance(morale: Int): Int {
        return when {
            morale >= GameConstants.EXCELLENT_MORALE_THRESHOLD -> GameConstants.EXCELLENT_MORALE_QUIT_CHANCE
            morale >= GameConstants.GOOD_MORALE_THRESHOLD -> GameConstants.GOOD_MORALE_QUIT_CHANCE
            morale >= GameConstants.AVERAGE_MORALE_THRESHOLD -> GameConstants.AVERAGE_MORALE_QUIT_CHANCE
            morale >= GameConstants.POOR_MORALE_THRESHOLD -> GameConstants.POOR_MORALE_QUIT_CHANCE
            else -> GameConstants.TERRIBLE_MORALE_QUIT_CHANCE
        }
    }
    
    /**
     * Generate a weighted random selection from a list
     */
    fun <T> weightedRandomSelection(items: List<T>, weights: List<Int>): T {
        require(items.size == weights.size) { "Items and weights must have the same size" }
        require(weights.all { it > 0 }) { "All weights must be positive" }
        
        val totalWeight = weights.sum()
        var randomValue = Random.nextInt(1, totalWeight + 1)
        
        for (i in items.indices) {
            randomValue -= weights[i]
            if (randomValue <= 0) {
                return items[i]
            }
        }
        
        return items.last() // Fallback
    }
    
    /**
     * Clamp a value between min and max
     */
    fun clamp(value: Int, min: Int, max: Int): Int {
        return value.coerceIn(min, max)
    }
    
    /**
     * Clamp a value between min and max for Long
     */
    fun clamp(value: Long, min: Long, max: Long): Long {
        return value.coerceIn(min, max)
    }
    
    /**
     * Format currency for display
     */
    fun formatCurrency(amount: Long): String {
        return "${String.format("%,d", amount)}"
    }
    
    /**
     * Format percentage for display
     */
    fun formatPercentage(percentage: Double): String {
        return "${String.format("%.1f", percentage)}%"
    }
    
    /**
     * Calculate the bonus eligibility for contract completion
     */
    fun isBonusEligible(weeksRemaining: Int): Boolean {
        return weeksRemaining > 0
    }
    
    /**
     * Calculate dynamic event frequency based on quarter
     */
    fun getEventFrequency(quarter: Int): Int {
        return GameConstants.WEEKLY_EVENT_CHANCE + (quarter * 5) // Slightly more events in later quarters
    }
}