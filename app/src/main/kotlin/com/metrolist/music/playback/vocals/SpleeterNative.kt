package com.metrolist.music.playback.vocals

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import java.io.Closeable
import java.io.File

/** Maps the source-separation C API from the verified, optional sherpa-onnx runtime. */
internal class SpleeterNative(modelDirectory: File, runtimeDirectory: File, threads: Int = 2) : Closeable {
    interface Api : Library {
        fun SherpaOnnxCreateOfflineSourceSeparation(config: Config): Pointer?
        fun SherpaOnnxDestroyOfflineSourceSeparation(engine: Pointer)
        fun SherpaOnnxOfflineSourceSeparationProcess(
            engine: Pointer, channels: Pointer, channelCount: Int, frames: Int, sampleRate: Int,
        ): Pointer?
        fun SherpaOnnxDestroySourceSeparationOutput(output: Pointer)
    }

    @Structure.FieldOrder("vocals", "accompaniment")
    class Spleeter : Structure() {
        @JvmField var vocals: String = ""
        @JvmField var accompaniment: String = ""
    }

    @Structure.FieldOrder("model")
    class Uvr : Structure() { @JvmField var model: String = "" }

    @Structure.FieldOrder("spleeter", "uvr", "num_threads", "debug", "provider")
    class Model : Structure() {
        @JvmField var spleeter = Spleeter()
        @JvmField var uvr = Uvr()
        @JvmField var num_threads: Int = 2
        @JvmField var debug: Int = 0
        @JvmField var provider: String = "cpu"
    }

    @Structure.FieldOrder("model")
    class Config : Structure() { @JvmField var model = Model() }

    @Structure.FieldOrder("samples", "num_channels", "n")
    class Stem(pointer: Pointer) : Structure(pointer) {
        @JvmField var samples: Pointer? = null
        @JvmField var num_channels: Int = 0
        @JvmField var n: Int = 0
        init { read() }
    }

    @Structure.FieldOrder("stems", "num_stems", "sample_rate")
    class Output(pointer: Pointer) : Structure(pointer) {
        @JvmField var stems: Pointer? = null
        @JvmField var num_stems: Int = 0
        @JvmField var sample_rate: Int = 0
        init { read() }
    }

    private val api: Api = run {
        val onnx = File(runtimeDirectory, System.mapLibraryName("onnxruntime")).absolutePath
        val sherpa = File(runtimeDirectory, System.mapLibraryName("sherpa-onnx-c-api")).absolutePath
        System.load(onnx)
        System.load(sherpa)
        Native.load(sherpa, Api::class.java)
    }
    private val engine = api.SherpaOnnxCreateOfflineSourceSeparation(Config().apply {
        model.num_threads = threads
        model.spleeter.vocals = File(modelDirectory, "vocals.fp16.onnx").absolutePath
        model.spleeter.accompaniment = File(modelDirectory, "accompaniment.fp16.onnx").absolutePath
    }) ?: error("Could not initialize Spleeter")

    fun accompaniment(left: FloatArray, right: FloatArray): Array<FloatArray> {
        require(left.isNotEmpty() && left.size == right.size)
        // The inverse STFT returns whole 1024-sample hops. Pad before inference and trim after
        // it, so short tails are safe and no samples disappear from the lyrics timebase.
        val paddedFrames = maxOf(4096, (left.size + 1023) / 1024 * 1024)
        Memory(paddedFrames * 4L).use { leftMemory ->
            Memory(paddedFrames * 4L).use { rightMemory ->
                Memory(Native.POINTER_SIZE * 2L).use { channels ->
                    leftMemory.clear()
                    rightMemory.clear()
                    leftMemory.write(0, left, 0, left.size)
                    rightMemory.write(0, right, 0, right.size)
                    channels.setPointer(0, leftMemory)
                    channels.setPointer(Native.POINTER_SIZE.toLong(), rightMemory)
                    val pointer = api.SherpaOnnxOfflineSourceSeparationProcess(
                        engine, channels, 2, paddedFrames, 44100,
                    ) ?: error("Spleeter returned no audio")
                    try {
                        val output = Output(pointer)
                        check(output.num_stems == 2 && output.sample_rate == 44100)
                        val vocals = Stem(checkNotNull(output.stems))
                        check(vocals.num_channels == 2 && vocals.n >= left.size)
                        val samples = checkNotNull(vocals.samples)
                        return Array(2) { channel ->
                            val voice = samples.getPointer(channel * Native.POINTER_SIZE.toLong())
                                .getFloatArray(0, left.size)
                            val original = if (channel == 0) left else right
                            // Spleeter predicts only the lower frequency bins. Subtracting its
                            // vocals from the original preserves the instrumental high end.
                            FloatArray(left.size) { original[it] - voice[it] }
                        }
                    } finally {
                        api.SherpaOnnxDestroySourceSeparationOutput(pointer)
                    }
                }
            }
        }
    }

    override fun close() { api.SherpaOnnxDestroyOfflineSourceSeparation(engine) }
}
