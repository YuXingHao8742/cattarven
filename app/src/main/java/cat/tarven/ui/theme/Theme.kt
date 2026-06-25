package cat.tarven.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val TavernLightColorScheme = lightColorScheme(
    primary = TavernPurple,
    onPrimary = Color.White,
    primaryContainer = TavernPurpleLight,
    onPrimaryContainer = TavernPurpleDark,
    secondary = TavernGold,
    onSecondary = Color.Black,
    secondaryContainer = TavernGoldLight,
    onSecondaryContainer = TavernGoldDark,
    tertiary = InfoBlue,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderColor,
    outlineVariant = LightDividerColor,
    error = ErrorRed,
    onError = Color.White,
    inverseSurface = LightTextPrimary,
    inverseOnSurface = LightBackground,
)

@Composable
fun CattarvenTheme(
    darkTheme: Boolean = true,
    appFontSize: Float = 14f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TavernDarkColorScheme else TavernLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = if (darkTheme) DarkBackground else LightBackground
            window.statusBarColor = bgColor.toArgb()
            window.navigationBarColor = bgColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getScaledTypography(appFontSize),
        content = content
    )
}

val MaterialTheme.surfaceElevated: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightSurfaceElevated else DarkSurfaceElevated

val MaterialTheme.bubbleUser: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightUserBubble else UserBubble

val MaterialTheme.bubbleUserBorder: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightUserBubbleBorder else UserBubbleBorder

val MaterialTheme.bubbleAssistant: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightAssistantBubble else AssistantBubble

val MaterialTheme.bubbleAssistantBorder: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightAssistantBubbleBorder else AssistantBubbleBorder

val MaterialTheme.bubbleSystem: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightSystemBubble else SystemBubble

val MaterialTheme.bubbleSystemBorder: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightSystemBubbleBorder else SystemBubbleBorder

val MaterialTheme.textMuted: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightTextMuted else TextMuted

val MaterialTheme.inputBackground: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightInputBackground else InputBackground

val MaterialTheme.inputBorder: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightInputBorder else InputBorder

val MaterialTheme.chipBackground: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightChipBackground else ChipBackground

val MaterialTheme.chipText: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightChipText else ChipText

val MaterialTheme.cardBackground: Color
    @Composable
    get() = if (colorScheme.background == LightBackground) LightCard else DarkCard