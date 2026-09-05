package nz.co.ridling.healthproof.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first
import nz.co.ridling.healthproof.data.healthconnect.HealthConnectAvailability
import nz.co.ridling.healthproof.data.healthconnect.HealthConnectManager
import nz.co.ridling.healthproof.data.healthconnect.HealthPermissions
import nz.co.ridling.healthproof.data.prefs.PreferredSourceStore
import nz.co.ridling.healthproof.domain.ConnectionStatus
import nz.co.ridling.healthproof.domain.DiagnosticData
import java.time.Instant

sealed interface RefreshResult {
    data class Success(val data: DiagnosticData) : RefreshResult
    data class HealthConnectNotReady(val availability: HealthConnectAvailability) : RefreshResult
    data class PermissionsMissing(val missingCount: Int, val data: DiagnosticData) : RefreshResult
    data class Error(val message: String) : RefreshResult
}

/**
 * Coordinates the Health Connect manager and the small amount of local preference state,
 * and assembles everything the UI needs into one [DiagnosticData] snapshot per refresh.
 */
class HealthDataRepository(
    context: Context,
    private val healthConnectManager: HealthConnectManager = HealthConnectManager(context.applicationContext),
    private val preferredSourceStore: PreferredSourceStore = PreferredSourceStore(context.applicationContext),
) {
    private var lastSuccessfulRefresh: Instant? = null

    fun availability(): HealthConnectAvailability = healthConnectManager.availability()

    suspend fun grantedPermissions() = healthConnectManager.grantedPermissions()

    fun permissionSet(): Set<String> = HealthPermissions.ALL

    suspend fun setPreferredSource(packageName: String?) = preferredSourceStore.setPreferredSource(packageName)

    suspend fun refresh(): RefreshResult {
        val availability = healthConnectManager.availability()
        if (availability != HealthConnectAvailability.Available) {
            return RefreshResult.HealthConnectNotReady(availability)
        }

        return runCatching {
            val granted = healthConnectManager.grantedPermissions()
            val missing = HealthPermissions.ALL - granted
            val range = HealthConnectManager.thirtyDayWindow()
            val preferredSource = preferredSourceStore.preferredSourcePackageName.first()

            val snapshot = healthConnectManager.readDiagnosticSnapshot(range, granted, preferredSource)

            if (granted.isNotEmpty()) {
                lastSuccessfulRefresh = Instant.now()
            }

            val connection = ConnectionStatus(
                availability = availability,
                permissionsGranted = missing.isEmpty(),
                missingPermissionCount = missing.size,
                lastSuccessfulRefresh = lastSuccessfulRefresh,
                dataSourceCount = snapshot.dataOrigins.size,
            )

            val data = DiagnosticData(
                connection = connection,
                weight = snapshot.weight,
                workouts = snapshot.workouts,
                dataOrigins = snapshot.dataOrigins,
                preferredSourcePackageName = preferredSource,
                missingRecordTypeNames = HealthPermissions.namesFor(missing),
                readIssues = snapshot.issues,
            )

            if (missing.isNotEmpty() && granted.isEmpty()) {
                RefreshResult.PermissionsMissing(missing.size, data)
            } else {
                RefreshResult.Success(data)
            }
        }.getOrElse { throwable ->
            Log.e(TAG, "Refresh failed: ${throwable.javaClass.simpleName}")
            RefreshResult.Error(throwable.javaClass.simpleName ?: "Unknown error")
        }
    }

    companion object {
        private const val TAG = "HealthDataRepository"
    }
}
