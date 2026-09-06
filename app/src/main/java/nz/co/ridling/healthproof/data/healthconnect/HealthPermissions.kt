package nz.co.ridling.healthproof.data.healthconnect

import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord

/**
 * Every permission this app can ever request. All read-only - Milestone 1 never
 * requests HealthPermission.getWritePermission() for any record type.
 */
object HealthPermissions {

    /** Permission string to a short, human-readable record type name, in a fixed display order. */
    private val NAMES_BY_PERMISSION: LinkedHashMap<String, String> = linkedMapOf(
        HealthPermission.getReadPermission(WeightRecord::class) to "Weight",
        HealthPermission.getReadPermission(BodyFatRecord::class) to "Body fat",
        HealthPermission.getReadPermission(ExerciseSessionRecord::class) to "Exercise sessions",
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) to "Active calories burned",
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) to "Total calories burned",
        HealthPermission.getReadPermission(DistanceRecord::class) to "Distance",
        HealthPermission.getReadPermission(HeartRateRecord::class) to "Heart rate",
        HealthPermission.getReadPermission(SpeedRecord::class) to "Speed",
        HealthPermission.getReadPermission(ElevationGainedRecord::class) to "Elevation gained",
        HealthPermission.getReadPermission(StepsRecord::class) to "Steps",
        HealthPermission.getReadPermission(StepsCadenceRecord::class) to "Step cadence",
    )

    val ALL: Set<String> = NAMES_BY_PERMISSION.keys

    /** Human-readable names, in display order, for the given permission strings. */
    fun namesFor(permissions: Collection<String>): List<String> =
        NAMES_BY_PERMISSION.filterKeys { it in permissions }.values.toList()

    fun requestPermissionsContract() = PermissionController.createRequestPermissionResultContract()
}
