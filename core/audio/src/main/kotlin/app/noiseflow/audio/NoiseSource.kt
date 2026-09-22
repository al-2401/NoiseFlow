package app.noiseflow.audio

/**
 * A mono, block-based signal source. Sources are stateful and are never shared
 * between threads: the engine owns them and only the render thread touches
 * them once they are installed.
 */
interface NoiseSource {
    /** Fills the first [frames] entries of [out]. Must not allocate. */
    fun render(out: FloatArray, frames: Int)

    fun reset()
}
