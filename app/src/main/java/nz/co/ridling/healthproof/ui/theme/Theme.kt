package nz.co.ridling.healthproof.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SeedGreen = Color(0xFF1B5E20)
private val SeedGreenLight = Color(0xFF4C8C4A)

private val LightColors = lightColorScheme(
    primary = SeedGreen,
    secondary = SeedGreenLight,
)

private val DarkColors = darkColorScheme(
    primary = SeedGreenLight,
    secondary = SeedGreen,
)

@Composable
fun HealthProofTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
