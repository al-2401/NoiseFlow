package app.noiseflow.data.store

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import app.noiseflow.data.model.AppState
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * JSON rather than protobuf.
 *
 * The architecture doc originally called for Proto DataStore. Once the schema
 * turned out to be a handful of value types, protoc codegen was buying nothing
 * and costing a build plugin, a second source language and a slower CI, so
 * this uses kotlinx.serialization instead. `ignoreUnknownKeys` plus explicit
 * defaults gives the same forward compatibility protobuf would have.
 */
object AppStateSerializer : Serializer<AppState> {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: AppState = AppState()

    override suspend fun readFrom(input: InputStream): AppState =
        try {
            json.decodeFromString(AppState.serializer(), input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            // Losing presets is bad; refusing to start is worse. Report and
            // fall back, and let the crash reporter tell us it happened.
            throw CorruptionException("Could not read saved state", e)
        }

    override suspend fun writeTo(t: AppState, output: OutputStream) {
        output.write(json.encodeToString(AppState.serializer(), t).encodeToByteArray())
    }
}
