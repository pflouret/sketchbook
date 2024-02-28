package growth

import processing.core.PVector

class TVertex(x: Float, y: Float) : PVector(x, y) {
    constructor(v: PVector) : this(v.x, v.y)

    var prev: TVertex? = null
    var next: TVertex? = null

    fun setBefore(v: TVertex) {
        if (next != null) {
            (next as TVertex).prev = v
        }
        v.next = next
        v.prev = this
        next = v
    }
}