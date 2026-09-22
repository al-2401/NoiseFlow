package app.noiseflow.data.store

import androidx.datastore.core.DataStore
import app.noiseflow.data.model.AppSettings
import app.noiseflow.data.model.AppState
import app.noiseflow.data.model.FactoryPresets
import app.noiseflow.data.model.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything the app persists, behind one interface.
 *
 * All reads survive I/O errors by falling back to defaults: this app has to
 * start and make noise even if its own storage is unreadable, because the user
 * is standing in a dark bedroom and does not care why.
 */
@Singleton
class AppStateRepository @Inject constructor(
    private val store: DataStore<AppState>,
) {
    val state: Flow<AppState> = store.data.catch { cause ->
        if (cause is IOException) emit(AppState()) else throw cause
    }

    val settings: Flow<AppSettings> = state.map { it.settings }

    /** Factory presets first, then the user's own, newest last. */
    val presets: Flow<List<Preset>> = state.map { FactoryPresets.all + it.userPresets }

    val lastMix: Flow<Preset?> = state.map { it.lastMix }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        store.updateData { it.copy(settings = transform(it.settings)) }
    }

    suspend fun savePreset(preset: Preset) {
        store.updateData { current ->
            val without = current.userPresets.filterNot { it.id == preset.id }
            current.copy(userPresets = without + preset)
        }
    }

    suspend fun deletePreset(id: String) {
        store.updateData { current ->
            current.copy(userPresets = current.userPresets.filterNot { it.id == id })
        }
    }

    suspend fun renamePreset(id: String, name: String) {
        store.updateData { current ->
            current.copy(
                userPresets = current.userPresets.map {
                    if (it.id == id) it.copy(name = name) else it
                },
            )
        }
    }

    suspend fun setLastMix(preset: Preset?) {
        store.updateData { it.copy(lastMix = preset) }
    }
}
