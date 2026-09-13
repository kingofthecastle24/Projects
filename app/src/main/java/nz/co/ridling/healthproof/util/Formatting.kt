package nz.co.ridling.healthproof.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private fun dateTimeFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())

fun Instant.toDisplayString(zoneId: ZoneId = ZoneId.systemDefault()): String =
    dateTimeFormatter().format(this.atZone(zoneId))

fun Double.formatKg(): String = String.format(Locale.getDefault(), "%.1f kg", this)

fun Double.formatKgChange(): String {
    val sign = if (this >= 0) "+" else "-"
    return String.format(Locale.getDefault(), "%s%.1f kg", sign, abs(this))
}

fun Double.formatKm(): String = String.format(Locale.getDefault(), "%.2f km", this / 1000.0)

fun Double.formatKcal(): String = String.format(Locale.getDefault(), "%.0f kcal", this)

fun Double.formatSpeedAsPaceMinPerKm(): String {
    if (this <= 0.0) return "--"
    val totalSecondsPerKm = Math.round(1000.0 / this)
    val minutes = totalSecondsPerKm / 60
    val seconds = totalSecondsPerKm % 60
    return String.format(Locale.getDefault(), "%d:%02d /km", minutes, seconds)
}

fun Double.formatMeters(): String = String.format(Locale.getDefault(), "%.0f m", this)

fun Duration.toDisplayString(): String {
    val hours = toHours()
    val minutes = toMinutes() % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
