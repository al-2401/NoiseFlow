package app.noiseflow.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import app.noiseflow.data.model.AppState
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun appStateStore(
        @ApplicationContext context: Context,
        scope: CoroutineScope,
    ): DataStore<AppState> = DataStoreFactory.create(
        serializer = AppStateSerializer,
        scope = scope,
        produceFile = { context.dataStoreFile("noiseflow_state.json") },
    )

    private fun Context.dataStoreFile(name: String) = java.io.File(filesDir, "datastore/$name")
}
