package nz.co.ridling.healthproof.data.food

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nz.co.ridling.healthproof.domain.ComplianceStatus
import nz.co.ridling.healthproof.domain.MealCategory
import nz.co.ridling.healthproof.domain.MealEntry
import java.time.LocalDate

private fun MealEntryEntity.toDomain(): MealEntry = MealEntry(
    id = id,
    date = LocalDate.parse(date),
    category = MealCategory.valueOf(category),
    name = name,
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    compliance = ComplianceStatus.valueOf(compliance),
)

private fun MealEntry.toEntity(): MealEntryEntity = MealEntryEntity(
    id = id,
    date = date.toString(),
    category = category.name,
    name = name,
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    compliance = compliance.name,
)

/** Local-only persistence for manually logged meal entries. Never leaves the device. */
class FoodLogRepository(private val dao: FoodLogDao) {

    fun observeEntriesForDate(date: LocalDate): Flow<List<MealEntry>> =
        dao.observeByDate(date.toString()).map { entities -> entities.map { it.toDomain() } }

    suspend fun addEntry(entry: MealEntry) {
        dao.insert(entry.toEntity())
    }

    suspend fun updateEntry(entry: MealEntry) {
        dao.update(entry.toEntity())
    }

    suspend fun deleteEntry(entry: MealEntry) {
        dao.delete(entry.toEntity())
    }
}
