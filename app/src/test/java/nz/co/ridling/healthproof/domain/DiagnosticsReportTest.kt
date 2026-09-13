package nz.co.ridling.healthproof.domain

import nz.co.ridling.healthproof.data.healthconnect.HealthConnectAvailability
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DiagnosticsReportTest {

    private fun connection(
        availability: HealthConnectAvailability = HealthConnectAvailability.Available,
        permissionsGranted: Boolean = true,
        missingPermissionCount: Int = 0,
        lastSuccessfulRefresh: Instant? = Instant.parse("2026-09-04T19:15:00Z"),
        dataSourceCount: Int = 1,
    ) = ConnectionStatus(availability, permissionsGranted, missingPermissionCount, lastSuccessfulRefresh, dataSourceCount)

    @Test
    fun `full report includes weight, workout and preferred source`() {
        val data = DiagnosticData(
            connection = connection(),
            weight = WeightSummary(
                latest = WeightSample(82.5, Instant.parse("2026-09-04T19:15:00Z"), "com.garmin.android.apps.connectmobile", "Garmin Connect"),
                previous = null,
                changeKilograms = null,
                recordsInWindow = 1,
            ),
            workouts = listOf(
                WorkoutSession(
                    exerciseType = "Running",
                    title = null,
                    startTime = Instant.parse("2026-09-04T18:02:00Z"),
                    endTime = Instant.parse("2026-09-04T18:32:00Z"),
                    distanceMeters = 5000.0,
                    activeCaloriesKcal = 320.0,
                    averageHeartRateBpm = 150,
                    maximumHeartRateBpm = 172,
                    averageSpeedMetersPerSecond = 2.8,
                    elevationGainedMeters = 12.0,
                    sourcePackageName = "com.garmin.android.apps.connectmobile",
                    sourceAppLabel = "Garmin Connect",
                ),
            ),
            dataOrigins = listOf(DataOrigin("com.garmin.android.apps.connectmobile", "Garmin Connect", 14)),
            preferredSourcePackageName = "com.garmin.android.apps.connectmobile",
        )

        val report = buildDiagnosticsReport(data, generatedAt = Instant.parse("2026-09-05T01:00:00Z"))

        assertTrue(report.contains("Health Connect: Available"))
        assertTrue(report.contains("Permissions: All granted"))
        assertTrue(report.contains("Garmin Connect (com.garmin.android.apps.connectmobile) — 14 record(s) [PREFERRED]"))
        assertTrue(report.contains("82.5 kg at"))
        assertTrue(report.contains("Running at"))
        assertTrue(report.contains("ERRORS"))
        assertTrue(report.contains("None"))
        // Must not leak the full 30-day history or per-metric workout detail into the export.
        assertFalse(report.contains("5000.0"))
        assertFalse(report.contains("320.0"))
    }

    @Test
    fun `empty state reports no records and no sources`() {
        val data = DiagnosticData(
            connection = connection(dataSourceCount = 0),
            weight = WeightSummary.EMPTY,
            workouts = emptyList(),
            dataOrigins = emptyList(),
            preferredSourcePackageName = null,
        )

        val report = buildDiagnosticsReport(data)

        assertTrue(report.contains("No weight records found."))
        assertTrue(report.contains("No exercise sessions found."))
        assertTrue(report.contains("None discovered."))
        assertTrue(report.contains("None selected"))
    }

    @Test
    fun `missing permissions and read issues are both listed`() {
        val data = DiagnosticData(
            connection = connection(permissionsGranted = false, missingPermissionCount = 2),
            weight = WeightSummary.EMPTY,
            workouts = emptyList(),
            dataOrigins = emptyList(),
            preferredSourcePackageName = null,
            missingRecordTypeNames = listOf("Body fat", "Step cadence"),
            readIssues = listOf(ReadIssue("Heart rate", "SecurityException")),
        )

        val report = buildDiagnosticsReport(data)

        assertTrue(report.contains("Permissions: 2 missing"))
        assertTrue(report.contains("Missing record types: Body fat, Step cadence"))
        assertTrue(report.contains("- Heart rate: SecurityException"))
    }
}
