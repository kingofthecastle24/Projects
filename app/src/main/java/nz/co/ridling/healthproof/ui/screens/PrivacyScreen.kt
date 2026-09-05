package nz.co.ridling.healthproof.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nz.co.ridling.healthproof.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            PrivacySection(
                title = "What is read",
                body = "This app reads, from Android Health Connect only: body weight, body fat percentage, " +
                    "exercise sessions, active calories burned, total calories burned, distance, heart rate, " +
                    "speed, elevation gained, step count, and step cadence. It reads the most recent 30 days " +
                    "of each. It never reads anything Health Connect doesn't already hold.",
            )
            PrivacySection(
                title = "Why it is used",
                body = "This is a proof-of-concept for a personal nutrition, weight and workout app. Reading " +
                    "this data lets the app show your Garmin-recorded weight trend and recent workouts without " +
                    "you re-entering anything by hand.",
            )
            PrivacySection(
                title = "Revoking access",
                body = "You are always in control. Open the Health Connect app, or this app's entry under " +
                    "Health Connect > App permissions, and you can revoke any or all of these permissions at " +
                    "any time. This app will simply stop being able to read new data once revoked.",
            )
            PrivacySection(
                title = "Your Garmin account",
                body = "This app never asks for, stores, or transmits your Garmin Connect username or password. " +
                    "It never talks to Garmin's servers or app directly. It only reads the data that Garmin " +
                    "Connect itself has already written into Health Connect on this device.",
            )
            PrivacySection(
                title = "Does anything leave this phone?",
                body = "No. This version of the app makes no network requests and has no server, analytics, " +
                    "or cloud sync of any kind. Every value shown on the diagnostic screen was read from, and " +
                    "stays on, this device.",
            )
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
}
