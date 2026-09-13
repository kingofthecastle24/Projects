package nz.co.ridling.healthproof.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nz.co.ridling.healthproof.data.HealthDataRepository
import nz.co.ridling.healthproof.data.RefreshResult
import nz.co.ridling.healthproof.data.healthconnect.HealthConnectAvailability
import nz.co.ridling.healthproof.ui.state.DiagnosticUiState

class DiagnosticViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HealthDataRepository(application)

    private val _uiState = MutableStateFlow<DiagnosticUiState>(DiagnosticUiState.Loading)
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    private var permissionRequestAttempted = false

    val permissionsToRequest: Set<String> get() = repository.permissionSet()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = DiagnosticUiState.Loading

            val availability = repository.availability()
            if (availability != HealthConnectAvailability.Available) {
                _uiState.value = DiagnosticUiState.Unavailable(availability)
                return@launch
            }

            when (val result = repository.refresh()) {
                is RefreshResult.Success -> _uiState.value = DiagnosticUiState.Content(result.data)
                is RefreshResult.PermissionsMissing ->
                    _uiState.value = DiagnosticUiState.PermissionRequired(permissionRequestAttempted)
                is RefreshResult.HealthConnectNotReady ->
                    _uiState.value = DiagnosticUiState.Unavailable(result.availability)
                is RefreshResult.Error -> _uiState.value = DiagnosticUiState.Error(result.message)
            }
        }
    }

    /** Call after the Health Connect permission request activity result returns, granted or not. */
    fun onPermissionRequestCompleted() {
        permissionRequestAttempted = true
        refresh()
    }

    fun selectPreferredSource(packageName: String?) {
        viewModelScope.launch {
            repository.setPreferredSource(packageName)
            refresh()
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DiagnosticViewModel(application) as T
                }
            }
    }
}
