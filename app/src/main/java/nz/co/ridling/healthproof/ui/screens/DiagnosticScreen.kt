package nz.co.ridling.healthproof.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nz.co.ridling.healthproof.R
import nz.co.ridling.healthproof.data.healthconnect.HealthConnectAvailability
import nz.co.ridling.healthproof.domain.DataOrigin
import nz.co.ridling.healthproof.domain.DiagnosticData
import nz.co.ridling.healthproof.domain.WorkoutSession
import nz.co.ridling.healthproof.util.formatKcal
import nz.co.ridling.healthproof.util.formatKg
import nz.co.ridling.healthproof.util.formatKgChange
import nz.co.ridling.healthproof.util.formatKm
import nz.co.ridling.healthproof.util.formatMeters
import nz.co.ridling.healthproof.util.formatSpeedAsPaceMinPerKm
import nz.co.ridling.healthproof.util.toDisplayString
import java.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    data: DiagnosticData,
    onRefresh: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onSelectPreferredSource: (String?) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenPrivacy) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.action_privacy_info))
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                },
            )
        },
    ) { padding ->
        val isEmpty = data.weight.recordsInWindow == 0 && data.workouts.isEmpty() && data.dataOrigins.isEmpty()

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ConnectionCard(data) }

            if (isEmpty) {
                item { EmptyDataCard() }
            } else {
                item { WeightCard(data) }
                item {
                    Text(
                        "Workouts (most recent ${data.workouts.size.coerceAtMost(10)})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (data.workouts.isEmpty()) {
                    item { Text("No exercise sessions found in the last 30 days.", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(data.workouts) { workout -> WorkoutCard(workout) }
                }
            }

            item {
                DataOriginsCard(
                    dataOrigins = data.dataOrigins,
                    preferredSourcePackageName = data.preferredSourcePackageName,
                    onSelectPreferredSource = onSelectPreferredSource,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ConnectionCard(data: DiagnosticData) {
    SectionCard(title = "Connection") {
        val availabilityLabel = when (data.connection.availability) {
            HealthConnectAvailability.Available -> "Available"
            else -> "Unavailable"
        }
        LabelValueRow("Health Connect", availabilityLabel)
        LabelValueRow(
            "Permissions",
            if (data.connection.permissionsGranted) "All granted" else "${data.connection.missingPermissionCount} missing",
        )
        LabelValueRow(
            "Last successful refresh",
            data.connection.lastSuccessfulRefresh?.toDisplayString() ?: "Never",
        )
        LabelValueRow("Data sources discovered", data.connection.dataSourceCount.toString())
    }
}

@Composable
private fun EmptyDataCard() {
    SectionCard(title = "No data found") {
        Text(
            "No weight or workout records were found in Health Connect for the last 30 days. " +
                "Make sure Garmin Connect is syncing to Health Connect, then refresh.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun WeightCard(data: DiagnosticData) {
    SectionCard(title = "Weight") {
        val weight = data.weight
        if (weight.latest == null) {
            Text("No weight records in the last 30 days.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LabelValueRow("Latest weight", weight.latest.kilograms.formatKg())
            LabelValueRow("Recorded", weight.latest.time.toDisplayString())
            LabelValueRow("Source", weight.latest.sourceAppLabel)
            LabelValueRow("Source package", weight.latest.sourcePackageName)
            if (weight.previous != null) {
                LabelValueRow("Previous weight", weight.previous.kilograms.formatKg())
            }
            if (weight.changeKilograms != null) {
                LabelValueRow("Change", weight.changeKilograms.formatKgChange())
            }
        }
        LabelValueRow("30-day records", weight.recordsInWindow.toString())
    }
}

@Composable
private fun WorkoutCard(workout: WorkoutSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(workout.title?.takeIf { it.isNotBlank() } ?: workout.exerciseType, fontWeight = FontWeight.Bold)
            Text(workout.exerciseType, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            LabelValueRow("Start", workout.startTime.toDisplayString())
            LabelValueRow("Duration", Duration.between(workout.startTime, workout.endTime).let { d ->
                val h = d.toHours(); val m = d.toMinutes() % 60
                if (h > 0) "${h}h ${m}m" else "${m}m"
            })
            workout.distanceMeters?.let { LabelValueRow("Distance", it.formatKm()) }
            workout.activeCaloriesKcal?.let { LabelValueRow("Active calories", it.formatKcal()) }
            workout.averageHeartRateBpm?.let { LabelValueRow("Avg heart rate", "$it bpm") }
            workout.maximumHeartRateBpm?.let { LabelValueRow("Max heart rate", "$it bpm") }
            workout.averageSpeedMetersPerSecond?.let { LabelValueRow("Avg pace", it.formatSpeedAsPaceMinPerKm()) }
            workout.elevationGainedMeters?.let { LabelValueRow("Elevation gained", it.formatMeters()) }
            Spacer(modifier = Modifier.height(4.dp))
            LabelValueRow("Source", workout.sourceAppLabel)
            LabelValueRow("Source package", workout.sourcePackageName)
        }
    }
}

@Composable
private fun DataOriginsCard(
    dataOrigins: List<DataOrigin>,
    preferredSourcePackageName: String?,
    onSelectPreferredSource: (String?) -> Unit,
) {
    SectionCard(title = "Data sources (metadata.dataOrigin.packageName)") {
        if (dataOrigins.isEmpty()) {
            Text("No data sources discovered yet.", style = MaterialTheme.typography.bodyMedium)
            return@SectionCard
        }
        Text(
            "Confirm which package is Garmin Connect on this device, then select it as the preferred source. " +
                "Weight and workouts will then be filtered to that source only, to avoid mixing duplicate data.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        dataOrigins.forEach { origin ->
            val isPreferred = origin.packageName == preferredSourcePackageName
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(origin.appLabel, fontWeight = FontWeight.Medium)
                    Text(origin.packageName, style = MaterialTheme.typography.bodySmall)
                    Text("${origin.recordCount} record(s) in last 30 days", style = MaterialTheme.typography.bodySmall)
                }
                Icon(
                    imageVector = if (isPreferred) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isPreferred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onSelectPreferredSource(origin.packageName) },
                    enabled = !isPreferred,
                ) { Text("Set as preferred") }
                if (isPreferred) {
                    TextButton(onClick = { onSelectPreferredSource(null) }) { Text("Clear") }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
