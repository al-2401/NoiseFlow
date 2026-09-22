package app.noiseflow.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.ActionCallback
import app.noiseflow.designsystem.theme.Skins
import app.noiseflow.playback.PlaybackController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * One-tap resume from the home screen.
 *
 * Deliberately does one thing: start or stop the last mix. A widget that needs
 * reading in the dark is a widget nobody uses.
 */
class NoiseFlowWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content(context) }
    }

    @Composable
    private fun Content(context: Context) {
        // Glance recomposes on update() rather than by observing a flow, so
        // this reads the current value and ToggleAction re-renders after it
        // flips. Good enough for a two-state widget; a collectAsState here
        // would keep the widget host subscribed for nothing.
        val playing = controllerOf(context).state.value.isPlaying
        GlanceTheme {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Skins.Midnight.palette.surface))
                    .padding(12.dp)
                    .clickable(actionRunCallback<ToggleAction>()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (playing) "⏸" else "▶")
            }
        }
    }
}

class NoiseFlowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoiseFlowWidget()
}

class ToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters,
    ) {
        controllerOf(context).toggle()
        NoiseFlowWidget().update(context, glanceId)
    }
}

/**
 * Widgets are not Hilt injection targets, so the singleton graph is reached
 * through an entry point rather than a second PlaybackController being built.
 * Two controllers would mean two engines and two AudioTracks.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun playbackController(): PlaybackController
}

private fun controllerOf(context: Context): PlaybackController =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        WidgetEntryPoint::class.java,
    ).playbackController()
