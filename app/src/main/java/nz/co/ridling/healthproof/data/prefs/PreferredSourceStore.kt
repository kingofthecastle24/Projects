package nz.co.ridling.healthproof.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(
    name = "health_proof_prefs",
)

private val PREFERRED_SOURCE_PACKAGE_KEY = stringPreferencesKey("preferred_source_package")

/**
 * The only thing this app persists locally right now: which package name (e.g. Garmin
 * Connect's) the user has confirmed as their preferred Health Connect data source.
 * No health data values themselves are ever written here.
 */
class PreferredSourceStore(private val context: Context) {

    val preferredSourcePackageName: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[PREFERRED_SOURCE_PACKAGE_KEY] }

    suspend fun setPreferredSource(packageName: String?) {
        context.dataStore.edit { prefs ->
            if (packageName == null) {
                prefs.remove(PREFERRED_SOURCE_PACKAGE_KEY)
            } else {
                prefs[PREFERRED_SOURCE_PACKAGE_KEY] = packageName
            }
        }
    }
}
