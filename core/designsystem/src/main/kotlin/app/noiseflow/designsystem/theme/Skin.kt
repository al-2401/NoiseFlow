package app.noiseflow.designsystem.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.noiseflow.designsystem.R
import app.noiseflow.designsystem.visualizer.VisualizerId

/**
 * A skin is a mood, not a palette.
 *
 * Colours, type, the visualiser and the control style all travel together —
 * switching skin should feel like changing the room, which is the only reason
 * a cosmetic feature earns its place in a sleep app. All skins are free: they
 * are how the app looks in store screenshots, so putting them behind a paywall
 * would cost more in acquisition than it could earn.
 */
@Immutable
data class Skin(
    val id: String,
    @StringRes val nameRes: Int,
    val palette: SkinPalette,
    val visualizer: VisualizerId,
    val control: ControlStyle = ControlStyle.Minimal,
    val isDark: Boolean = true,
    /** Material You: colours come from the wallpaper instead of the palette. */
    val dynamicColor: Boolean = false,
    val monospace: Boolean = false,
)

/** How the primary controls are drawn. Layout is identical in every skin. */
enum class ControlStyle {
    /** Flat slider and a circular play button. */
    Minimal,

    /** A physical dial and a toggle switch, on a moulded housing. */
    Machine,

    /** Numeric readouts and a real spectrum, monospaced. */
    Terminal,
}

@Immutable
data class SkinPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val onBackground: Color,
    val muted: Color,
    val accent: Color,
    /**
     * Tint laid over everything on the night screen. Warm and very dark: blue
     * light at 3am is the one thing a sleep app must not put in someone's eyes.
     */
    val nightTint: Color,
)

object Skins {

    val Midnight = Skin(
        id = "midnight",
        nameRes = R.string.skin_midnight,
        visualizer = VisualizerId.Rings,
        palette = SkinPalette(
            background = Color(0xFF06080F),
            surface = Color(0xFF0D1220),
            surfaceVariant = Color(0xFF161D31),
            primary = Color(0xFF6C8CFF),
            onPrimary = Color(0xFF050813),
            onBackground = Color(0xFFDCE3F5),
            muted = Color(0xFF6A7490),
            accent = Color(0xFF8FB4FF),
            nightTint = Color(0xFF1A0E00),
        ),
    )

    val Ember = Skin(
        id = "ember",
        nameRes = R.string.skin_ember,
        visualizer = VisualizerId.Ember,
        palette = SkinPalette(
            background = Color(0xFF0B0603),
            surface = Color(0xFF17100A),
            surfaceVariant = Color(0xFF241710),
            primary = Color(0xFFFF9B54),
            onPrimary = Color(0xFF1A0A00),
            onBackground = Color(0xFFF2DCC8),
            muted = Color(0xFF8A6B54),
            accent = Color(0xFFFFC08A),
            nightTint = Color(0xFF1F0E00),
        ),
    )

    /**
     * The skeuomorphic one. A moulded housing, a grille and a real dial, in the
     * spirit of the hardware sleep machines people grew up with. It is one
     * skin, not the interface language: the layout and the controls are the
     * same as everywhere else, only their presentation changes.
     */
    val Machine = Skin(
        id = "machine",
        nameRes = R.string.skin_machine,
        visualizer = VisualizerId.Machine,
        control = ControlStyle.Machine,
        palette = SkinPalette(
            background = Color(0xFF15161A),
            surface = Color(0xFF23262C),
            surfaceVariant = Color(0xFF2E3138),
            primary = Color(0xFFD8CFC0),
            onPrimary = Color(0xFF1A1B1F),
            onBackground = Color(0xFFE6E2DA),
            muted = Color(0xFF7C8089),
            accent = Color(0xFFB4A88F),
            nightTint = Color(0xFF1A0E00),
        ),
    )

    val System = Skin(
        id = "system",
        nameRes = R.string.skin_system,
        visualizer = VisualizerId.Neutral,
        dynamicColor = true,
        palette = Midnight.palette,
    )

    val Aurora = Skin(
        id = "aurora",
        nameRes = R.string.skin_aurora,
        visualizer = VisualizerId.Aurora,
        palette = SkinPalette(
            background = Color(0xFF04090C),
            surface = Color(0xFF0A161A),
            surfaceVariant = Color(0xFF122428),
            primary = Color(0xFF5FE3C0),
            onPrimary = Color(0xFF00201A),
            onBackground = Color(0xFFD5F2EA),
            muted = Color(0xFF5E8A82),
            accent = Color(0xFFA985FF),
            nightTint = Color(0xFF1A0E00),
        ),
    )

    val Forest = Skin(
        id = "forest",
        nameRes = R.string.skin_forest,
        visualizer = VisualizerId.Foliage,
        palette = SkinPalette(
            background = Color(0xFF070B07),
            surface = Color(0xFF101710),
            surfaceVariant = Color(0xFF1A241A),
            primary = Color(0xFF8FBF6A),
            onPrimary = Color(0xFF0B1206),
            onBackground = Color(0xFFDDE8D4),
            muted = Color(0xFF6B7D62),
            accent = Color(0xFFC8D9A8),
            nightTint = Color(0xFF1A0E00),
        ),
    )

    /** The one light skin, for daytime focus use. */
    val Paper = Skin(
        id = "paper",
        nameRes = R.string.skin_paper,
        visualizer = VisualizerId.Seismograph,
        isDark = false,
        palette = SkinPalette(
            background = Color(0xFFF7F5F0),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFECE8E0),
            primary = Color(0xFF2E2A24),
            onPrimary = Color(0xFFF7F5F0),
            onBackground = Color(0xFF1C1A16),
            muted = Color(0xFF8A857C),
            accent = Color(0xFF9A6B3F),
            nightTint = Color(0xFF1A0E00),
        ),
    )

    /**
     * For people who like that there is real DSP underneath: numbers, a
     * monospaced face and an actual spectrum analyser. Cheap to build and the
     * most shared screenshot we are likely to have.
     */
    val Terminal = Skin(
        id = "terminal",
        nameRes = R.string.skin_terminal,
        visualizer = VisualizerId.Spectrum,
        control = ControlStyle.Terminal,
        monospace = true,
        palette = SkinPalette(
            background = Color(0xFF000000),
            surface = Color(0xFF060A06),
            surfaceVariant = Color(0xFF0C140C),
            primary = Color(0xFF3BE86B),
            onPrimary = Color(0xFF001A06),
            onBackground = Color(0xFF9DF5B4),
            muted = Color(0xFF2F6B3E),
            accent = Color(0xFF7CFFA0),
            nightTint = Color(0xFF1A0E00),
        ),
    )

    /** Shipped in 1.0; the rest arrive in 1.1. */
    val launchSet = listOf(Midnight, Ember, Machine, System)

    val all = listOf(Midnight, Ember, Machine, System, Aurora, Forest, Paper, Terminal)

    fun byId(id: String): Skin = all.firstOrNull { it.id == id } ?: Midnight
}
