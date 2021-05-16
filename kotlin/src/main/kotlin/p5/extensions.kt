package p5

import processing.core.PVector

fun PVector.distSq(v: PVector): Float = this.dist(v).let { it * it }

