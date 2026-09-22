package app.noiseflow.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.noiseflow.designsystem.component.LabelledSlider
import app.noiseflow.designsystem.component.OptionRow
import app.noiseflow.designsystem.theme.Skins
import java.util.Locale
import app.noiseflow.i18n.R as I18nR

@Composable
fun SettingsScreen(
    onUpsell: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings

    LazyColumn(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                stringResource(I18nR.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }

        item {
            Section(stringResource(I18nR.string.settings_skin)) {
                // Every skin, at every tier. See FeatureGate for why.
                OptionRow(
                    options = Skins.launchSet,
                    selected = Skins.byId(settings.skinId),
                    onSelect = { viewModel.setSkin(it.id) },
                    labelFor = { stringResource(it.nameRes) },
                )
            }
        }

        item {
            Section(stringResource(I18nR.string.settings_language)) {
                OptionRow(
                    options = listOf<String?>(null) + state.availableLocales,
                    selected = state.currentLocaleTag,
                    onSelect = viewModel::setLocale,
                    labelFor = { tag ->
                        if (tag == null) {
                            stringResource(I18nR.string.settings_language_system)
                        } else {
                            Locale.forLanguageTag(tag).getDisplayName(Locale.forLanguageTag(tag))
                        }
                    },
                )
            }
        }

        item {
            SwitchRow(
                title = stringResource(I18nR.string.settings_night_curve),
                subtitle = stringResource(I18nR.string.settings_night_curve_hint),
                checked = settings.nightCurveEnabled,
                onChange = { if (state.isPro) viewModel.setNightCurve(it) else onUpsell() },
            )
        }

        item {
            Column {
                SwitchRow(
                    title = stringResource(I18nR.string.settings_volume_cap),
                    subtitle = stringResource(I18nR.string.settings_volume_cap_hint),
                    checked = settings.volumeCapEnabled,
                    onChange = viewModel::setVolumeCapEnabled,
                )
                if (settings.volumeCapEnabled) {
                    LabelledSlider(
                        label = stringResource(I18nR.string.settings_volume_cap),
                        value = settings.volumeCap,
                        onValueChange = viewModel::setVolumeCap,
                        valueDescription = "${(settings.volumeCap * PERCENT).toInt()}%",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        item {
            SwitchRow(
                title = stringResource(I18nR.string.settings_analytics),
                subtitle = stringResource(I18nR.string.settings_analytics_hint),
                checked = settings.analyticsOptIn,
                onChange = viewModel::setAnalyticsOptIn,
            )
        }

        item {
            TextButton(onClick = viewModel::restorePurchases) {
                Text(stringResource(I18nR.string.settings_restore_purchases))
            }
        }

        item {
            // Required, and kept in plain sight rather than buried: the app
            // must never read as a medical device.
            Text(
                stringResource(I18nR.string.settings_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private const val PERCENT = 100
