package app.noiseflow.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

val LocalSkin = staticCompositionLocalOf { Skins.Midnight }

/** True while the night screen is up: stops animation and warms the palette. */
val LocalNightMode = staticCompositionLocalOf { false }

@Composable
fun NoiseFlowTheme(
    skin: Skin = Skins.Midnight,
    nightMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val colors = when {
        skin.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        skin.isDark -> darkColorScheme(
            primary = skin.palette.primary,
            onPrimary = skin.palette.onPrimary,
            background = skin.palette.background,
            onBackground = skin.palette.onBackground,
            surface = skin.palette.surface,
            onSurface = skin.palette.onBackground,
            surfaceVariant = skin.palette.surfaceVariant,
            onSurfaceVariant = skin.palette.muted,
            secondary = skin.palette.accent,
            outline = skin.palette.muted,
        )

        else -> lightColorScheme(
            primary = skin.palette.primary,
            onPrimary = skin.palette.onPrimary,
            background = skin.palette.background,
            onBackground = skin.palette.onBackground,
            surface = skin.palette.surface,
            onSurface = skin.palette.onBackground,
            surfaceVariant = skin.palette.surfaceVariant,
            onSurfaceVariant = skin.palette.muted,
            secondary = skin.palette.accent,
            outline = skin.palette.muted,
        )
    }

    val typography = if (skin.monospace) {
        Typography().let { base ->
            base.copy(
                displayLarge = base.displayLarge.copy(fontFamily = FontFamily.Monospace),
                headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                labelLarge = base.labelLarge.copy(fontFamily = FontFamily.Monospace),
            )
        }
    } else {
        Typography()
    }

    CompositionLocalProvider(
        LocalSkin provides skin,
        LocalNightMode provides nightMode,
    ) {
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}
