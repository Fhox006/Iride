package com.metrolist.music.eq.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.metrolist.music.eq.data.ParametricEQ
import com.metrolist.music.eq.data.ParametricEQBand
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * Custom audio processor for ExoPlayer that applies parametric EQ using biquad filters
 * Uses ParametricEQ format from AutoEQ project
 */
@UnstableApi
@SuppressWarnings("Deprecated")
class CustomEqualizerAudioProcessor : AudioProcessor {

    private var sampleRate = 0
    private var channelCount = 0
    private var encoding = C.ENCODING_INVALID
    private var isActive = false
    private var equalizerEnabled = false

    private var inputBuffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    private var filters: List<BiquadFilter> = emptyList()
    private var filterSpecs: List<ParametricEQBand> = emptyList()
    private var preampGain: Double = 1.0
    private var pendingProfile: ParametricEQ? = null

    @Volatile
    private var dynamicBassStage: DynamicBassStage? = null

    @Volatile
    private var dynamicBassRequested = false

    private var pendingShelfDb: Double = 0.0
    private var pendingStrength: Double = 0.0

    companion object {
        private const val TAG = "CustomEqualizerAudioProcessor"
        private const val DYNAMIC_BASS_SHELF_HZ = 90.0
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }

    /**
     * Apply an EQ profile
     */
    @Synchronized
    fun applyProfile(parametricEQ: ParametricEQ) {
        if (sampleRate == 0) {
            Timber.tag(TAG)
                .d("Audio processor not configured yet. Storing profile as pending with ${parametricEQ.bands.size} bands")
            pendingProfile = parametricEQ
            return
        }

        preampGain = 10.0.pow(parametricEQ.preamp / 20.0)

        createFilters(parametricEQ.bands)
        ensureDynamicBassStage()
        equalizerEnabled = true

        Timber.tag(TAG)
            .d("Applied EQ profile with ${filters.size} bands and ${parametricEQ.preamp} dB preamp")
    }

    /**
     * Disable the equalizer
     */
    @Synchronized
    fun disable() {
        equalizerEnabled = false
        filters = emptyList()
        filterSpecs = emptyList()
        preampGain = 1.0
        pendingProfile = null
        dynamicBassStage = null
        dynamicBassRequested = false
        pendingShelfDb = 0.0
        pendingStrength = 0.0
        Timber.tag(TAG).d("Equalizer disabled")
    }

    /**
     * Configure the dynamic low-shelf stage (advanced "Duration" transient shaper).
     * When [strength] > 0 the shelf boost is applied dynamically by this stage instead
     * of a static band, so it must not be added to [applyProfile] bands as well.
     */
    @Synchronized
    fun setDynamicBass(shelfGainDb: Double, strength: Double) {
        dynamicBassRequested = strength > 0.0 && shelfGainDb != 0.0
        pendingShelfDb = if (dynamicBassRequested) shelfGainDb else 0.0
        pendingStrength = strength.coerceIn(0.0, 1.0)

        val stage = dynamicBassStage
        if (dynamicBassRequested && stage != null) {
            stage.shelfGainDb = pendingShelfDb
            stage.strength = pendingStrength
        } else if (!dynamicBassRequested) {
            dynamicBassStage = null
        }
        Timber.tag(TAG)
            .d("Dynamic bass requested=$dynamicBassRequested shelf=${pendingShelfDb}dB strength=${pendingStrength}")
    }

    private fun ensureDynamicBassStage() {
        if (!dynamicBassRequested || sampleRate == 0) return
        var stage = dynamicBassStage
        if (stage == null) {
            stage = DynamicBassStage(sampleRate, DYNAMIC_BASS_SHELF_HZ).apply {
                shelfGainDb = pendingShelfDb
                strength = pendingStrength
            }
            dynamicBassStage = stage
        } else {
            stage.shelfGainDb = pendingShelfDb
            stage.strength = pendingStrength
        }
    }

    /**
     * Check if equalizer is enabled
     */
    fun isEnabled(): Boolean = equalizerEnabled

