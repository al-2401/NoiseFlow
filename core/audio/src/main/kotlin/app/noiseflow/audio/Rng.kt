package app.noiseflow.audio

/**
 * xorshift128+ .
 *
 * Not [java.util.Random]: that one is synchronized and boxes nothing but still
 * costs a CAS per call, which is unacceptable in a render loop that produces
 * millions of samples per second. This generator is allocation-free and
 * deterministic for a given seed, which is what the spectral tests rely on.
 */
class Rng(seed: Long) {
    private var s0: Long
    private var s1: Long

    init {
        // SplitMix64 to spread a possibly poor seed across the whole state.
        var z = seed
        s0 = splitMix(z.also { z = it + GOLDEN })
        s1 = splitMix(z + GOLDEN)
        if (s0 == 0L && s1 == 0L) {
            s0 = 0x9E3779B97F4A7C15uL.toLong()
            s1 = 0xBF58476D1CE4E5B9uL.toLong()
        }
    }

    fun nextLong(): Long {
        var x = s0
        val y = s1
        s0 = y
        x = x xor (x shl 23)
        s1 = x xor y xor (x ushr 17) xor (y ushr 26)
        return s1 + y
    }

    /** Uniform in [-1, 1). RMS is 1/sqrt(3); callers normalise downstream. */
    fun nextBipolar(): Float {
        // Top 24 bits give a clean float mantissa without bias.
        val bits = (nextLong() ushr 40).toInt() // 24 bits, 0 .. 2^24-1
        return bits * TWO_OVER_2_24 - 1f
    }

    private fun splitMix(input: Long): Long {
        var z = input
        z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9uL.toLong()
        z = (z xor (z ushr 27)) * 0x94D049BB133111EBuL.toLong()
        return z xor (z ushr 31)
    }

    private companion object {
        const val GOLDEN = -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        const val TWO_OVER_2_24 = 2f / 16_777_216f
    }
}
