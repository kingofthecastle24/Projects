package nz.co.ridling.healthproof.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nz.co.ridling.healthproof.R
import nz.co.ridling.healthproof.data.healthconnect.HealthConnectAvailability

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun UnavailableScreen(
    availability: HealthConnectAvailability,
    onInstallOrUpdate: () -> Unit,
    onRetry: () -> Unit,
) {
    val (title, body, actionLabel) = when (availability) {
        HealthConnectAvailability.NotInstalled -> Triple(
            "Health Connect isn't installed",
            "This app reads your weight and workout data through Android Health Connect. Install it from the Play Store to continue.",
            stringResource(R.string.action_install_health_connect),
        )
        HealthConnectAvailability.RequiresUpdate -> Triple(
            "Health Connect needs updating",
            "Your installed version of Health Connect is too old to support the data types this app reads. Please update it.",
            stringResource(R.string.action_update_health_connect),
        )
        else -> Triple(
            "Health Connect is unavailable",
            "Health Connect isn't supported on this device, so Garmin weight and workout data can't be read.",
            null,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        if (actionLabel != null) {
            Button(onClick = onInstallOrUpdate) { Text(actionLabel) }
        }
        OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.action_retry))
        }
    }
}

@Composable
fun PermissionRequiredScreen(
    deniedPreviously: Boolean,
    onGrantAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.HealthAndSafety,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.rationale_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.rationale_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        if (deniedPreviously) {
            Text(
                "It looks like access hasn't been granted yet. You can try again, or open Health Connect directly to grant it there.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        Button(onClick = onGrantAccess) { Text(stringResource(R.string.action_grant_access)) }
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.action_open_health_connect_settings))
        }
        OutlinedButton(onClick = onOpenPrivacy, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.action_privacy_info))
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge)
        Text(
            "Reading Health Connect data failed ($message). No health information was included in this message.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}
