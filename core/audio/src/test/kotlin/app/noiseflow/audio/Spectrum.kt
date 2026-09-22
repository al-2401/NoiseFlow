package app.noiseflow.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Test-only spectral analysis.
 *
 * The product claim is "this generator has slope X dB/octave". That is a
 * measurable statement, so it gets measured rather than listened to: Welch
 * periodogram, third-octave band averaging, least-squares fit of level against
 * log frequency.
 */
object Spectrum {

    private const val SEGMENT = 8192
    private const val HOP = SEGMENT / 2

    /** Mean power per bin, index i corresponding to i * sampleRate / SEGMENT Hz. */
    fun welchPsd(signal: FloatArray, segments: Int = 128): DoubleArray {
        val window = DoubleArray(SEGMENT) { 0.5 - 0.5 * cos(2.0 * PI * it / (SEGMENT - 1)) }
        val psd = DoubleArray(SEGMENT / 2)
        var used = 0
        var start = 0
        while (used < segments && start + SEGMENT <= signal.size) {
            val re = DoubleArray(SEGMENT)
            val im = DoubleArray(SEGMENT)
            for (i in 0 until SEGMENT) re[i] = signal[start + i].toDouble() * window[i]
            fft(re, im)
            for (k in 0 until SEGMENT / 2) psd[k] += re[k] * re[k] + im[k] * im[k]
            used++
            start += HOP
        }
        require(used > 0) { "signal too short: ${signal.size} samples" }
        for (k in psd.indices) psd[k] /= used
        return psd
    }

    /**
     * Least-squares slope of the spectrum in dB per octave across [lowHz, highHz].
     *
     * Averages into third-octave bands first. Raw periodogram bins of a noise
     * signal scatter by several dB, and fitting through that scatter needs far
     * more data than fitting through ~20 band averages.
     */
    fun slopeDbPerOctave(
        signal: FloatArray,
        sampleRate: Int,
        lowHz: Double = 100.0,
        highHz: Double = 10_000.0,
        segments: Int = 128,
    ): Double {
        val psd = welchPsd(signal, segments)
        val binHz = sampleRate.toDouble() / SEGMENT

        val xs = ArrayList<Double>()
        val ys = ArrayList<Double>()
        val ratio = 2.0.pow(1.0 / 3.0)
        var centre = lowHz
        while (centre <= highHz) {
            val lo = centre / sqrt(ratio)
            val hi = centre * sqrt(ratio)
            var sum = 0.0
            var count = 0
            var k = (lo / binHz).toInt().coerceAtLeast(1)
            while (k * binHz <= hi && k < psd.size) {
                sum += psd[k]
                count++
                k++
            }
            if (count > 0 && sum > 0) {
                xs += log2(centre)
                ys += 10.0 * log10(sum / count)
            }
            centre *= ratio
        }
        require(xs.size >= 4) { "not enough bands to fit a slope" }

        val n = xs.size
        val mx = xs.average()
        val my = ys.average()
        var num = 0.0
        var den = 0.0
        for (i in 0 until n) {
            num += (xs[i] - mx) * (ys[i] - my)
            den += (xs[i] - mx) * (xs[i] - mx)
        }
        return num / den
    }

    /** Mean power per bin in a third-octave band, in dB. */
    fun bandLevelDb(signal: FloatArray, sampleRate: Int, centreHz: Double, segments: Int = 128): Double {
        val psd = welchPsd(signal, segments)
        val binHz = sampleRate.toDouble() / SEGMENT
        val ratio = 2.0.pow(1.0 / 6.0)
        val lo = centreHz / ratio
        val hi = centreHz * ratio
        var sum = 0.0
        var count = 0
        var k = (lo / binHz).toInt().coerceAtLeast(1)
        while (k * binHz <= hi && k < psd.size) {
            sum += psd[k]
            count++
            k++
        }
        require(count > 0) { "no bins around $centreHz Hz" }
        return 10.0 * log10(sum / count)
    }

    /** Frequency of the loudest third-octave band, used to locate green noise. */
    fun peakBandHz(signal: FloatArray, sampleRate: Int, segments: Int = 128): Double {
        var best = 0.0
        var bestLevel = Double.NEGATIVE_INFINITY
        val ratio = 2.0.pow(1.0 / 3.0)
        var centre = 40.0
        while (centre < sampleRate / 2.5) {
            val level = bandLevelDb(signal, sampleRate, centre, segments)
            if (level > bestLevel) {
                bestLevel = level
                best = centre
            }
            centre *= ratio
        }
        return best
    }

    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        require(n and (n - 1) == 0) { "FFT size must be a power of two" }

        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                re[i] = re[j].also { re[j] = re[i] }
                im[i] = im[j].also { im[j] = im[i] }
            }
        }

        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wRe = cos(angle)
            val wIm = kotlin.math.sin(angle)
            var i = 0
            while (i < n) {
                var curRe = 1.0
                var curIm = 0.0
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val vRe = re[i + k + len / 2] * curRe - im[i + k + len / 2] * curIm
                    val vIm = re[i + k + len / 2] * curIm + im[i + k + len / 2] * curRe
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[i + k + len / 2] = uRe - vRe
                    im[i + k + len / 2] = uIm - vIm
                    val nextRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nextRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}

/** Renders a mono source into a fresh array. */
fun NoiseSource.renderSeconds(seconds: Double, sampleRate: Int): FloatArray {
    val total = (seconds * sampleRate).toInt()
    val out = FloatArray(total)
    val block = FloatArray(4096)
    var written = 0
    while (written < total) {
        val n = minOf(block.size, total - written)
        render(block, n)
        System.arraycopy(block, 0, out, written, n)
        written += n
    }
    return out
}

fun rms(signal: FloatArray): Double {
    var sum = 0.0
    for (v in signal) sum += v.toDouble() * v.toDouble()
    return sqrt(sum / signal.size)
}
