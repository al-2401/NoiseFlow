package app.noiseflow.playback

import android.media.AudioAttributes
import android.media.AudioFormat as AndroidAudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import app.noiseflow.audio.NoiseEngine
import kotlin.math.max

/**
 * Pumps [NoiseEngine] into an [AudioTrack] on a dedicated thread.
 *
 * Two deliberate choices, both about battery rather than latency:
 *
 *  - A ~200 ms buffer. Nothing here is interactive, so a large buffer just
 *    means the CPU wakes up five times a second instead of fifty. Over eight
 *    hours that difference is the battery budget.
 *  - PERFORMANCE_MODE_POWER_SAVING, which lets the framework route through the
 *    deep buffer / offload path where the device has one. This is the reason
 *    minSdk is 26.
 */
class AudioTrackSink(
    private val engine: NoiseEngine,
    private val bufferSeconds: Float = DEFAULT_BUFFER_SECONDS,
) {
    private var thread: Thread? = null

    @Volatile
    private var running = false

    /** Set when the engine has faded out and the track may be released. */
    @Volatile
    var onSilent: (() -> Unit)? = null

    val isRunning: Boolean get() = running

    @Synchronized
    fun start() {
        if (running) return
        running = true
        thread = Thread(::renderLoop, "NoiseFlow-Render").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.join(JOIN_TIMEOUT_MS)
        thread = null
    }

    private fun renderLoop() {
        val track = try {
            buildTrack()
        } catch (e: UnsupportedOperationException) {
            Log.e(TAG, "could not open an AudioTrack", e)
            running = false
            return
        }

        val frames = engine.blockFrames
        val buffer = FloatArray(frames * CHANNELS)
        try {
            track.play()
            while (running) {
                engine.render(buffer, frames)
                var written = 0
                while (written < buffer.size && running) {
                    val n = track.write(
                        buffer,
                        written,
                        buffer.size - written,
                        AudioTrack.WRITE_BLOCKING,
                    )
                    if (n < 0) {
                        Log.e(TAG, "AudioTrack.write failed: $n")
                        running = false
                        break
                    }
                    written += n
                }
                if (engine.isSilent) {
                    running = false
                    onSilent?.invoke()
                }
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private fun buildTrack(): AudioTrack {
        val minBytes = AudioTrack.getMinBufferSize(
            engine.sampleRate,
            AndroidAudioFormat.CHANNEL_OUT_STEREO,
            AndroidAudioFormat.ENCODING_PCM_FLOAT,
        )
        val wanted = (engine.sampleRate * bufferSeconds * CHANNELS * BYTES_PER_FLOAT).toInt()
        val bufferBytes = max(minBytes, wanted)

        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AndroidAudioFormat.Builder()
                    .setEncoding(AndroidAudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(engine.sampleRate)
                    .setChannelMask(AndroidAudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setPerformanceMode(AudioTrack.PERFORMANCE_MODE_POWER_SAVING)
                }
            }
            .build()
    }

    private companion object {
        const val TAG = "AudioTrackSink"
        const val CHANNELS = 2
        const val BYTES_PER_FLOAT = 4
        const val DEFAULT_BUFFER_SECONDS = 0.2f
        const val JOIN_TIMEOUT_MS = 2_000L
    }
}
