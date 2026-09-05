package nz.co.ridling.healthproof.ui.foodlog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nz.co.ridling.healthproof.data.food.FoodLogDatabase
import nz.co.ridling.healthproof.data.food.FoodLogRepository
import nz.co.ridling.healthproof.domain.ComplianceStatus
import nz.co.ridling.healthproof.domain.MealCategory
import nz.co.ridling.healthproof.domain.MealEntry
import nz.co.ridling.healthproof.domain.calculateDailyCompliance
import nz.co.ridling.healthproof.domain.calculateDailyTotals
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FoodLogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FoodLogRepository(FoodLogDatabase.getInstance(application).foodLogDao())

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val editingEntry = MutableStateFlow<MealEntry?>(null)

    private val entriesForSelectedDate = selectedDate.flatMapLatest { date -> repository.observeEntriesForDate(date) }

    val uiState: StateFlow<FoodLogUiState> = combine(
        selectedDate,
        entriesForSelectedDate,
        editingEntry,
    ) { date, entries, editing ->
        FoodLogUiState(
            selectedDate = date,
            entries = entries,
            dailyTotals = calculateDailyTotals(entries),
            complianceStatus = calculateDailyCompliance(entries),
            editingEntry = editing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodLogUiState())

    fun goToPreviousDay() {
        selectedDate.value = selectedDate.value.minusDays(1)
    }

    fun goToNextDay() {
        selectedDate.value = selectedDate.value.plusDays(1)
    }

    fun goToToday() {
        selectedDate.value = LocalDate.now()
    }

    fun startNewEntry(category: MealCategory) {
        editingEntry.value = MealEntry(
            date = selectedDate.value,
            category = category,
            name = "",
            calories = 0,
            proteinGrams = 0.0,
            carbsGrams = 0.0,
            fatGrams = 0.0,
            compliance = ComplianceStatus.COMPLIANT,
        )
    }

    fun startEditEntry(entry: MealEntry) {
        editingEntry.value = entry
    }

    fun cancelEdit() {
        editingEntry.value = null
    }

    fun saveEntry(entry: MealEntry) {
        viewModelScope.launch {
            if (entry.id == 0L) repository.addEntry(entry) else repository.updateEntry(entry)
            editingEntry.value = null
        }
    }

    fun deleteEntry(entry: MealEntry) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FoodLogViewModel(application) as T
                }
            }
    }
}
