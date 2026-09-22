package app.noiseflow.feature.presets

import android.content.Context
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.noiseflow.data.model.FactoryPresets
import app.noiseflow.data.model.Preset
import app.noiseflow.i18n.R as I18nR

@Composable
fun PresetsScreen(
    modifier: Modifier = Modifier,
    viewModel: PresetsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                stringResource(I18nR.string.presets_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        items(state.factory, key = { it.id }) { preset ->
            PresetCard(
                title = preset.displayName(context),
                isActive = preset.id == state.activeId,
                onApply = { viewModel.apply(preset) },
                onShare = { viewModel.share(preset) },
                onDelete = null,
            )
        }
        if (state.mine.isNotEmpty()) {
            item {
                Text(
                    stringResource(I18nR.string.presets_mine),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
        items(state.mine, key = { it.id }) { preset ->
            PresetCard(
                title = preset.name,
                isActive = preset.id == state.activeId,
                onApply = { viewModel.apply(preset) },
                onShare = { viewModel.share(preset) },
                onDelete = { viewModel.delete(preset.id) },
            )
        }
    }
}

@Composable
private fun PresetCard(
    title: String,
    isActive: Boolean,
    onApply: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onApply),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (isActive) {
                    Text(
                        stringResource(I18nR.string.player_play),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Row {
                TextButton(onClick = onShare) { Text(stringResource(I18nR.string.presets_share)) }
                onDelete?.let {
                    TextButton(onClick = it) { Text(stringResource(I18nR.string.presets_delete)) }
                }
            }
        }
    }
}

/**
 * Factory presets carry an id, not a name: the stored id is stable forever
 * while the visible name has to change with the user's language.
 */
private fun Preset.displayName(context: Context): String = when (id) {
    FactoryPresets.UPSTAIRS_NEIGHBOUR -> context.getString(I18nR.string.preset_neighbour)
    FactoryPresets.AEROPLANE -> context.getString(I18nR.string.preset_aeroplane)
    FactoryPresets.NURSERY -> context.getString(I18nR.string.preset_nursery)
    FactoryPresets.RAINFALL -> context.getString(I18nR.string.preset_rainfall)
    FactoryPresets.DEEP -> context.getString(I18nR.string.preset_deep)
    FactoryPresets.FOCUS -> context.getString(I18nR.string.preset_focus)
    FactoryPresets.SNORING -> context.getString(I18nR.string.preset_snoring)
    FactoryPresets.TRAFFIC -> context.getString(I18nR.string.preset_traffic)
    else -> name
}
