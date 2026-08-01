/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural "needle drop" sound played when an album's PLAY button is pressed: a damped low thunk
 * (the needle landing) under a handful of short crackle-pops that decay with the clip — a record
 * settling, not a burst of static. No shipped audio asset exists for this in the app, and
 * synthesizing ~25k PCM samples on a background thread is less to maintain than bundling +
 * licensing a sample.
 */
object TurntableSfx {
    private const val SampleRate = 44100
    private const val DurationMs = 550
    private const val ThunkMs = 80
    private const val PopCount = 10
    private const val PopMs = 4

    // ponytail: synthesized approximation, not a mastered sample — swap in a real recorded
    // needle-drop .mp3/.ogg under res/raw if the procedural texture ever isn't convincing enough.
    fun play() {
        thread(isDaemon = true) {
            val frameCount = SampleRate * DurationMs / 1000
            val thunkFrames = SampleRate * ThunkMs / 1000
            val popFrames = SampleRate * PopMs / 1000
            val random = Random(System.nanoTime())
            val buffer = ShortArray(frameCount)

            for (i in 0 until frameCount) {
                val t = i.toDouble() / SampleRate
                var sample = 0.0

                if (i < thunkFrames) {
                    sample += sin(2 * PI * 90.0 * t) * exp(-t * 40.0) * 0.9
                }

                // Quiet surface hiss bed, well under the pops, decaying with the clip.
                sample += (random.nextDouble() * 2 - 1) * 0.03 * exp(-t * 6.0)

                buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }

            // Scatter discrete crackle-pops on top — irregular spacing, louder near the start,
            // fading out with the clip, the way a record actually crackles.
            var cursor = thunkFrames / 2
            repeat(PopCount) {
                cursor += popFrames + random.nextInt(popFrames * 6)
                if (cursor + popFrames >= frameCount) return@repeat
                val amp = (0.25 + random.nextDouble() * 0.35) * exp(-(cursor.toDouble() / SampleRate) * 6.0)
                for (j in 0 until popFrames) {
                    val env = exp(-(j.toDouble() / popFrames) * 10.0)
                    val idx = cursor + j
                    val current = buffer[idx] / Short.MAX_VALUE.toDouble()
                    val added = (current + (random.nextDouble() * 2 - 1) * amp * env).coerceIn(-1.0, 1.0)
                    buffer[idx] = (added * Short.MAX_VALUE).toInt().toShort()
                }
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(buffer.size * 2)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep(DurationMs.toLong() + 150)
            audioTrack.stop()
            audioTrack.release()
        }
    }
}
