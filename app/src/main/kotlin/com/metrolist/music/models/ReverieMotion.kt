package com.metrolist.music.models

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

const val REVERIE_RING_COUNT = 12

data class ReverieContour(val square: Float = 0f, val rosette: Float = 0f, val diamond: Float = 0f)

data class ReverieFigurePose(val x: Float, val y: Float, val radius: Float, val rotation: Float, val contour: ReverieContour)

private val reveriePoses = listOf(
    ReverieFigurePose(.50f, .58f, .59f, 0f, ReverieContour()),
    ReverieFigurePose(.55f, .59f, .59f, .08f, ReverieContour(square = 1f)),
    ReverieFigurePose(.58f, .56f, .59f, .16f, ReverieContour(rosette = 1f)),
    ReverieFigurePose(.53f, .60f, .59f, .10f, ReverieContour()),
    ReverieFigurePose(.47f, .58f, .59f, -.08f, ReverieContour(diamond = 1f)),
    ReverieFigurePose(.51f, .55f, .59f, 0f, ReverieContour()),
    ReverieFigurePose(.57f, .58f, .59f, -.12f, ReverieContour(rosette = 1f)),
    ReverieFigurePose(.54f, .60f, .59f, -.04f, ReverieContour(square = 1f)),
    ReverieFigurePose(.50f, .58f, .59f, 0f, ReverieContour()),
)

/** A chapter owns a fixed pose. Only the transition between two chapters interpolates it. */
fun reverieFigurePose(chapter: Int, variant: Int = 0): ReverieFigurePose =
    reveriePoses[Math.floorMod(chapter.coerceIn(reveriePoses.indices) + variant, reveriePoses.size)]

/** Radial blends keep a single closed contour and nested rings, including during interrupted seeks. */
fun reverieMorphRadius(angle: Float, squareWeight: Float, rosetteWeight: Float, diamondWeight: Float): Float {
    val square = (cos(angle).pow(4) + sin(angle).pow(4)).pow(-.25f)
    val rosette = 1f + .13f * cos(6 * angle)
    val diamond = (abs(cos(angle)).pow(1.4f) + abs(sin(angle)).pow(1.4f)).pow(-1f / 1.4f)
    return 1f + (square - 1f) * squareWeight + (rosette - 1f) * rosetteWeight + (diamond - 1f) * diamondWeight
}
