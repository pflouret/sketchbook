package growth

import com.sun.glass.ui.Size
import p5.ProcessingAppK
import p5.distSq
import processing.core.PVector
import java.util.*

class DifferentialGrowth : ProcessingAppK() {
    private val drawPoints = true
    private val drawShape = true
    private val padding = 15
    private val repulsionRadius = 25f
    private val nearDistance = 6f
    private val splitDistance = 7f
    private val splitRandomness = 0.7f
    private val attractionForce = 0.005f

    private var first: Node? = null

    companion object {
        val SIZE = Size(800, 800)
        fun run() = DifferentialGrowth().runSketch()
    }

    override fun settings() {
        size(SIZE.width, SIZE.height, FX2D)
    }

    override fun setup() {
        super.setup()
        reset()
        clear()
        //noLoop();
//        fill(0)
        stroke(0)
        strokeWeight(2f)
    }

    override fun reset() {
        initialCircle()
    }

    private fun initialLine() {
        first = Node(w2 - 130, h2 - 130)
        first!!.next = Node(w2 + 130, h2 + 130)
    }

    private fun initialTube() {
        first = Node(w2 - 150, h2 - 130)
        first!!.linkNext(Node(w2 + 130, h2 + 130))
        first!!.next!!.linkNext(Node(w2 + 150, h2 + 130))
        first!!.next!!.next!!.linkNext(Node(w2 - 130, h2 - 130))
    }

    private fun initialCircle() {
        val n = 10
        val r = 20
        first = Node(w2 + r + random(-.5f, .5f), h2 + random(-.5f, .5f))
        var v = first!!
        for (i in 1 until n) {
            val angle = i * 2 * PI / n
            v.linkNext(
                Node(
                    w2 + r * cos(angle) + random(-.5f, .5f),
                    h2 + r * sin(angle) + random(-.5f, .5f)
                )
            )
            v = v.next!!
        }
        first!!.prev = v
        v.next = first
    }

    private fun update() {
        forEachNode { it.age++ }
        reject()
        splitUniformly()
        attract()
    }

    private fun reject() {
        forEachNode { a ->
            forEachNode { b ->
                val d2: Float = a.distSq(b)
                if (b !== a &&
                    b !== a.prev &&
                    b !== a.next &&
                    isWithinBounds(a) &&
                    d2 < repulsionRadius * repulsionRadius
                ) {
                    a.set(lerp(a.x, b.x, -1 / d2), lerp(a.y, b.y, -1 / d2))
                }
            }
        }
    }

    private fun isWithinBounds(n: Node) =
        n.x > padding && n.x < width - padding && n.y > padding && n.y < height - padding

    private fun attract() {
        forEachNode { a ->
            if (!isWithinBounds(a)) {
                return@forEachNode
            }
            if (a.dist(a.prev) > nearDistance) {
                a.set(
                    lerp(a.x, a.prev!!.x, attractionForce),
                    lerp(a.y, a.prev!!.y, attractionForce)
                )
            }
            if (a.dist(a.next) > nearDistance) {
                a.set(
                    lerp(a.x, a.next!!.x, attractionForce),
                    lerp(a.y, a.next!!.y, attractionForce)
                )
            }
        }
    }

    private fun splitUniformly() {
        forEachNode { n ->
            if (n.dist(n.next) > splitDistance) {
                n.linkNext(
                    Node(
                        n.copy().add(n.next).mult(0.5f)
                            .add(random(splitRandomness), random(splitRandomness))
                    )
                )
            }
        }
    }

    private fun splitByCurvature() {
        for (i in 0..99) {
            val nodes: MutableList<Node> = ArrayList()
            forEachNode(nodes::add)
            nodes.sortBy {
                abs(PVector.angleBetween(it.copy().sub(it.prev), it.copy().sub(it.next)))
            }
            val toSplit = nodes.subList(0, min(10, nodes.size)) //nodes.size()/100);

            forEachNode { a ->
                if (!isWithinBounds(a)) {
                    return@forEachNode
                }
                if (a.dist(a.next) > splitDistance && (toSplit.contains(a) || random() < 0.00005f)) {
                    a.linkNext(
                        Node(
                            a.copy()
                                .add(a.next)
                                .mult(0.5f)
                                .add(random(splitRandomness), random(splitRandomness))
                        )
                    )
                }
            }
        }
    }

    override fun drawInternal() {
        clear()
        drawShape()
        update()
    }

    private fun drawPoint(n: Node) {
        if (!drawPoints) {
            return
        }
        pushStyle()
        strokeWeight(5f + 5f/n.age)
        point(n)
        popStyle()
    }

    private fun drawLine() {
        beginShape()
        forEachNode { n ->
            drawPoint(n)
            vertex(n)
        }
        endShape()
    }

    private fun drawShape() {
        if (drawShape) {
            beginShape()
            curveVertex(first!!.prev!!)
        }
        forEachNode { n ->
            drawPoint(n)
            if (drawShape) {
                curveVertex(n)
            }
        }
        if (drawShape) {
            curveVertex(first!!)
            curveVertex(first!!.next!!)
            endShape()
        }
    }

    private fun forEachNode(consumer: (nn: Node) -> Unit) {
        var n = first!!
        do {
            consumer(n)
        } while (n.next?.also { n = it } !== first)
    }

    internal inner class Node(x: Float, y: Float) : PVector(x, y) {
        constructor(v: PVector) : this(v.x, v.y)

        var age = 1
        var prev: Node? = null
        var next: Node? = null

        fun linkNext(n: Node) {
            if (next != null) {
                next!!.prev = n
            }
            n.next = next
            n.prev = this
            next = n
        }
    }
}

fun main() = DifferentialGrowth.run()