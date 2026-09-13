package nz.co.ridling.healthproof

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import nz.co.ridling.healthproof.data.healthconnect.HealthPermissions
import nz.co.ridling.healthproof.ui.DiagnosticViewModel
import nz.co.ridling.healthproof.ui.screens.ConnectionTestScreen
import nz.co.ridling.healthproof.ui.screens.DiagnosticScreen
import nz.co.ridling.healthproof.ui.screens.ErrorScreen
import nz.co.ridling.healthproof.ui.screens.LoadingScreen
import nz.co.ridling.healthproof.ui.screens.PermissionRequiredScreen
import nz.co.ridling.healthproof.ui.screens.PrivacyScreen
import nz.co.ridling.healthproof.ui.screens.UnavailableScreen
import nz.co.ridling.healthproof.ui.state.DiagnosticUiState
import nz.co.ridling.healthproof.ui.theme.HealthProofTheme
import nz.co.ridling.healthproof.util.openHealthConnectInPlayStore
import nz.co.ridling.healthproof.util.openHealthConnectSettings

private const val ROUTE_DIAGNOSTIC = "diagnostic"
private const val ROUTE_PRIVACY = "privacy"
private const val ROUTE_CONNECTION_TEST = "connection_test"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthProofTheme {
                HealthProofApp()
            }
        }
    }
}

@Composable
private fun HealthProofApp() {
    val context = LocalContext.current
    val viewModel: DiagnosticViewModel = viewModel(
        factory = DiagnosticViewModel.factory(context.applicationContext as Application),
    )
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    var permissionLaunchError by remember { mutableStateOf<String?>(null) }
    var lastPermissionResultCount by remember { mutableStateOf<Int?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(HealthPermissions.requestPermissionsContract()) { grantedByDialog ->
        lastPermissionResultCount = grantedByDialog.size
        viewModel.onPermissionRequestCompleted()
    }

    // Health Connect permissions can change while this app isn't in the foreground - most
    // commonly, the user granting or revoking them from the Health Connect app directly rather
    // than through this app's own request flow. Re-check on every resume so that's picked up
    // without the user having to tap anything in this app first.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    NavHost(navController = navController, startDestination = ROUTE_DIAGNOSTIC) {
        composable(ROUTE_DIAGNOSTIC) {
            when (val state = uiState) {
                is DiagnosticUiState.Loading -> LoadingScreen()

                is DiagnosticUiState.Unavailable -> UnavailableScreen(
                    availability = state.availability,
                    onInstallOrUpdate = { openHealthConnectInPlayStore(context) },
                    onRetry = { viewModel.refresh() },
                )

                is DiagnosticUiState.PermissionRequired -> PermissionRequiredScreen(
                    deniedPreviously = state.deniedPreviously,
                    onGrantAccess = {
                        permissionLaunchError = try {
                            permissionLauncher.launch(viewModel.permissionsToRequest)
                            null
                        } catch (e: Exception) {
                            "${e.javaClass.simpleName}: ${e.message}"
                        }
                    },
                    onOpenSettings = { openHealthConnectSettings(context) },
                    onOpenPrivacy = { navController.navigate(ROUTE_PRIVACY) },
                    diagnosticMessage = permissionLaunchError
                        ?: lastPermissionResultCount?.let { "Health Connect's own dialog last returned $it permission(s) granted." },
                )

                is DiagnosticUiState.Content -> DiagnosticScreen(
                    data = state.data,
                    onRefresh = { viewModel.refresh() },
                    onOpenPrivacy = { navController.navigate(ROUTE_PRIVACY) },
                    onSelectPreferredSource = { pkg -> viewModel.selectPreferredSource(pkg) },
                    onRunConnectionTest = { navController.navigate(ROUTE_CONNECTION_TEST) },
                )

                is DiagnosticUiState.Error -> ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.refresh() },
                )
            }
        }
        composable(ROUTE_PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
        composable(ROUTE_CONNECTION_TEST) {
            val currentData = (uiState as? DiagnosticUiState.Content)?.data
            if (currentData != null) {
                ConnectionTestScreen(data = currentData, onBack = { navController.popBackStack() })
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
    }
}
