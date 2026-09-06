package nz.co.ridling.healthproof.data.healthconnect

/** Result of checking whether Health Connect is usable on this device. */
sealed interface HealthConnectAvailability {
    data object Available : HealthConnectAvailability
    data object NotInstalled : HealthConnectAvailability
    data object RequiresUpdate : HealthConnectAvailability
    data object Unavailable : HealthConnectAvailability
}
