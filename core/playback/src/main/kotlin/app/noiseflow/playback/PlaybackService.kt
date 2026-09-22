package app.noiseflow.playback

import android.content.Intent
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the render thread alive for the whole night.
 *
 * No wake lock: a foreground service of type mediaPlayback with an active
 * AudioTrack is enough, and taking a partial wake lock on top of that is a
 * well-known way to lose the battery budget for nothing.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var controller: PlaybackController

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = NoisePlayer(
            looper = Looper.getMainLooper(),
            controller = controller,
            title = getString(R.string.playback_notification_title),
            artist = getString(R.string.playback_notification_subtitle),
        )
        session = MediaSession.Builder(this, player).build()

        // The controller is the source of truth, so the session has to be told
        // whenever it changes underneath us -- the sleep timer stopping
        // playback is not something the session initiated.
        lifecycleScope.launch {
            controller.state
                .map { it.isPlaying }
                .distinctUntilChanged()
                .collect {
                    player.invalidateState()
                    if (!it) stopSelf()
                }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away while sound is playing is not a request to stop:
        // people do it on purpose before putting the phone down.
        if (!controller.state.value.isPlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