    /**
     * Create biquad filters from ParametricEQ bands
     * Only creates filters for enabled bands below Nyquist frequency
     * Supports PK (peaking), LSC (low-shelf), and HSC (high-shelf) filter types
     *
     * When the band topology is unchanged (same count and filter types) the existing
     * filters are retuned in place, preserving their delay-line state: applying tweaks
     * while playing stays click-free.
     */
    private fun createFilters(bands: List<ParametricEQBand>) {
        if (sampleRate == 0) {
            Timber.tag(TAG).w("Cannot create filters: sample rate not set")
            return
        }

        val active = bands.filter { it.enabled && it.frequency < sampleRate / 2.0 }
        val current = filters

        val topologyMatches = current.size == active.size &&
                filterSpecs.size == active.size &&
                filterSpecs.indices.all { i ->
                    filterSpecs[i].filterType == active[i].filterType
                }

        if (topologyMatches && current.isNotEmpty()) {
            current.forEachIndexed { i, filter ->
                filter.recalculate(
                    gainOverride = active[i].gain,
                    frequencyOverride = active[i].frequency,
                    qOverride = active[i].q
                )
            }
        } else {
            filters = active.map { band ->
                BiquadFilter(
                    sampleRate = sampleRate,
                    frequency = band.frequency,
                    gain = band.gain,
                    q = band.q,
                    filterType = band.filterType
                )
            }
        }
        filterSpecs = active

        Timber.tag(TAG)
            .d("Applied ${filters.size} biquad filters from ${bands.size} bands (PK/LSC/HSC)")
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding

        Timber.tag(TAG)
            .d("Configured: sampleRate=$sampleRate, channels=$channelCount, encoding=$encoding")

        pendingProfile?.let { profile ->
            preampGain = 10.0.pow(profile.preamp / 20.0)
            createFilters(profile.bands)
            equalizerEnabled = true
            pendingProfile = null
            Timber.tag(TAG)
                .d("Applied pending profile with ${filters.size} bands and ${profile.preamp} dB preamp")
        }

        ensureDynamicBassStage()

        if (encoding != C.ENCODING_PCM_16BIT || channelCount > 2) {
            val exception = AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            throw exception
        }

        isActive = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!equalizerEnabled || filters.isEmpty()) {
            val remaining = inputBuffer.remaining()
            if (remaining == 0) return

            if (outputBuffer.capacity() < remaining) {
                outputBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
            } else {
                outputBuffer.clear()
            }
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val inputSize = inputBuffer.remaining()
        if (inputSize == 0) {
            return
        }

        if (outputBuffer === EMPTY_BUFFER || outputBuffer === inputBuffer) {
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        } else if (outputBuffer.capacity() < inputSize) {
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }

        when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                processAudioBuffer16Bit(inputBuffer, outputBuffer)
            }
            else -> {
                outputBuffer.put(inputBuffer)
            }
        }

        outputBuffer.flip()
    }

    /**
     * Process 16-bit PCM audio through all biquad filters
     */
    private fun processAudioBuffer16Bit(input: ByteBuffer, output: ByteBuffer) {

        val sampleCount = input.remaining() / 2

        repeat(sampleCount / channelCount) {
            when (channelCount) {
                1 -> {
                    val sample = input.getShort().toDouble() / 32768.0
                    var processed = sample

                    dynamicBassStage?.let { stage ->
                        processed = stage.process(processed, processed).first
                    }

                    for (filter in filters) {
                        processed = filter.processSample(processed)
                    }

                    processed *= preampGain

                    val outputSample = (processed * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    output.putShort(outputSample)
                }
                2 -> {
                    val leftSample = input.getShort().toDouble() / 32768.0
                    val rightSample = input.getShort().toDouble() / 32768.0

                    var processedLeft = leftSample
                    var processedRight = rightSample

                    dynamicBassStage?.let { stage ->
                        val (shapedLeft, shapedRight) = stage.process(processedLeft, processedRight)
                        processedLeft = shapedLeft
                        processedRight = shapedRight
                    }

                    for (filter in filters) {
                        val (left, right) = filter.processStereo(processedLeft, processedRight)
                        processedLeft = left
                        processedRight = right
                    }

                    processedLeft *= preampGain
                    processedRight *= preampGain

                    val outputLeft = (processedLeft * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                    val outputRight = (processedRight * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()

                    output.putShort(outputLeft)
                    output.putShort(outputRight)
                }
                else -> {
                    repeat(channelCount) {
                        output.putShort(input.getShort())
                    }
                }
            }
        }
    }

    override fun getOutput(): ByteBuffer {
        val buffer = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return buffer
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer.remaining() == 0
    }

    @Deprecated("Deprecated in Java")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false

        filters.forEach { it.reset() }
        dynamicBassStage?.reset()
    }

    override fun reset() {
        @Suppress("DEPRECATION")
        flush()
        inputBuffer = EMPTY_BUFFER
        sampleRate = 0
        channelCount = 0
        encoding = C.ENCODING_INVALID
        isActive = false
        filters.forEach { it.reset() }
        dynamicBassStage = null
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }
}
