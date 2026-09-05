package nz.co.ridling.healthproof.domain

import nz.co.ridling.healthproof.data.healthconnect.HealthConnectAvailability
import java.time.Instant

/** One discovered contributor of Health Connect data, keyed by its package name. */
data class DataOrigin(
    val packageName: String,
    val appLabel: String,
    val recordCount: Int,
)

data class WeightSample(
    val kilograms: Double,
    val time: Instant,
    val sourcePackageName: String,
    val sourceAppLabel: String,
)

data class WeightSummary(
    val latest: WeightSample?,
    val previous: WeightSample?,
    val changeKilograms: Double?,
    val recordsInWindow: Int,
) {
    companion object {
        val EMPTY = WeightSummary(latest = null, previous = null, changeKilograms = null, recordsInWindow = 0)
    }
}

data class WorkoutSession(
    val exerciseType: String,
    val title: String?,
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Double?,
    val activeCaloriesKcal: Double?,
    val averageHeartRateBpm: Long?,
    val maximumHeartRateBpm: Long?,
    val averageSpeedMetersPerSecond: Double?,
    val elevationGainedMeters: Double?,
    val sourcePackageName: String,
    val sourceAppLabel: String,
)

data class ConnectionStatus(
    val availability: HealthConnectAvailability,
    val permissionsGranted: Boolean,
    val missingPermissionCount: Int,
    val lastSuccessfulRefresh: Instant?,
    val dataSourceCount: Int,
)

/** Everything the diagnostic screen needs, assembled by the repository on each refresh. */
data class DiagnosticData(
    val connection: ConnectionStatus,
    val weight: WeightSummary,
    val workouts: List<WorkoutSession>,
    val dataOrigins: List<DataOrigin>,
    val preferredSourcePackageName: String?,
)
