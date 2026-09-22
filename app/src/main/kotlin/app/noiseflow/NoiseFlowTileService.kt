package app.noiseflow

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.noiseflow.playback.PlaybackController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Quick Settings tile.
 *
 * The fastest path there is: pull down the shade and tap, without unlocking or
 * finding the app. That is the two-seconds-to-sound target from the concept
 * doc, and it is the path someone actually uses at 3am.
 */
@AndroidEntryPoint
class NoiseFlowTileService : TileService() {

    @Inject
    lateinit var playback: PlaybackController

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        playback.toggle()
        refresh()
    }

    private fun refresh() {
        qsTile?.apply {
            state = if (playback.state.value.isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.app_name)
            updateTile()
        }
    }
}
