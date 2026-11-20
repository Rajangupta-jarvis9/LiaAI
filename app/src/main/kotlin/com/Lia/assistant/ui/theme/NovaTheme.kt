package com.Lia.assistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.Lia.assistant.data.ThemeMode

@Composable
fun NovaTheme(
    mode         : ThemeMode = ThemeMode.DARK,
    reducedMotion: Boolean   = false,
    content      : @Composable () -> Unit,
) {
    val isDark = when (mode) {
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val novaColors = if (isDark) NovaDarkColors else NovaLightColors

    val m3Colors = if (isDark) darkColorScheme(
        primary          = novaColors.accent,
        onPrimary        = novaColors.onAccent,
        secondary        = novaColors.accentSecondary,
        background       = novaColors.background,
        surface          = novaColors.surface,
        onBackground     = novaColors.textPrimary,
        onSurface        = novaColors.textPrimary,
        onSurfaceVariant = novaColors.textSecondary,
        error            = novaColors.error,
    ) else lightColorScheme(
        primary          = novaColors.accent,
        onPrimary        = novaColors.onAccent,
        secondary        = novaColors.accentSecondary,
        background       = novaColors.background,
        surface          = novaColors.surface,
        onBackground     = novaColors.textPrimary,
        onSurface        = novaColors.textPrimary,
        onSurfaceVariant = novaColors.textSecondary,
        error            = novaColors.error,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor     = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val wic = WindowCompat.getInsetsController(window, view)
            wic.isAppearanceLightStatusBars     = !isDark
            wic.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalNovaColors        provides novaColors,
        LocalNovaReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography  = NovaTypography,
            content     = content,
        )
    }
}

@Composable
fun novaColors() = LocalNovaColors.current
