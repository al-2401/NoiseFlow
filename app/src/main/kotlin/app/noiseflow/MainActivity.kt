package app.noiseflow

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import app.noiseflow.data.store.AppStateRepository
import app.noiseflow.designsystem.theme.NoiseFlowTheme
import app.noiseflow.designsystem.theme.Skins
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: AppStateRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val skinFlow = repository.settings
            .map { Skins.byId(it.skinId) }
            .stateIn(lifecycleScope, SharingStarted.Eagerly, Skins.Midnight)

        setContent {
            val skin by skinFlow.collectAsState()
            NoiseFlowTheme(skin = skin) {
                NoiseFlowNavHost(deepLink = intent?.dataString)
            }
        }
    }

    /** singleTask, so a shared preset link opened while running lands here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
