package nz.co.ridling.healthproof.data.healthconnect

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin as HcDataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import nz.co.ridling.healthproof.domain.DataOrigin
import nz.co.ridling.healthproof.domain.WeightSample
import nz.co.ridling.healthproof.domain.WeightSummary
import nz.co.ridling.healthproof.domain.WorkoutSession
import nz.co.ridling.healthproof.domain.summarizeWeightSamples
import java.time.Instant
import kotlin.reflect.KClass

private const val TAG = "HealthConnectManager"

/**
 * Thin wrapper around [HealthConnectClient]. Every function here is read-only:
 * no write*, insert*, update* or delete* call is ever made against Health Connect.
 */
class HealthConnectManager(private val context: Context) {

    fun availability(): HealthConnectAvailability {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.RequiresUpdate
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectAvailability.NotInstalled
            else -> HealthConnectAvailability.Unavailable
        }
    }

    private val client: HealthConnectClient? by lazy {
        if (availability() == HealthConnectAvailability.Available) {
            runCatching { HealthConnectClient.getOrCreate(context) }
                .onFailure { Log.w(TAG, "Health Connect client unavailable: ${it.javaClass.simpleName}") }
                .getOrNull()
        } else {
            null
        }
    }

    suspend fun grantedPermissions(): Set<String> {
        val c = client ?: return emptySet()
        return runCatching { c.permissionController.getGrantedPermissions() }
            .onFailure { Log.w(TAG, "Failed to read granted permissions: ${it.javaClass.simpleName}") }
            .getOrDefault(emptySet())
    }

    private fun appLabelFor(packageName: String): String {
        return runCatching {
            val pm = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    /**
     * Reads every record type the app currently has permission for, over [range], purely to
     * discover which apps (dataOrigin.packageName) are contributing data and how much. This is
     * how the Garmin Connect package name gets confirmed on a real device (requirement: do not
     * hard-code it up front).
     */
    suspend fun discoverDataOrigins(range: TimeRangeFilter, granted: Set<String>): List<DataOrigin> {
        val c = client ?: return emptyList()
        val counts = linkedMapOf<String, Int>()

        suspend fun <T : Record> tally(type: KClass<T>, permission: String) {
            if (permission !in granted) return
            val records = readRecordsSafely(c, type, range)
            for (record in records) {
                val pkg = record.metadata.dataOrigin.packageName
                counts[pkg] = (counts[pkg] ?: 0) + 1
            }
        }

        tally(WeightRecord::class, permissionFor<WeightRecord>())
        tally(BodyFatRecord::class, permissionFor<BodyFatRecord>())
        tally(ExerciseSessionRecord::class, permissionFor<ExerciseSessionRecord>())
        tally(ActiveCaloriesBurnedRecord::class, permissionFor<ActiveCaloriesBurnedRecord>())
        tally(TotalCaloriesBurnedRecord::class, permissionFor<TotalCaloriesBurnedRecord>())
        tally(DistanceRecord::class, permissionFor<DistanceRecord>())
        tally(HeartRateRecord::class, permissionFor<HeartRateRecord>())
        tally(SpeedRecord::class, permissionFor<SpeedRecord>())
        tally(ElevationGainedRecord::class, permissionFor<ElevationGainedRecord>())
        tally(StepsRecord::class, permissionFor<StepsRecord>())
        tally(StepsCadenceRecord::class, permissionFor<StepsCadenceRecord>())

        return counts.map { (pkg, count) -> DataOrigin(pkg, appLabelFor(pkg), count) }
            .sortedByDescending { it.recordCount }
    }

    suspend fun readWeightSummary(
        range: TimeRangeFilter,
        granted: Set<String>,
        preferredSourcePackageName: String?,
    ): WeightSummary {
        val c = client ?: return WeightSummary.EMPTY
        if (permissionFor<WeightRecord>() !in granted) return WeightSummary.EMPTY

        val records = readRecordsSafely(c, WeightRecord::class, range)
            .sortedByDescending { it.time }
            .let { all ->
                if (preferredSourcePackageName != null) {
                    all.filter { it.metadata.dataOrigin.packageName == preferredSourcePackageName }
                } else {
                    all
                }
            }

        val samples = records.map {
            WeightSample(
                kilograms = it.weight.inKilograms,
                time = it.time,
                sourcePackageName = it.metadata.dataOrigin.packageName,
                sourceAppLabel = appLabelFor(it.metadata.dataOrigin.packageName),
            )
        }

        return summarizeWeightSamples(samples)
    }

    suspend fun readWorkouts(
        range: TimeRangeFilter,
        granted: Set<String>,
        preferredSourcePackageName: String?,
        limit: Int = 10,
    ): List<WorkoutSession> {
        val c = client ?: return emptyList()
        if (permissionFor<ExerciseSessionRecord>() !in granted) return emptyList()

        val sessions = readRecordsSafely(c, ExerciseSessionRecord::class, range)
            .sortedByDescending { it.startTime }
            .let { all ->
                if (preferredSourcePackageName != null) {
                    all.filter { it.metadata.dataOrigin.packageName == preferredSourcePackageName }
                } else {
                    all
                }
            }
            .take(limit)

        return sessions.map { session -> session.toWorkoutSession(c, granted) }
    }

    private suspend fun ExerciseSessionRecord.toWorkoutSession(
        c: HealthConnectClient,
        granted: Set<String>,
    ): WorkoutSession {
        val sessionRange = TimeRangeFilter.between(startTime, endTime)
        // Scope aggregation to this session's own data origin so a duplicate recording of the
        // same workout by a second app is never summed into these totals.
        val originFilter = setOf(HcDataOrigin(metadata.dataOrigin.packageName))

        val metrics = buildSet {
            if (permissionFor<ActiveCaloriesBurnedRecord>() in granted) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            if (permissionFor<DistanceRecord>() in granted) add(DistanceRecord.DISTANCE_TOTAL)
            if (permissionFor<HeartRateRecord>() in granted) {
                add(HeartRateRecord.BPM_AVG)
                add(HeartRateRecord.BPM_MAX)
            }
            if (permissionFor<SpeedRecord>() in granted) add(SpeedRecord.SPEED_AVG)
            if (permissionFor<ElevationGainedRecord>() in granted) add(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)
        }

        val aggregation: AggregationResult? = if (metrics.isEmpty()) {
            null
        } else {
            runCatching {
                c.aggregate(AggregateRequest(metrics = metrics, timeRangeFilter = sessionRange, dataOriginFilter = originFilter))
            }.onFailure { Log.w(TAG, "Aggregation failed for a session: ${it.javaClass.simpleName}") }
                .getOrNull()
        }

        return WorkoutSession(
            exerciseType = ExerciseTypeNames.label(exerciseType),
            title = title,
            startTime = startTime,
            endTime = endTime,
            distanceMeters = aggregation?.get(DistanceRecord.DISTANCE_TOTAL)?.inMeters,
            activeCaloriesKcal = aggregation?.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)?.inKilocalories,
            averageHeartRateBpm = aggregation?.get(HeartRateRecord.BPM_AVG),
            maximumHeartRateBpm = aggregation?.get(HeartRateRecord.BPM_MAX),
            averageSpeedMetersPerSecond = aggregation?.get(SpeedRecord.SPEED_AVG)?.inMetersPerSecond,
            elevationGainedMeters = aggregation?.get(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)?.inMeters,
            sourcePackageName = metadata.dataOrigin.packageName,
            sourceAppLabel = appLabelFor(metadata.dataOrigin.packageName),
        )
    }

    private suspend fun <T : Record> readRecordsSafely(
        client: HealthConnectClient,
        type: KClass<T>,
        range: TimeRangeFilter,
    ): List<T> {
        return runCatching {
            client.readRecords(ReadRecordsRequest(recordType = type, timeRangeFilter = range)).records
        }.onFailure { Log.w(TAG, "Read failed for ${type.simpleName}: ${it.javaClass.simpleName}") }
            .getOrDefault(emptyList())
    }

    private inline fun <reified T : Record> permissionFor(): String =
        androidx.health.connect.client.permission.HealthPermission.getReadPermission(T::class)

    companion object {
        fun thirtyDayWindow(now: Instant = Instant.now()): TimeRangeFilter =
            TimeRangeFilter.between(now.minusSeconds(30L * 24 * 60 * 60), now)
    }
}
