package app.noiseflow

import android.app.Application
import app.noiseflow.data.store.AppStateRepository
import app.noiseflow.i18n.AppLocale
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NoiseFlowApplication : Application() {

    @Inject
    lateinit var repository: AppStateRepository

    @Inject
    lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            val settings = repository.settings.first()
            AppLocale.set(settings.localeTag)
            configureCrashReporting(settings.analyticsOptIn)
        }
    }

    /**
     * Crash reports always; usage analytics only if asked for.
     *
     * A crash reporter is not optional -- we cannot fix what we cannot see --
     * but it is stripped of anything identifying, and it never carries what
     * someone is listening to. Everything beyond that is opt-in, because this
     * app lives in a bedroom and tracking by default would contradict the one
     * promise the product is built on.
     */
    private fun configureCrashReporting(analyticsOptIn: Boolean) {
        SentryAndroid.init(this) { options ->
            options.isEnableAutoSessionTracking = analyticsOptIn
            options.isSendDefaultPii = false
            options.sampleRate = 1.0
            options.tracesSampleRate = if (analyticsOptIn) TRACES_SAMPLE_RATE else 0.0
            options.beforeSend = io.sentry.SentryOptions.BeforeSendCallback { event, _ ->
                event.user = null
                event
            }
        }
    }

    private companion object {
        const val TRACES_SAMPLE_RATE = 0.05
    }
}
