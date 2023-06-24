package p5

import com.hamoid.VideoExport
import com.sun.glass.ui.Size
import io.github.pflouret.sketchbook.p5.ProcessingApp
import javafx.scene.paint.Color
import midi.MidiController
import processing.core.PVector
import processing.event.KeyEvent
import themidibus.MidiBus
import kotlin.math.roundToInt

enum class PaperSize(val size: Size) {
    // at 96dpi, or thereabouts (whatever inkscape likes)
    US_LETTER(816, 1056),
    A3(1122, 1587),
    A4(793, 1122),
    A5(559, 793),
    A6(396, 559),
    B5(665, 944),
    B6(472, 665),
    KIKU4(858, 1156),
    ORIGAMI_150(566, 566),
    CAHIER_XL(720, 936),
    INDEX_CARD(288, 480),
    TRAVELER_PASSPORT(336, 468),
    POSTCARD(415, 529)
    ;

    constructor(width: Int, height: Int) : this(Size(width, height))
    fun landscape() = Size(size.height, size.width)
}

fun PVector.within(width: Int, height: Int) = x > 0 && x < width && y > 0 && y < height

open class ProcessingAppK : ProcessingApp() {
    var printControllerChanges = true
    var exportNextFrameSvg = false
    var w2 = 0f
    var h2 = 0f

    var midi: MidiBus? = null
    var video: VideoExport? = null

    companion object {
    }

    open fun drawInternal() {}
    open fun reset() {}

    override fun settings() {
        size(800, 800, P2D)
    }

    override fun setup() {
        super.setup()

        clear()
        noFill()
        hint(ENABLE_STROKE_PURE)
        resetSeed(true)
        registerMethod("pre", this)

        surface.setResizable(true)
        surface.setTitle(javaClass.simpleName.lowercase())
        w2 = width / 2f
        h2 = height / 2f

        if (saveVideo) {
            video = createVideoExporter().also { it.startMovie() }
        }
    }

    override fun draw() {
        if (exportNextFrameSvg) {
            makeSketchFilename("%s_####.svg").also {
                beginRecord(SVG, it)
                println(it)
            }
        }

        drawInternal()

        if (exportNextFrameSvg) {
            endRecord()
            exportNextFrameSvg = false
        }

        video?.saveFrame()
    }

    override fun keyTyped(e: KeyEvent) {
        when (e.key) {
            'R' -> exportNextFrameSvg = true
            'S' -> screenshot()
            'c' -> clear()
            'p' -> toggleLoop()
            'x' -> reset()
            'V' -> {
                video?.endMovie()
                video = null
            }
            else -> {
            }
        }

        if (redrawOnEvent || exportNextFrameSvg) {
            redraw()
        }
    }

    open fun resetSeed() = resetSeed(false)
    open fun resetSeed(newSeed: Boolean) {
        if (newSeed) {
            seed = random(9999999f).toLong()
        }
        randomSeed(seed)
        noiseSeed(seed)
    }

    fun toRgbHex(color: Color): Int {
        return ((color.opacity * 255).roundToInt() shl 24) +
                ((color.red * 255).roundToInt() shl 16) +
                ((color.green * 255).roundToInt() shl 8) +
                (color.blue * 255).roundToInt()
    }

    open fun random() = random(1f)
    open fun random(high: Int) = random(high.toFloat())
    open fun random(low: Int, high: Int) = random(low.toFloat(), high.toFloat())
    open fun randomVector() = PVector(random(width.toFloat()), random(height.toFloat()))
    open fun toggleLoop() = if (isLooping) noLoop() else loop()
    open fun point(v: PVector) = point(v.x, v.y)
    open fun vertex(v: PVector) = vertex(v.x, v.y)
    open fun curveVertex(v: PVector) = curveVertex(v.x, v.y)
    open fun noise(v: PVector) = noise(v.x, v.y, v.z)
    open fun screenshot() = saveFrame(makeSketchFilename("%s_####.png"))
    open fun translate(v: PVector) = translate(v.x, v.y)
    fun inViewport(v: PVector) = v.within(width, height)

    fun withShape(block: () -> Unit) {
        beginShape()
        block()
        endShape()
    }

    fun initMidi(controller: MidiController): MidiBus =
        MidiBus(this, controller.usbName, "").let { midi = it; it }

    open fun controllerChange(channel: Int, number: Int, value: Int) {
        controllerChangeAbs(channel, number, value)
        controllerChangeRel(channel, number, if (value > 64) 1 else -1)
        if (redrawOnEvent) {
            redraw()
        }
    }

    open fun controllerChangeAbs(channel: Int, cc: Int, value: Int) {
        // midi fighter twister, channel 1(0-indexed) for cc switch 0 = off, 127 = on
        if (channel != 1 || value != 0) {
            return
        }
        when (cc) {
            12 -> toggleLoop()
            13 -> reset()
            14 -> exportNextFrameSvg = true
            15 -> exit()
            else -> return
        }
    }
    open fun controllerChangeRel(channel: Int, cc: Int, value: Int) {}

    open fun noteOn(channel: Int, pitch: Int, velocity: Int) {}
    open fun noteOff(channel: Int, pitch: Int, velocity: Int) {}

    fun createVideoExporter(): VideoExport {
        return VideoExport(this, makeSketchFilename("%s.mp4")).also {
            it.setFrameRate(60f)
            it.setDebugging(false)
        }
    }

}