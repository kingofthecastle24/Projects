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
import nz.co.ridling.healthproof.domain.ReadIssue
import nz.co.ridling.healthproof.domain.WeightSample
import nz.co.ridling.healthproof.domain.WeightSummary
import nz.co.ridling.healthproof.domain.WorkoutSession
import nz.co.ridling.healthproof.domain.summarizeWeightSamples
import java.time.Instant
import kotlin.reflect.KClass

private const val TAG = "HealthConnectManager"

/** Everything read from Health Connect in one pass, plus any per-type read failures. */
data class HealthConnectSnapshot(
    val dataOrigins: List<DataOrigin>,
    val weight: WeightSummary,
    val workouts: List<WorkoutSession>,
    val issues: List<ReadIssue>,
)

/** Raw records for each type, read at most once per refresh. Exception class names only in [issues] - never health data. */
private data class RecordsSnapshot(
    val weight: List<WeightRecord> = emptyList(),
    val bodyFat: List<BodyFatRecord> = emptyList(),
    val exercise: List<ExerciseSessionRecord> = emptyList(),
    val activeCalories: List<ActiveCaloriesBurnedRecord> = emptyList(),
    val totalCalories: List<TotalCaloriesBurnedRecord> = emptyList(),
    val distance: List<DistanceRecord> = emptyList(),
    val heartRate: List<HeartRateRecord> = emptyList(),
    val speed: List<SpeedRecord> = emptyList(),
    val elevation: List<ElevationGainedRecord> = emptyList(),
    val steps: List<StepsRecord> = emptyList(),
    val stepsCadence: List<StepsCadenceRecord> = emptyList(),
    val issues: List<ReadIssue> = emptyList(),
)

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
     * Reads every record type the app has permission for, exactly once, then derives the data
     * origins list, weight summary and workout list from that single read pass. This is how the
     * Garmin Connect package name gets confirmed on a real device (requirement: do not hard-code
     * it up front), and how a failed read for one record type is surfaced instead of silently
     * looking like "no data".
     */
    suspend fun readDiagnosticSnapshot(
        range: TimeRangeFilter,
        granted: Set<String>,
        preferredSourcePackageName: String?,
    ): HealthConnectSnapshot {
        val c = client
            ?: return HealthConnectSnapshot(dataOrigins = emptyList(), weight = WeightSummary.EMPTY, workouts = emptyList(), issues = emptyList())

        val snapshot = readAllRecords(c, range, granted)

        return HealthConnectSnapshot(
            dataOrigins = discoverDataOrigins(snapshot),
            weight = weightSummary(snapshot, preferredSourcePackageName),
            workouts = workouts(c, snapshot, granted, preferredSourcePackageName),
            issues = snapshot.issues,
        )
    }

    private suspend fun readAllRecords(c: HealthConnectClient, range: TimeRangeFilter, granted: Set<String>): RecordsSnapshot {
        val issues = mutableListOf<ReadIssue>()

        suspend fun <T : Record> read(type: KClass<T>, permission: String): List<T> {
            if (permission !in granted) return emptyList()
            return readRecordsSafely(c, type, range).fold(
                onSuccess = { it },
                onFailure = { error ->
                    val name = HealthPermissions.namesFor(listOf(permission)).firstOrNull() ?: type.simpleName.orEmpty()
                    issues += ReadIssue(name, error.javaClass.simpleName ?: "Unknown error")
                    emptyList()
                },
            )
        }

        return RecordsSnapshot(
            weight = read(WeightRecord::class, permissionFor<WeightRecord>()),
            bodyFat = read(BodyFatRecord::class, permissionFor<BodyFatRecord>()),
            exercise = read(ExerciseSessionRecord::class, permissionFor<ExerciseSessionRecord>()),
            activeCalories = read(ActiveCaloriesBurnedRecord::class, permissionFor<ActiveCaloriesBurnedRecord>()),
            totalCalories = read(TotalCaloriesBurnedRecord::class, permissionFor<TotalCaloriesBurnedRecord>()),
            distance = read(DistanceRecord::class, permissionFor<DistanceRecord>()),
            heartRate = read(HeartRateRecord::class, permissionFor<HeartRateRecord>()),
            speed = read(SpeedRecord::class, permissionFor<SpeedRecord>()),
            elevation = read(ElevationGainedRecord::class, permissionFor<ElevationGainedRecord>()),
            steps = read(StepsRecord::class, permissionFor<StepsRecord>()),
            stepsCadence = read(StepsCadenceRecord::class, permissionFor<StepsCadenceRecord>()),
            issues = issues,
        )
    }

    private fun discoverDataOrigins(snapshot: RecordsSnapshot): List<DataOrigin> {
        val counts = linkedMapOf<String, Int>()
        fun tally(records: List<Record>) {
            for (record in records) {
                val pkg = record.metadata.dataOrigin.packageName
                counts[pkg] = (counts[pkg] ?: 0) + 1
            }
        }
        tally(snapshot.weight)
        tally(snapshot.bodyFat)
        tally(snapshot.exercise)
        tally(snapshot.activeCalories)
        tally(snapshot.totalCalories)
        tally(snapshot.distance)
        tally(snapshot.heartRate)
        tally(snapshot.speed)
        tally(snapshot.elevation)
        tally(snapshot.steps)
        tally(snapshot.stepsCadence)

        return counts.map { (pkg, count) -> DataOrigin(pkg, appLabelFor(pkg), count) }
            .sortedByDescending { it.recordCount }
    }

    private fun weightSummary(snapshot: RecordsSnapshot, preferredSourcePackageName: String?): WeightSummary {
        val records = snapshot.weight.let { all ->
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

    private suspend fun workouts(
        c: HealthConnectClient,
        snapshot: RecordsSnapshot,
        granted: Set<String>,
        preferredSourcePackageName: String?,
        limit: Int = 10,
    ): List<WorkoutSession> {
        val sessions = snapshot.exercise
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
    ): Result<List<T>> {
        return runCatching {
            client.readRecords(ReadRecordsRequest(recordType = type, timeRangeFilter = range)).records
        }.onFailure { Log.w(TAG, "Read failed for ${type.simpleName}: ${it.javaClass.simpleName}") }
    }

    private inline fun <reified T : Record> permissionFor(): String =
        androidx.health.connect.client.permission.HealthPermission.getReadPermission(T::class)

    companion object {
        fun thirtyDayWindow(now: Instant = Instant.now()): TimeRangeFilter =
            TimeRangeFilter.between(now.minusSeconds(30L * 24 * 60 * 60), now)
    }
}
