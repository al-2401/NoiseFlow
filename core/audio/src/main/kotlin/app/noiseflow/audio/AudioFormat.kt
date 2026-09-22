package app.noiseflow.audio

/** Everything in the engine is float PCM in [-1, 1]. */
object AudioFormat {
    const val DEFAULT_SAMPLE_RATE = 44_100

    /**
     * Render block size. Deliberately large: latency is irrelevant for a sleep
     * app, and fewer, bigger blocks mean fewer CPU wakeups, which is directly
     * the battery budget from docs/01-concept.md.
     */
    const val DEFAULT_BLOCK_FRAMES = 2048

    const val CHANNELS = 2

    /**
     * Target RMS for a single calibrated generator, about -12 dBFS. Leaves
     * headroom for eight layers plus modulation before the limiter engages.
     */
    const val GENERATOR_TARGET_RMS = 0.25f
}
