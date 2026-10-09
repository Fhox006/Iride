package com.metrolist.music.ui.component

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.media.FaceDetector

/** Android's built-in detector needs an even width and RGB_565; no model or SDK is bundled. */
internal object ArtistPortraitDetector {
    private const val SampleSize = 320
    private const val MaxFaces = 5
    private val lock = Any()
    private var detector: FaceDetector? = null

    fun quality(bitmap: Bitmap): Float {
        val sample = Bitmap.createBitmap(SampleSize, SampleSize, Bitmap.Config.RGB_565)
        return try {
            Canvas(sample).drawBitmap(bitmap, null, Rect(0, 0, SampleSize, SampleSize), Paint(Paint.FILTER_BITMAP_FLAG))
            synchronized(lock) {
                val engine = detector ?: FaceDetector(SampleSize, SampleSize, MaxFaces).also { detector = it }
                val faces = arrayOfNulls<FaceDetector.Face>(MaxFaces)
                val count = engine.findFaces(sample, faces)
                (0 until count).maxOfOrNull { index ->
                    val face = faces[index] ?: return@maxOfOrNull 0f
                    val midpoint = PointF()
                    face.getMidPoint(midpoint)
                    artistPortraitQuality(face.confidence(), face.eyesDistance() / SampleSize,
                        midpoint.x / SampleSize, midpoint.y / SampleSize)
                } ?: 0f
            }
        } catch (_: LinkageError) {
            // Some system images omit the native detector. Keep the border/definition fallback usable.
            0f
        } catch (_: RuntimeException) {
            0f
        } finally {
            sample.recycle()
        }
    }
}
