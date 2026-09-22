package app.noiseflow.feature.mixer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.noiseflow.audio.LayerSpec
import app.noiseflow.audio.ModulationShape
import app.noiseflow.audio.generators.SoundSpec
import app.noiseflow.designsystem.component.LabelledSlider
import app.noiseflow.designsystem.component.OptionRow
import app.noiseflow.i18n.ToneLabels
import app.noiseflow.i18n.R as I18nR

@Composable
fun MixerScreen(
    onUpsell: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MixerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(I18nR.string.mixer_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }

        items(state.layers, key = { it.id }) { layer ->
            LayerCard(
                layer = layer,
                isPro = state.isPro,
                description = layer.describe(context),
                onLevel = { viewModel.setLevel(layer.id, it) },
                onPan = { viewModel.setPan(layer.id, it) },
                onWidth = { viewModel.setWidth(layer.id, it) },
                onRoom = { viewModel.setRoom(layer.id, it) },
                onModulation = { viewModel.setModulation(layer.id, it) },
                onRemove = { viewModel.removeLayer(layer.id) },
                onUpsell = onUpsell,
            )
        }

        item {
            if (state.canAddLayer) {
                TextButton(onClick = { viewModel.addLayer(SoundSpec.Tilted(DEFAULT_NEW_SLOPE)) }) {
                    Text(stringResource(I18nR.string.mixer_add_layer))
                }
            } else if (!state.isPro) {
                // Not a locked door with no handle: say what the limit is and
                // where the extra layers live, then let them decide.
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(I18nR.string.mixer_layer_limit))
                    TextButton(onClick = onUpsell) {
                        Text(stringResource(I18nR.string.paywall_pro))
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerCard(
    layer: LayerSpec,
    isPro: Boolean,
    description: String,
    onLevel: (Float) -> Unit,
    onPan: (Float) -> Unit,
    onWidth: (Float) -> Unit,
    onRoom: (Float) -> Unit,
    onModulation: (ModulationShape) -> Unit,
    onRemove: () -> Unit,
    onUpsell: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(description, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onRemove) {
                    Text(stringResource(I18nR.string.mixer_remove_layer))
                }
            }
            LabelledSlider(
                stringResource(I18nR.string.mixer_level),
                layer.level,
                onLevel,
                percent(layer.level),
            )
            LabelledSlider(
                stringResource(I18nR.string.mixer_pan),
                layer.pan,
                onPan,
                percent((layer.pan + 1f) / 2f),
                range = -1f..1f,
            )
            LabelledSlider(
                stringResource(I18nR.string.mixer_width),
                layer.width,
                onWidth,
                percent(layer.width),
            )
            LabelledSlider(
                stringResource(I18nR.string.mixer_room),
                layer.room,
                { if (isPro) onRoom(it) else onUpsell() },
                percent(layer.room),
                enabled = isPro,
            )
            Text(stringResource(I18nR.string.mixer_modulation))
            OptionRow(
                options = ModulationShape.entries,
                selected = layer.modulation,
                onSelect = { if (isPro) onModulation(it) else onUpsell() },
                labelFor = { stringResource(it.labelRes()) },
            )
        }
    }
}

private fun ModulationShape.labelRes(): Int = when (this) {
    ModulationShape.NONE -> I18nR.string.modulation_none
    ModulationShape.WAVES -> I18nR.string.modulation_waves
    ModulationShape.BREATH -> I18nR.string.modulation_breath
    ModulationShape.DRIFT -> I18nR.string.modulation_drift
}

private fun LayerSpec.describe(context: android.content.Context): String = when (val s = sound) {
    is SoundSpec.Tilted -> ToneLabels.describe(context, s.slopeDbPerOctave)
    SoundSpec.Grey -> context.getString(I18nR.string.noise_grey)
    SoundSpec.Green -> context.getString(I18nR.string.noise_green)
}

private fun percent(value: Float): String = "${(value * PERCENT).toInt()}%"

private const val PERCENT = 100
private const val DEFAULT_NEW_SLOPE = -3f
