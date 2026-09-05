package nz.co.ridling.healthproof.domain

import nz.co.ridling.healthproof.data.healthconnect.HealthConnectAvailability
import nz.co.ridling.healthproof.util.formatKg
import nz.co.ridling.healthproof.util.toDisplayString
import java.time.Instant

/**
 * Plain-text summary for the "Run Connection Test" / "Copy or Export Diagnostics" feature.
 *
 * Deliberately limited to connection health, source discovery, and single latest-value
 * snapshots (weight, workout) - never the full 30-day record lists, so sharing this text
 * never hands over detailed health history.
 */
fun buildDiagnosticsReport(data: DiagnosticData, generatedAt: Instant = Instant.now()): String = buildString {
    appendLine("Health Connect Proof — Connection Test")
    appendLine("Generated: ${generatedAt.toDisplayString()}")
    appendLine()

    appendLine("CONNECTION")
    val availabilityLabel = when (data.connection.availability) {
        HealthConnectAvailability.Available -> "Available"
        HealthConnectAvailability.NotInstalled -> "Not installed"
        HealthConnectAvailability.RequiresUpdate -> "Requires update"
        HealthConnectAvailability.Unavailable -> "Unavailable"
    }
    appendLine("Health Connect: $availabilityLabel")
    appendLine(
        if (data.connection.permissionsGranted) {
            "Permissions: All granted"
        } else {
            "Permissions: ${data.connection.missingPermissionCount} missing"
        },
    )
    appendLine(
        "Missing record types: " +
            if (data.missingRecordTypeNames.isEmpty()) "None" else data.missingRecordTypeNames.joinToString(", "),
    )
    appendLine("Last successful refresh: ${data.connection.lastSuccessfulRefresh?.toDisplayString() ?: "Never"}")
    appendLine("Data sources discovered: ${data.connection.dataSourceCount}")
    appendLine()

    appendLine("DATA SOURCES")
    if (data.dataOrigins.isEmpty()) {
        appendLine("None discovered.")
    } else {
        for (origin in data.dataOrigins) {
            val preferredTag = if (origin.packageName == data.preferredSourcePackageName) " [PREFERRED]" else ""
            appendLine("- ${origin.appLabel} (${origin.packageName}) — ${origin.recordCount} record(s)$preferredTag")
        }
    }
    appendLine()

    appendLine("SELECTED PREFERRED SOURCE")
    appendLine(data.preferredSourcePackageName ?: "None selected")
    appendLine()

    appendLine("LATEST WEIGHT")
    val latestWeight = data.weight.latest
    if (latestWeight == null) {
        appendLine("No weight records found.")
    } else {
        appendLine("${latestWeight.kilograms.formatKg()} at ${latestWeight.time.toDisplayString()} (source: ${latestWeight.sourceAppLabel})")
    }
    appendLine()

    appendLine("LATEST WORKOUT")
    val latestWorkout = data.workouts.firstOrNull()
    if (latestWorkout == null) {
        appendLine("No exercise sessions found.")
    } else {
        appendLine("${latestWorkout.exerciseType} at ${latestWorkout.startTime.toDisplayString()} (source: ${latestWorkout.sourceAppLabel})")
    }
    appendLine()

    appendLine("ERRORS")
    if (data.readIssues.isEmpty()) {
        appendLine("None")
    } else {
        for (issue in data.readIssues) {
            appendLine("- ${issue.recordTypeName}: ${issue.message}")
        }
    }
}.trimEnd()
