package cat.tarven.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val TavernDarkColorScheme = darkColorScheme(
    primary = TavernPurple,
    onPrimary = Color.White,
    primaryContainer = TavernPurpleDark,
    onPrimaryContainer = TavernPurpleLight,
    secondary = TavernGold,
    onSecondary = Color.Black,
    secondaryContainer = TavernGoldDark,
    onSecondaryContainer = TavernGoldLight,
    tertiary = InfoBlue,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    outlineVariant = DividerColor,
    error = ErrorRed,
    onError = Color.White,
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
)

@Composable
fun CattarvenTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = TavernDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}