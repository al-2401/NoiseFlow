package app.noiseflow.playback

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Adapts [PlaybackController] to the media3 [Player] interface so the system
 * gets a proper media session: notification transport, lock screen, headset
 * buttons and Bluetooth, none of which we want to reimplement.
 *
 * [SimpleBasePlayer] exists precisely for playback that is not file-backed,
 * which is our case: there is no media to seek through, just a generator that
 * is either running or not.
 */
class NoisePlayer(
    looper: Looper,
    private val controller: PlaybackController,
    private val title: String,
    private val artist: String,
) : SimpleBasePlayer(looper) {

    override fun getState(): State {
        val current = controller.state.value
        return State.Builder()
            .setAvailableCommands(COMMANDS)
            .setPlaybackState(Player.STATE_READY)
            .setPlayWhenReady(current.isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaylist(listOf(mediaItemData()))
            .setContentPositionMs { (current.elapsedSeconds * MILLIS_PER_SECOND).toLong() }
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) controller.play() else controller.pause()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        controller.pause()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        controller.pause(fadeOutSeconds = 0.2f)
        return Futures.immediateVoidFuture()
    }

    private fun mediaItemData(): MediaItemData =
        MediaItemData.Builder(MEDIA_ID)
            .setMediaItem(MediaItem.Builder().setMediaId(MEDIA_ID).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            // The stream never ends on its own; the sleep timer ends it.
            .setIsSeekable(false)
            .setIsDynamic(true)
            .build()

    private companion object {
        const val MEDIA_ID = "noiseflow:mix"
        const val MILLIS_PER_SECOND = 1_000

        val COMMANDS: Player.Commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_STOP,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_SET_VOLUME,
                Player.COMMAND_GET_VOLUME,
                Player.COMMAND_RELEASE,
            )
            .build()
    }
}
