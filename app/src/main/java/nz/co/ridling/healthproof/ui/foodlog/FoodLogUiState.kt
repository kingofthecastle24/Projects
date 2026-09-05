package nz.co.ridling.healthproof.ui.foodlog

import nz.co.ridling.healthproof.domain.DailyComplianceStatus
import nz.co.ridling.healthproof.domain.DailyTotals
import nz.co.ridling.healthproof.domain.MealEntry
import java.time.LocalDate

data class FoodLogUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val entries: List<MealEntry> = emptyList(),
    val dailyTotals: DailyTotals = DailyTotals.ZERO,
    val complianceStatus: DailyComplianceStatus = DailyComplianceStatus.NO_ENTRIES,
    /** Non-null while the add/edit form is open; an entry with id == 0 means "new". */
    val editingEntry: MealEntry? = null,
)
