package app.noiseflow.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.noiseflow.designsystem.component.LabelledSlider
import app.noiseflow.designsystem.component.PlayControl
import app.noiseflow.designsystem.component.ToneControl
import app.noiseflow.designsystem.theme.LocalSkin
import app.noiseflow.designsystem.visualizer.Visualizer
import app.noiseflow.i18n.DurationLabels
import app.noiseflow.i18n.ToneLabels
import app.noiseflow.i18n.R as I18nR

/**
 * The only screen most people will ever use.
 *
 * Everything that matters sits in the lower half within thumb reach, because
 * the posture this app is used in is lying down, one-handed, in the dark.
 */
@Composable
fun PlayerScreen(
    onOpenMixer: () -> Unit,
    onOpenNightScreen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val skin = LocalSkin.current

    Box(
        modifier
            .fillMaxSize()
            .background(skin.palette.background),
    ) {
        Visualizer(
            id = skin.visualizer,
            level = state.effectiveLevel,
            // Only ever animates while sound is running and this screen is up.
            animated = state.isPlaying,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state.timerRemainingMillis?.let { remaining ->
                Text(
                    text = stringResource(
                        I18nR.string.timer_remaining,
                        DurationLabels.remaining(context, remaining),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            if (!state.hasSound) {
                Text(
                    stringResource(I18nR.string.player_no_sound),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            ToneControl(
                slope = state.toneSlope,
                onSlopeChange = viewModel::setTone,
                label = stringResource(I18nR.string.tone_label),
                valueDescription = ToneLabels.accessibilityValue(context, state.toneSlope),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
            )

            LabelledSlider(
                label = stringResource(I18nR.string.player_volume),
                value = state.masterLevel,
                onValueChange = viewModel::setLevel,
                valueDescription = "${(state.effectiveLevel * PERCENT).toInt()}%",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )

            PlayControl(
                isPlaying = state.isPlaying,
                onToggle = viewModel::toggle,
                playLabel = stringResource(I18nR.string.player_play),
                pauseLabel = stringResource(I18nR.string.player_pause),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(onClick = onOpenMixer) {
                    Text(stringResource(I18nR.string.player_open_mixer))
                }
                TextButton(onClick = onOpenNightScreen) {
                    Text(stringResource(I18nR.string.player_night_screen))
                }
            }
        }
    }
}

/**
 * What the phone shows for the other seven hours.
 *
 * Near-black, warm, no animation, no controls but the one that leaves. This is
 * the cheapest visible win we have over every competitor: none of them have a
 * screen you can look at at 3am without being dazzled.
 */
@Composable
fun NightScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val skin = LocalSkin.current
    val exitLabel = stringResource(I18nR.string.night_screen_exit)

    Box(
        modifier
            .fillMaxSize()
            .background(skin.palette.nightTint)
            .clickable(onClick = onExit)
            .semantics { contentDescription = exitLabel },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            state.timerRemainingMillis?.let {
                Text(
                    DurationLabels.remaining(context, it),
                    style = MaterialTheme.typography.displaySmall,
                    color = skin.palette.muted,
                )
            }
            Text(
                exitLabel,
                style = MaterialTheme.typography.labelSmall,
                color = skin.palette.muted,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

private const val PERCENT = 100
