package app.noiseflow.billing

/**
 * Where the free tier ends.
 *
 * All of it in one place, so what is paid is a product decision that can be
 * read in ten lines rather than an emergent property of scattered `if (isPro)`
 * checks.
 *
 * The shape of the split matters as much as its contents: the free tier has no
 * time limit and no adverts, and every generator plus the Tone control is
 * free. Competitors cap free use at ten minutes a session or ninety minutes a
 * day, and that is the thing their reviews complain about. Pro sells depth --
 * more layers, movement, the night curve, the nature library -- not access.
 *
 * Skins are free at every tier on purpose: they are what the store screenshots
 * show, so gating them would cost more in installs than it could earn.
 */
object FeatureGate {

    const val FREE_LAYER_LIMIT = 2
    const val PRO_LAYER_LIMIT = 8
    const val FREE_PRESET_LIMIT = 3

    fun layerLimit(isPro: Boolean): Int = if (isPro) PRO_LAYER_LIMIT else FREE_LAYER_LIMIT

    fun presetLimit(isPro: Boolean): Int = if (isPro) Int.MAX_VALUE else FREE_PRESET_LIMIT

    fun canUseModulation(isPro: Boolean): Boolean = isPro

    fun canUseRoom(isPro: Boolean): Boolean = isPro

    fun canUseNightCurve(isPro: Boolean): Boolean = isPro

    fun canUseWidgets(isPro: Boolean): Boolean = isPro

    /** Free forever, for everyone. Do not add a caller that passes false. */
    fun canUseAllGenerators(): Boolean = true

    fun canUseAllSkins(): Boolean = true

    fun hasTimeLimit(): Boolean = false
}
