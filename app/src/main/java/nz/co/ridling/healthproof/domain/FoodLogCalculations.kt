package nz.co.ridling.healthproof.domain

/** Pure, testable daily aggregation logic - no Room/Android dependency. */

fun calculateDailyTotals(entries: List<MealEntry>): DailyTotals =
    entries.fold(DailyTotals.ZERO) { acc, entry ->
        DailyTotals(
            calories = acc.calories + entry.calories,
            proteinGrams = acc.proteinGrams + entry.proteinGrams,
            carbsGrams = acc.carbsGrams + entry.carbsGrams,
            fatGrams = acc.fatGrams + entry.fatGrams,
        )
    }

/**
 * A single cheat-meal entry makes the whole day a "cheat day"; otherwise a single
 * not-compliant entry makes the day "not compliant"; otherwise, if there's at least
 * one entry and all are compliant, the day is "compliant".
 */
fun calculateDailyCompliance(entries: List<MealEntry>): DailyComplianceStatus {
    if (entries.isEmpty()) return DailyComplianceStatus.NO_ENTRIES
    if (entries.any { it.compliance == ComplianceStatus.CHEAT }) return DailyComplianceStatus.CHEAT_DAY
    if (entries.any { it.compliance == ComplianceStatus.NOT_COMPLIANT }) return DailyComplianceStatus.NOT_COMPLIANT
    return DailyComplianceStatus.COMPLIANT
}
