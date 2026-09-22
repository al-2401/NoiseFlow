package app.noiseflow.data

import app.noiseflow.data.model.LayerConfig
import app.noiseflow.data.model.ModulationConfig
import app.noiseflow.data.model.Preset
import app.noiseflow.data.model.SoundConfig
import app.noiseflow.data.store.PresetLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PresetLinkTest {

    private val preset = Preset(
        id = "user.1",
        name = "Ночной дождь",
        layers = listOf(
            LayerConfig("a", SoundConfig.Tilt(-4.5f), level = 0.8f, modulation = ModulationConfig.WAVES),
            LayerConfig("b", SoundConfig.Green, level = 0.3f, room = 0.4f),
        ),
        createdAtMillis = 1_700_000_000_000L,
    )

    @Test
    fun `round trips through a link`() {
        val decoded = PresetLink.fromUri(PresetLink.toUri(preset))
        assertEquals(preset, decoded)
    }

    @Test
    fun `link is short enough to share in a message`() {
        val uri = PresetLink.toUri(preset)
        assertTrue(uri.length < 400, "link was ${uri.length} characters: $uri")
    }

    @Test
    fun `factory flag never travels`() {
        // Otherwise a shared preset would masquerade as one we shipped.
        val shared = PresetLink.decode(PresetLink.encode(preset.copy(isFactory = true)))
        assertEquals(false, shared?.isFactory)
    }

    @Test
    fun `garbage decodes to null rather than throwing`() {
        assertNull(PresetLink.decode("not-a-real-token"))
        assertNull(PresetLink.fromUri("https://example.com/whatever"))
        assertNull(PresetLink.decode(""))
    }
}
