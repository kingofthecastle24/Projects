package nz.co.ridling.healthproof

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import nz.co.ridling.healthproof.data.healthconnect.HealthPermissions
import nz.co.ridling.healthproof.ui.DiagnosticViewModel
import nz.co.ridling.healthproof.ui.foodlog.FoodLogScreen
import nz.co.ridling.healthproof.ui.foodlog.FoodLogViewModel
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
private const val ROUTE_FOOD_LOG = "food_log"

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
    val application = context.applicationContext as Application
    val diagnosticViewModel: DiagnosticViewModel = viewModel(factory = DiagnosticViewModel.factory(application))
    val foodLogViewModel: FoodLogViewModel = viewModel(factory = FoodLogViewModel.factory(application))
    val uiState by diagnosticViewModel.uiState.collectAsState()
    val foodLogUiState by foodLogViewModel.uiState.collectAsState()
    val navController = rememberNavController()

    val permissionLauncher = rememberLauncherForActivityResult(HealthPermissions.requestPermissionsContract()) {
        diagnosticViewModel.onPermissionRequestCompleted()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == ROUTE_DIAGNOSTIC || currentRoute == ROUTE_FOOD_LOG

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_DIAGNOSTIC,
                        onClick = {
                            navController.navigate(ROUTE_DIAGNOSTIC) {
                                popUpTo(ROUTE_DIAGNOSTIC) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.MonitorHeart, contentDescription = null) },
                        label = { Text("Health") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == ROUTE_FOOD_LOG,
                        onClick = {
                            navController.navigate(ROUTE_FOOD_LOG) {
                                popUpTo(ROUTE_DIAGNOSTIC) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Restaurant, contentDescription = null) },
                        label = { Text("Food Log") },
                    )
                }
            }
        },
    ) { outerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_DIAGNOSTIC,
            modifier = Modifier.padding(outerPadding),
        ) {
            composable(ROUTE_DIAGNOSTIC) {
                when (val state = uiState) {
                    is DiagnosticUiState.Loading -> LoadingScreen()

                    is DiagnosticUiState.Unavailable -> UnavailableScreen(
                        availability = state.availability,
                        onInstallOrUpdate = { openHealthConnectInPlayStore(context) },
                        onRetry = { diagnosticViewModel.refresh() },
                    )

                    is DiagnosticUiState.PermissionRequired -> PermissionRequiredScreen(
                        deniedPreviously = state.deniedPreviously,
                        onGrantAccess = { permissionLauncher.launch(diagnosticViewModel.permissionsToRequest) },
                        onOpenSettings = { openHealthConnectSettings(context) },
                        onOpenPrivacy = { navController.navigate(ROUTE_PRIVACY) },
                    )

                    is DiagnosticUiState.Content -> DiagnosticScreen(
                        data = state.data,
                        onRefresh = { diagnosticViewModel.refresh() },
                        onOpenPrivacy = { navController.navigate(ROUTE_PRIVACY) },
                        onSelectPreferredSource = { pkg -> diagnosticViewModel.selectPreferredSource(pkg) },
                        onRunConnectionTest = { navController.navigate(ROUTE_CONNECTION_TEST) },
                    )

                    is DiagnosticUiState.Error -> ErrorScreen(
                        message = state.message,
                        onRetry = { diagnosticViewModel.refresh() },
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
            composable(ROUTE_FOOD_LOG) {
                FoodLogScreen(
                    state = foodLogUiState,
                    onPreviousDay = { foodLogViewModel.goToPreviousDay() },
                    onNextDay = { foodLogViewModel.goToNextDay() },
                    onToday = { foodLogViewModel.goToToday() },
                    onAddEntry = { category -> foodLogViewModel.startNewEntry(category) },
                    onEditEntry = { entry -> foodLogViewModel.startEditEntry(entry) },
                    onDeleteEntry = { entry -> foodLogViewModel.deleteEntry(entry) },
                    onSaveEntry = { entry -> foodLogViewModel.saveEntry(entry) },
                    onCancelEdit = { foodLogViewModel.cancelEdit() },
                )
            }
        }
    }
}
