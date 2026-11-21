package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.ThemeMode

@Immutable
data class SaviiExtendedColors(
    val isDark: Boolean,
    val glassSurface: Color,
    val glassBorder: Color,
    val subtleAccent: Color,
    val cardGradientTop: Color,
    val cardGradientBottom: Color,
    val statusPending: Color,
    val statusInProgress: Color,
    val statusCompleted: Color,
    val statusIncomplete: Color,
    val statusOverdue: Color,
    val statusImportant: Color,
)

private val DarkExtendedColors = SaviiExtendedColors(
    isDark = true,
    glassSurface = GlassTintDark,
    glassBorder = GlassBorderDark,
    subtleAccent = ElectricBlue,
    cardGradientTop = MidnightElevated,
    cardGradientBottom = MidnightNavy,
    statusPending = StatusPendingBlue,
    statusInProgress = StatusInProgressYellow,
    statusCompleted = StatusCompletedGreen,
    statusIncomplete = StatusIncompleteRed,
    statusOverdue = StatusOverdueRed,
    statusImportant = StatusImportantPurple,
)

private val LightExtendedColors = SaviiExtendedColors(
    isDark = false,
    glassSurface = GlassTintLight,
    glassBorder = GlassBorderLight,
    subtleAccent = ElectricBlueDeep,
    cardGradientTop = LightSurface,
    cardGradientBottom = LightSurfaceAlt,
    statusPending = StatusPendingBlue,
    statusInProgress = StatusInProgressYellow,
    statusCompleted = StatusCompletedGreen,
    statusIncomplete = StatusIncompleteRed,
    statusOverdue = StatusOverdueRed,
    statusImportant = StatusImportantPurple,
)

val LocalSaviiColors = staticCompositionLocalOf { DarkExtendedColors }

private val SaviiDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = MidnightElevated,
    onPrimaryContainer = ElectricVioletSoft,
    secondary = ElectricBlue,
    onSecondary = Obsidian,
    secondaryContainer = MidnightSurface,
    onSecondaryContainer = SubtleBlueAccent,
    tertiary = StatusImportantPurple,
    onTertiary = Color.White,
    background = Obsidian,
    onBackground = InkPrimaryDark,
    surface = MidnightNavy,
    onSurface = InkPrimaryDark,
    surfaceVariant = MidnightSurface,
    onSurfaceVariant = InkSecondaryDark,
    outline = GlassBorderDark,
    error = StatusIncompleteRed,
    onError = Color.White,
)

private val SaviiLightColorScheme = lightColorScheme(
    primary = ElectricVioletDeep,
    onPrimary = Color.White,
    primaryContainer = LightElevated,
    onPrimaryContainer = ElectricVioletDeep,
    secondary = ElectricBlueDeep,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceAlt,
    onSecondaryContainer = ElectricBlueDeep,
    tertiary = StatusImportantPurple,
    onTertiary = Color.White,
    background = LightCanvas,
    onBackground = InkPrimaryLight,
    surface = LightSurface,
    onSurface = InkPrimaryLight,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = InkSecondaryLight,
    outline = GlassBorderLight,
    error = StatusIncompleteRed,
    onError = Color.White,
)

@Composable
fun SaviiFocusTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> SaviiDarkColorScheme
        else -> SaviiLightColorScheme
    }

    val extendedColors = if (useDark) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDark
                isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(LocalSaviiColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SaviiTypography,
            content = content,
        )
    }
}
