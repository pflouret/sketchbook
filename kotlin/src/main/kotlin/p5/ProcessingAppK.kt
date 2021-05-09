package p5

import com.hamoid.VideoExport
import com.sun.glass.ui.Size
import io.github.pflouret.sketchbook.p5.ProcessingApp
import javafx.scene.paint.Color
import midi.Op1
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
    ORIGAMI_150(566, 566)
    ;

    constructor(width: Int, height: Int) : this(Size(width, height))
}

open class ProcessingAppK : ProcessingApp() {
    var printControllerChanges = true
    var exportNextFrameSvg = false
    var w2 = 0f
    var h2 = 0f

    var midi: MidiBus? = null
    var video: VideoExport? = null

    companion object {
        const val OP1_DEVICE_NAME = "OP-1 Midi Device"
    }

    open fun drawInternal() {}
    open fun reset() {}

    override fun settings() {
        size(800, 800, FX2D)
        w2 = width / 2f
        h2 = height / 2f
    }

    override fun setup() {
        super.setup()

        clear()
        noFill()
        hint(ENABLE_STROKE_PURE)
        resetSeed(true)
        registerMethod("pre", this)

        surface.setResizable(true)
        surface.setTitle(javaClass.simpleName.toLowerCase())

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
            'h' -> toggleGuiVisibility()
            'p' -> toggleLoop()
            'r' -> reset()
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
    open fun randomvector() = PVector(random(width.toFloat()), random(height.toFloat()))
    open fun toggleLoop() = if (isLooping) noLoop() else loop()
    open fun point(v: PVector) = point(v.x, v.y)
    open fun vertex(v: PVector) = vertex(v.x, v.y)
    open fun curveVertex(v: PVector) = curveVertex(v.x, v.y)
    open fun noise(v: PVector) = noise(v.x, v.y, v.z)
    open fun screenshot() = saveFrame(makeSketchFilename("%s_####.png"))


    fun setupOp1(): MidiBus {
//        MidiBus.list()
        val midibus = MidiBus(this, OP1_DEVICE_NAME, "")
        midi = midibus
        return midibus
    }

    open fun op1ControllerChangeAbs(cc: Op1, channel: Int, value: Int) {}
    open fun op1ControllerChangeRel(cc: Op1, channel: Int, value: Int) {
        when (cc) {
            Op1.REC -> exportNextFrameSvg = true
            Op1.MIC -> exit()
            Op1.STOP -> reset()
            else -> return
        }
    }

    open fun noteOn(channel: Int, pitch: Int, velocity: Int) {}
    open fun noteOff(channel: Int, pitch: Int, velocity: Int) {}

    open fun controllerChange(channel: Int, number: Int, value: Int) {
        val op1IsAttached = midi?.attachedInputs()?.contains(OP1_DEVICE_NAME) ?: false
        if (!op1IsAttached) {
            return
        }

        val cc = Op1.valueOf(number)
        val relValue = if (value > 1) -1 else value

        if (!cc.isKnob && value > 1) {
            return
        }

        op1ControllerChangeAbs(cc, channel, if (cc.isKnob) value else relValue)
        op1ControllerChangeRel(cc, channel, relValue)

        if (redrawOnEvent) {
            redraw()
        }
        if (printControllerChanges) {
            println("${cc.name}: ch: $channel v: $value rel: $relValue")
        }
    }

    fun createVideoExporter(): VideoExport {
        return VideoExport(this, makeSketchFilename("%s.mp4")).also {
            it.setFrameRate(60f)
            it.setDebugging(false)
        }
    }

}