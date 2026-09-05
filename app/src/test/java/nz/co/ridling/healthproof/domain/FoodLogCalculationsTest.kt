package nz.co.ridling.healthproof.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FoodLogCalculationsTest {

    private val today = LocalDate.of(2026, 9, 5)

    private fun entry(
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        compliance: ComplianceStatus,
        category: MealCategory = MealCategory.LUNCH,
    ) = MealEntry(date = today, category = category, name = "test", calories = calories, proteinGrams = protein, carbsGrams = carbs, fatGrams = fat, compliance = compliance)

    @Test
    fun `daily totals of no entries is zero`() {
        val totals = calculateDailyTotals(emptyList())
        assertEquals(DailyTotals.ZERO, totals)
    }

    @Test
    fun `daily totals sum across entries`() {
        val entries = listOf(
            entry(400, 30.0, 40.0, 10.0, ComplianceStatus.COMPLIANT),
            entry(600, 45.0, 20.0, 25.0, ComplianceStatus.COMPLIANT),
        )
        val totals = calculateDailyTotals(entries)
        assertEquals(1000, totals.calories)
        assertEquals(75.0, totals.proteinGrams, 0.0001)
        assertEquals(60.0, totals.carbsGrams, 0.0001)
        assertEquals(35.0, totals.fatGrams, 0.0001)
    }

    @Test
    fun `compliance with no entries is NO_ENTRIES`() {
        assertEquals(DailyComplianceStatus.NO_ENTRIES, calculateDailyCompliance(emptyList()))
    }

    @Test
    fun `compliance is COMPLIANT when all entries compliant`() {
        val entries = listOf(
            entry(400, 30.0, 40.0, 10.0, ComplianceStatus.COMPLIANT),
            entry(600, 45.0, 20.0, 25.0, ComplianceStatus.COMPLIANT),
        )
        assertEquals(DailyComplianceStatus.COMPLIANT, calculateDailyCompliance(entries))
    }

    @Test
    fun `compliance is NOT_COMPLIANT when any entry is not compliant`() {
        val entries = listOf(
            entry(400, 30.0, 40.0, 10.0, ComplianceStatus.COMPLIANT),
            entry(600, 45.0, 20.0, 25.0, ComplianceStatus.NOT_COMPLIANT),
        )
        assertEquals(DailyComplianceStatus.NOT_COMPLIANT, calculateDailyCompliance(entries))
    }

    @Test
    fun `a single cheat entry makes the whole day a cheat day even with a non-compliant entry`() {
        val entries = listOf(
            entry(400, 30.0, 40.0, 10.0, ComplianceStatus.NOT_COMPLIANT),
            entry(600, 45.0, 20.0, 25.0, ComplianceStatus.CHEAT),
        )
        assertEquals(DailyComplianceStatus.CHEAT_DAY, calculateDailyCompliance(entries))
    }
}
