package nz.co.ridling.healthproof.data.food

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stored as plain strings for date/category/compliance (ISO-8601 date, enum name) rather than
 * via Room TypeConverters - simplest thing that works for one table with no queries that need
 * to compare dates as anything other than exact-match/range on the ISO string, which sorts and
 * compares correctly as text.
 */
@Entity(tableName = "meal_entries")
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val category: String,
    val name: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val compliance: String,
)
