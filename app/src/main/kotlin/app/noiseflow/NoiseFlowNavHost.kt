package app.noiseflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.noiseflow.feature.mixer.MixerScreen
import app.noiseflow.feature.paywall.PaywallScreen
import app.noiseflow.feature.player.NightScreen
import app.noiseflow.feature.player.PlayerScreen
import app.noiseflow.feature.presets.PresetsScreen
import app.noiseflow.feature.presets.PresetsViewModel
import app.noiseflow.feature.settings.SettingsScreen

private object Routes {
    const val PLAYER = "player"
    const val NIGHT = "night"
    const val MIXER = "mixer"
    const val PRESETS = "presets"
    const val SETTINGS = "settings"
    const val PAYWALL = "paywall"
}

@Composable
fun NoiseFlowNavHost(deepLink: String?) {
    val controller = rememberNavController()
    val presetsViewModel: PresetsViewModel = hiltViewModel()

    LaunchedEffect(deepLink) {
        deepLink?.let(presetsViewModel::import)
    }

    NavHost(navController = controller, startDestination = Routes.PLAYER) {
        composable(Routes.PLAYER) {
            PlayerScreen(
                onOpenMixer = { controller.navigate(Routes.MIXER) },
                onOpenNightScreen = { controller.navigate(Routes.NIGHT) },
            )
        }
        composable(Routes.NIGHT) {
            NightScreen(onExit = { controller.popBackStack() })
        }
        composable(Routes.MIXER) {
            MixerScreen(onUpsell = { controller.navigate(Routes.PAYWALL) })
        }
        composable(Routes.PRESETS) {
            PresetsScreen()
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onUpsell = { controller.navigate(Routes.PAYWALL) })
        }
        composable(Routes.PAYWALL) {
            PaywallScreen(onClose = { controller.popBackStack() })
        }
    }
}
