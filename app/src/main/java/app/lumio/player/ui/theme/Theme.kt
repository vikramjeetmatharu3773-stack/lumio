package app.lumio.player.ui.theme

import android.app.Activity
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import app.lumio.player.data.settings.AccentSource
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.data.settings.ThemeMode

object LumioColors {
    val BgDark = Color(0xFF07070D)
    val BgLight = Color(0xFFF3F4FA)
    val OnDark = Color(0xFFF2F2F7)
    val OnLight = Color(0xFF14141C)
}

private fun buildScheme(accent: Color, dark: Boolean, amoled: Boolean): ColorScheme {
    val bg = when { amoled -> Color.Black; dark -> LumioColors.BgDark; else -> LumioColors.BgLight }
    val on = if (dark) LumioColors.OnDark else LumioColors.OnLight
    val onAccent = if (accent.luminance() > 0.5f) Color(0xFF101018) else Color.White
    return if (dark) darkColorScheme(
        primary = accent, onPrimary = onAccent, secondary = accent, tertiary = accent,
        background = bg, onBackground = on, surface = bg, onSurface = on,
        surfaceVariant = Color(0xFF1A1A26), onSurfaceVariant = on.copy(alpha = 0.7f),
        outline = Color.White.copy(alpha = 0.2f), error = Color(0xFFFF6B6B),
    ) else lightColorScheme(
        primary = accent, onPrimary = onAccent, secondary = accent, tertiary = accent,
        background = bg, onBackground = on, surface = bg, onSurface = on,
        surfaceVariant = Color(0xFFE4E6F2), onSurfaceVariant = on.copy(alpha = 0.7f),
        outline = Color.Black.copy(alpha = 0.2f), error = Color(0xFFD32F2F),
    )
}

@Composable
fun LumioTheme(
    settings: AppSettings,
    artworkAccent: Color?,
    backdrop: BackdropState,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        else -> true
    }
    val systemAnimOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val animations = settings.animationsEnabled && !systemAnimOff
    val custom = Color(settings.customAccent)
    val target = when (settings.accentSource) {
        AccentSource.ARTWORK -> artworkAccent ?: custom
        AccentSource.CUSTOM -> custom
        AccentSource.MATERIAL_YOU ->
            if (Build.VERSION.SDK_INT >= 31) (if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
            else custom
    }
    val accent by animateColorAsState(target, tween(if (animations) 600 else 0), label = "accent")
    val glass = remember(
        settings.glassBlur, settings.glassOpacity, settings.glassBorder, settings.glassSpecular,
        settings.glassRefraction, settings.glassCorner, settings.reduceTransparency, systemAnimOff,
    ) {
        GlassStyle(
            blur = settings.glassBlur, opacity = settings.glassOpacity, border = settings.glassBorder,
            specular = settings.glassSpecular, refraction = settings.glassRefraction,
            corner = settings.glassCorner.dp, reduceTransparency = settings.reduceTransparency || systemAnimOff,
        )
    }
    val motion = remember(animations, settings.animationSpeed, settings.haptics) {
        MotionSpec(animations, settings.animationSpeed, settings.haptics)
    }
    val scheme = remember(accent, dark, settings.themeMode) { buildScheme(accent, dark, settings.themeMode == ThemeMode.AMOLED) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalGlass provides glass,
        LocalBackdrop provides backdrop,
        LocalIsDark provides dark,
        LocalMotion provides motion,
    ) {
        MaterialTheme(colorScheme = scheme, typography = LumioTypography, content = content)
    }
}
