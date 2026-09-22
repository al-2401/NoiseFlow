package app.noiseflow.data.store

import app.noiseflow.data.model.Preset
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Encodes a preset into a shareable link, with no server involved.
 *
 * Sharing a mix is the only viral mechanism in the product, and running a
 * backend for it would mean accounts, storage, moderation and a privacy policy
 * that says we keep your data. Compressing the preset into the URL itself
 * avoids all of that: the link works offline, forever, and we never see it.
 */
object PresetLink {

    const val SCHEME = "noiseflow"
    const val HOST = "preset"

    private val json = Json { ignoreUnknownKeys = true }
    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder: Base64.Decoder = Base64.getUrlDecoder()

    fun encode(preset: Preset): String {
        val payload = json.encodeToString(Preset.serializer(), preset.copy(isFactory = false))
            .encodeToByteArray()
        return encoder.encodeToString(deflate(payload))
    }

    fun toUri(preset: Preset): String = "$SCHEME://$HOST/${encode(preset)}"

    /** Returns null for anything that is not one of our links. */
    fun decode(token: String): Preset? = try {
        val raw = inflate(decoder.decode(token))
        json.decodeFromString(Preset.serializer(), raw.decodeToString())
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: SerializationException) {
        null
    }

    fun fromUri(uri: String): Preset? {
        val prefix = "$SCHEME://$HOST/"
        if (!uri.startsWith(prefix)) return null
        return decode(uri.removePrefix(prefix))
    }

    private fun deflate(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        return try {
            deflater.setInput(input)
            deflater.finish()
            val out = ByteArray(input.size + HEADROOM)
            val size = deflater.deflate(out)
            out.copyOf(size)
        } finally {
            deflater.end()
        }
    }

    private fun inflate(input: ByteArray): ByteArray {
        val inflater = Inflater(true)
        return try {
            inflater.setInput(input)
            val out = ByteArray(MAX_DECOMPRESSED)
            val size = inflater.inflate(out)
            // A link that expands past the cap is either corrupt or hostile.
            require(size in 1 until MAX_DECOMPRESSED) { "implausible preset payload" }
            out.copyOf(size)
        } finally {
            inflater.end()
        }
    }

    private const val HEADROOM = 64
    private const val MAX_DECOMPRESSED = 16 * 1024
}
