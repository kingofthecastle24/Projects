package nz.co.ridling.healthproof.data.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord

/** Human-readable labels for the ExerciseSessionRecord.exerciseType int constants we're likely to see from Garmin. */
object ExerciseTypeNames {

    private val names: Map<Int, String> = mapOf(
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to "Running",
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to "Treadmill running",
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING to "Walking",
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING to "Hiking",
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING to "Cycling",
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY to "Stationary cycling",
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER to "Open water swimming",
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL to "Pool swimming",
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING to "Strength training",
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING to "Rowing",
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE to "Rowing machine",
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL to "Elliptical",
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA to "Yoga",
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES to "Pilates",
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING to "Stair climbing",
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE to "Stair climbing machine",
        ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT to "Other workout",
    )

    fun label(exerciseType: Int): String = names[exerciseType] ?: "Other (type $exerciseType)"
}
