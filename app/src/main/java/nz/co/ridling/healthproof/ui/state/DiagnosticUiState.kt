package nz.co.ridling.healthproof.ui.state

import nz.co.ridling.healthproof.data.healthconnect.HealthConnectAvailability
import nz.co.ridling.healthproof.domain.DiagnosticData

/**
 * Top-level screen states for the diagnostic home screen. [Content] additionally covers the
 * "empty data" case internally (zero records after a successful, fully-permitted refresh) since
 * that is just a property of the data, not a separate flow.
 */
sealed interface DiagnosticUiState {
    data object Loading : DiagnosticUiState

    data class Unavailable(val availability: HealthConnectAvailability) : DiagnosticUiState

    data class PermissionRequired(val deniedPreviously: Boolean) : DiagnosticUiState

    data class Content(val data: DiagnosticData) : DiagnosticUiState

    data class Error(val message: String) : DiagnosticUiState
}
