package nz.co.ridling.healthproof.domain

import java.time.LocalDate

enum class MealCategory(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
}

/**
 * Manually self-assessed per-entry - there is no automatic classification yet
 * (that arrives with the later Anthropic food-interpretation milestone).
 */
enum class ComplianceStatus(val displayName: String) {
    COMPLIANT("Compliant"),
    NOT_COMPLIANT("Not compliant"),
    CHEAT("Cheat meal"),
}

data class MealEntry(
    val id: Long = 0,
    val date: LocalDate,
    val category: MealCategory,
    val name: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val compliance: ComplianceStatus,
)

data class DailyTotals(
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
) {
    companion object {
        val ZERO = DailyTotals(0, 0.0, 0.0, 0.0)
    }
}

/** The day's overall slow-carb status, derived from each entry's own self-assessment. */
enum class DailyComplianceStatus(val displayName: String) {
    NO_ENTRIES("No entries logged"),
    COMPLIANT("Compliant day"),
    NOT_COMPLIANT("Not compliant"),
    CHEAT_DAY("Cheat day"),
}
