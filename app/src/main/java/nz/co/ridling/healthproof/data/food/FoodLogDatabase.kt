package nz.co.ridling.healthproof.data.food

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MealEntryEntity::class], version = 1, exportSchema = false)
abstract class FoodLogDatabase : RoomDatabase() {

    abstract fun foodLogDao(): FoodLogDao

    companion object {
        @Volatile
        private var instance: FoodLogDatabase? = null

        fun getInstance(context: Context): FoodLogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FoodLogDatabase::class.java,
                    "food_log.db",
                ).build().also { instance = it }
            }
    }
}
