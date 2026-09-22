package app.noiseflow.playback

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import app.noiseflow.audio.LayerSpec
import app.noiseflow.audio.NightCurve
import app.noiseflow.audio.NoiseEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(
    val isPlaying: Boolean = false,
    val masterLevel: Float = DEFAULT_LEVEL,
    val layers: List<LayerSpec> = emptyList(),
    val timerRemainingMillis: Long? = null,
    val elapsedSeconds: Double = 0.0,
    val nightCurveEnabled: Boolean = false,
) {
    companion object {
        const val DEFAULT_LEVEL = 0.8f
    }
}

/**
 * The single source of truth for what is playing. UI, the media session and
 * the widget all go through here; none of them touch the engine directly.
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scope: CoroutineScope,
) {
    private val engine = NoiseEngine()
    private val sink = AudioTrackSink(engine)
    private val alarms = context.getSystemService(AlarmManager::class.java)

    val sleepTimer = SleepTimer()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var ticker: Job? = null

    init {
        sink.onSilent = { onEngineWentSilent() }
        engine.setMasterLevel(_state.value.masterLevel)
    }

    fun setMix(layers: List<LayerSpec>) {
        _state.update { it.copy(layers = layers) }
        engine.setLayers(layers)
    }

    fun setMasterLevel(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        _state.update { it.copy(masterLevel = clamped) }
        engine.setMasterLevel(clamped)
    }

    fun setNightCurveEnabled(enabled: Boolean) {
        _state.update { it.copy(nightCurveEnabled = enabled) }
        engine.setNightCurve(if (enabled) NightCurve.DEFAULT else null)
    }

    fun setSleepTimer(mode: SleepTimer.Mode) {
        sleepTimer.set(mode, nextAlarmMillis())
    }

    fun toggle() = if (_state.value.isPlaying) pause() else play()

    fun play() {
        if (_state.value.isPlaying) return
        if (_state.value.layers.isEmpty()) return
        _state.update { it.copy(isPlaying = true) }
        engine.resetClock()
        engine.start()
        sink.start()
        startForegroundService()
        startTicker()
    }

    /** Fades out and lets the render thread finish; see [onEngineWentSilent]. */
    fun pause(fadeOutSeconds: Float = DEFAULT_FADE_OUT) {
        if (!_state.value.isPlaying) return
        engine.stop(fadeOutSeconds)
    }

    private fun onEngineWentSilent() {
        sink.stop()
        sleepTimer.clear()
        _state.update { it.copy(isPlaying = false, timerRemainingMillis = null) }
        ticker?.cancel()
        ticker = null
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        elapsedSeconds = engine.elapsedSeconds,
                        timerRemainingMillis = sleepTimer.state.value.remainingMillis(now),
                    )
                }
                if (sleepTimer.hasExpired()) {
                    pause(sleepTimer.fadeOutSeconds)
                    sleepTimer.clear()
                }
                delay(TICK_MILLIS)
            }
        }
    }

    private fun nextAlarmMillis(): Long? =
        alarms?.nextAlarmClock?.triggerTime

    private fun startForegroundService() {
        val intent = Intent(context, PlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
        const val DEFAULT_FADE_OUT = 3f
    }
}
