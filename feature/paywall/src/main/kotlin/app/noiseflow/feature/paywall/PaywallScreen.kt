package app.noiseflow.feature.paywall

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import app.noiseflow.billing.ProductId
import app.noiseflow.i18n.R as I18nR

@Composable
fun PaywallScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(I18nR.string.paywall_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(I18nR.string.paywall_subtitle), style = MaterialTheme.typography.bodyMedium)

        if (state.isPro) {
            Text(stringResource(I18nR.string.paywall_owned), style = MaterialTheme.typography.titleMedium)
        }

        // The free column is shown, not hidden. Someone who decides the free
        // tier is enough is still a user who leaves five stars.
        ComparisonTable()

        state.products.forEach { product ->
            PriceCard(
                label = stringResource(product.id.labelRes()),
                price = product.formattedPrice,
                selected = product.id == state.selected,
                enabled = product.available && !state.isPro,
                onClick = { viewModel.select(product.id) },
            )
        }

        Button(
            onClick = { activity?.let(viewModel::purchase) },
            enabled = !state.isWorking && !state.isPro && state.products.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(I18nR.string.paywall_cta))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = viewModel::restore) {
                Text(stringResource(I18nR.string.paywall_restore))
            }
            TextButton(onClick = onClose) {
                Text(stringResource(I18nR.string.action_close))
            }
        }

        state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun ComparisonTable() {
    val rows = listOf(
        I18nR.string.paywall_feature_generators to true,
        I18nR.string.paywall_feature_skins to true,
        I18nR.string.paywall_feature_ads to true,
        I18nR.string.paywall_feature_layers to false,
        I18nR.string.paywall_feature_movement to false,
        I18nR.string.paywall_feature_presets to false,
        I18nR.string.paywall_feature_widgets to false,
    )
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { (res, isFree) ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(res), Modifier.weight(1f))
                    Text(
                        stringResource(
                            if (isFree) I18nR.string.paywall_free else I18nR.string.paywall_pro,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceCard(
    label: String,
    price: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClick, enabled = enabled) {
                Text(if (price.isEmpty()) "—" else price)
            }
        }
        if (selected) {
            Text(
                stringResource(I18nR.string.paywall_cta),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
            )
        }
    }
}

private fun ProductId.labelRes(): Int = when (this) {
    ProductId.MONTHLY -> I18nR.string.paywall_monthly
    ProductId.YEARLY -> I18nR.string.paywall_yearly
    ProductId.LIFETIME -> I18nR.string.paywall_lifetime
}
