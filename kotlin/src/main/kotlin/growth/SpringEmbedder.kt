package growth

import p5.ProcessingAppK
import processing.core.PApplet
import processing.core.PVector
import processing.event.KeyEvent
import util.Size
import java.util.Vector

class SpringEmbedder : ProcessingAppK() {
    private var vertices = mutableListOf<TVertex>()
    private var splitDistance = 9f
    private var splitProbability = 0.208f
    private var splitRandomness = 0.2f

    private var drawPoints = false
    private var fillShape = false

    private val constrainToViewport = false
    private val padding = 15f
    private var temperature = 0f

    override fun settings() {
        size(SIZE.width, SIZE.height, P2D)
        smooth(4)
    }

    override fun setup() {
        super.setup()

        noiseDetail(6, 0.45f)
        strokeWeight(2f)

        reset()
    }

    override fun draw() {
        background(255)

        if (fillShape) {
            fill(0, 230f)
        } else {
            noFill()
        }

        withPush {
            translate(w2, h2)
            scale(0.5f)
            drawShape()
            if (drawPoints) {
                drawPoints()
            }
        }
        eames()
//        fruchtermanReingold();
    }

    private fun drawShape() {
        withShape {
            var v = vertices.first()
            val first = vertices.first()
            curveVertex(first.prev!!.x, first.prev!!.y)
            do {
                curveVertex(v.x, v.y)
            } while (v.next.also { v = it!! } !== first)
            curveVertex(first.x, first.y)
            curveVertex(first.next!!.x, first.next!!.y)
        }
    }

    private fun drawPoints() {
        withPush {
            strokeWeight(6f)
            vertices.forEach(::point)
        }
    }


    override fun reset() {
        vertices = mutableListOf()
        temperature = 2f
        val r = 30f
        val initialPoly = listOf(
            PVector(r, 0f),
            PVector(0f, -r),
            PVector(-r, 0f),
            PVector(0f, r),
        )

        var prev: TVertex? = null
        var first: TVertex? = null
        for (v in initialPoly) {
            val vx = TVertex(v.x, v.y)
            vertices.add(vx)
            if (prev == null) {
                first = vx
            } else {
                prev.setBefore(vx)
            }
            prev = vx
        }
        first?.prev = prev
        prev?.next = first
    }

    private fun eames() {
        val attractionFactor = 0.15f
        val repulsionStrength = 0.75f
        val repulsionRadius = 30f
        splitDistance = 7f
        splitRandomness = 1.0f
        val attractionDeltas = vertices.associateWith {
            PVector.sub(it.prev!!, it)
                .setMag(log(it.dist(it.prev!!) / 15))
                .add(PVector.sub(it.next!!, it).setMag(it.dist(it.next!!) / 15))
        }

        val repulsionDeltas = vertices.associateWith { a ->
            vertices.filter { b ->
                b !== a && b !== a.prev && b !== a.next && a.dist(b) < repulsionRadius
            }.map { b ->
                PVector.sub(a, b).setMag(1 / sqrt(a.dist(b)))
            }.ifEmpty {
                listOf(PVector(0f, 0f))
            }.reduce { acc, v ->
                acc.add(v)
            }
        }

        vertices.forEach { a ->
            val delta = PVector.mult(attractionDeltas[a]!!, attractionFactor)
                .add(PVector.mult(repulsionDeltas[a]!!, repulsionStrength))
            val b = PVector.add(a, delta)
            if (!constrainToViewport
                || (b.x > -w2 + padding
                        && b.x < w2 - padding
                        && b.y > -h2 + padding
                        && b.y < h2 - padding)
            ) {
                a.set(b)
            }
        }
        noiseResample(false)
    }

    private fun fruchtermanReingold() {
        if (temperature <= 0) {
            return
        }

        val area = (width * height).toFloat()
        val repulsionRadius = 18f
        splitDistance = 7f
        splitProbability = 0.003f
        splitRandomness = 0.3f

        val attractionDeltas = mutableMapOf<TVertex?, PVector>()
        for (a in vertices) {
            val v = PVector.sub(a, a.next)
            v.setMag(v.magSq() / sqrt(area / vertices.size))
            attractionDeltas[a] = PVector.add(
                attractionDeltas.getOrElse(a) { PVector(0f, 0f) },
                v.mult(-1f)
            )
            attractionDeltas[a.next] = PVector.add(
                attractionDeltas.getOrElse(a) { PVector(0f, 0f) },
                v
            )
        }

        val repulsionDeltas = vertices.associateWith { a ->
            vertices.filter { b ->
                b !== a && b !== a.prev && b !== a.next && a.dist(b) < repulsionRadius
            }.map { b ->
                PVector.sub(a, b).setMag(area / vertices.size / a.dist(b))
            }.ifEmpty {
                listOf(PVector(0f, 0f))
            }.reduce { acc, v ->
                acc.add(v)
            }
        }

        vertices.forEach { a ->
            val da = PVector.add(attractionDeltas[a]!!, repulsionDeltas[a])
            val delta = da.setMag(.5f * min(da.mag(), temperature))
            a.add(delta)
            if (!constrainToViewport) {
                a.set(
                    min(w2 - padding, max(-w2 + padding, a.x)),
                    min(h2 - padding, max(-h2 + padding, a.y))
                )
            }
        }

        noiseResample(false)
        temperature -= .0001f
    }


    private fun resample() {
        resample(splitProbability)
    }

    private fun resample(p: Float) {
        val newVertices = Vector<TVertex>(1000)
        //vertices.sort((o1, o2) -> -Float.compare(o1.magSquared(), o2.magSquared()));
        vertices.addAll(
            vertices.mapNotNull { v ->
                if (random(1f) < p && v.dist(v.next) > splitDistance) {
                    val jitter = random(-splitRandomness, splitRandomness)
                    TVertex(PVector.add(v, v.next).mult(0.5f).add(jitter, jitter)).also {
                        v.setBefore(it)
                    }
                } else {
                    null
                }
            })
        vertices.addAll(newVertices)
    }

    private fun noiseResample(force: Boolean) {
        val noiseScale = 1 / 100.0f
        vertices.addAll(
            vertices.mapNotNull { v ->
                if ((force || random() < .17f * noise(v.x * noiseScale))
                    && v.dist(v.next) > splitDistance
                ) {
                    val jitter = random(-splitRandomness, splitRandomness)
                    TVertex(PVector.add(v, v.next).mult(0.5f).add(jitter, jitter)).also {
                        v.setBefore(it)
                    }
                } else {
                    null
                }
            }
        )
    }

    override fun keyTyped(event: KeyEvent) {
        super.keyTyped(event)
        when (key) {
            's' -> noiseResample(true)
            'd' -> drawPoints = !drawPoints
            'f' -> fillShape = !fillShape
        }
    }


    companion object {
        val SIZE = Size(1000, 1000)
//        val SIZE = PaperSize.INDEX_CARD.size
//        val SIZE = PaperSize.INDEX_CARD.landscape()
//        val SIZE = PaperSize.ORIGAMI_150.size
    }
}

fun main() {
    PApplet.runSketch(arrayOf("growth.SpringEmbedder"), null)
}