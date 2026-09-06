package nz.co.ridling.healthproof.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient

private const val HEALTH_CONNECT_PACKAGE_NAME = "com.google.android.apps.healthdata"

/** Sends the user to the Play Store listing that installs or updates the Health Connect app. */
fun openHealthConnectInPlayStore(context: Context) {
    val uri = "market://details?id=$HEALTH_CONNECT_PACKAGE_NAME&url=healthconnect%3A%2F%2Fonboarding".toUri()
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setPackage("com.android.vending")
        data = uri
        putExtra("overlay", true)
        putExtra("callerId", context.packageName)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE_NAME".toUri(),
            ),
        )
    }
}

/** Opens Health Connect's own "App permissions" settings, where the user can revoke access at any time. */
fun openHealthConnectSettings(context: Context) {
    try {
        context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
    } catch (_: ActivityNotFoundException) {
        openHealthConnectInPlayStore(context)
    }
}
