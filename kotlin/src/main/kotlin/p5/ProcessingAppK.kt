package p5

import com.hamoid.VideoExport
import com.sun.glass.ui.Size
import io.github.pflouret.sketchbook.p5.ProcessingApp
import javafx.scene.paint.Color
import midi.BS
import midi.MidiDevice
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
        const val OP1_DEVICE_NAME = "OP-1 Midi Device"
        const val BEATSTEP_DEVICE_NAME = "Arturia BeatStep"
    }

    open fun drawInternal() {}
    open fun reset() {}

    override fun settings() {
        size(800, 800, FX2D)
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
    open fun randomVector() = PVector(random(width.toFloat()), random(height.toFloat()))
    open fun toggleLoop() = if (isLooping) noLoop() else loop()
    open fun point(v: PVector) = point(v.x, v.y)
    open fun vertex(v: PVector) = vertex(v.x, v.y)
    open fun curveVertex(v: PVector) = curveVertex(v.x, v.y)
    open fun noise(v: PVector) = noise(v.x, v.y, v.z)
    open fun screenshot() = saveFrame(makeSketchFilename("%s_####.png"))
    fun inViewport(v: PVector) = v.within(width, height)

    fun withShape(block: () -> Unit) {
        beginShape()
        block()
        endShape()
    }

    fun setupOp1(): MidiBus {
        val midibus = MidiBus(this, OP1_DEVICE_NAME, "")
        midi = midibus
        return midibus
    }

    fun setupBeatStep(): MidiBus {
        val midibus = MidiBus(this, BEATSTEP_DEVICE_NAME, "")
        midi = midibus
        return midibus
    }


    open fun controllerChangeAbs(cc: MidiDevice, channel: Int, value: Int) {}
    open fun controllerChangeRel(cc: MidiDevice, channel: Int, value: Int) {
        when (cc) {
            Op1.REC, BS.PAD_16 -> exportNextFrameSvg = true
            Op1.STOP, BS.STOP -> exit()
            Op1.MIC, BS.PAD_8 -> reset()
            Op1.PLAY, BS.PAD_7 -> toggleLoop()
            else -> return
        }
    }

    open fun noteOn(channel: Int, pitch: Int, velocity: Int) {}
    open fun noteOff(channel: Int, pitch: Int, velocity: Int) {}

    open fun controllerChange(channel: Int, number: Int, value: Int) {
        val attachedInputs = midi?.attachedInputs() ?: return
        val cc: MidiDevice
        val relValue: Int

        when {
            attachedInputs.contains(OP1_DEVICE_NAME) -> {
                cc = Op1.valueOf(number)
                relValue = if (value > 1) -1 else 1
            }
            attachedInputs.contains(BEATSTEP_DEVICE_NAME) -> {
                cc = BS.valueOf(number)
                relValue = if (value > 65) -1 else 1
            }
            else -> return
        }

        if (!cc.isKnob && value > 0) {
            return
        }

        controllerChangeAbs(cc, channel, if (cc.isKnob) value else relValue)
        controllerChangeRel(cc, channel, relValue)

        if (redrawOnEvent) {
            redraw()
        }
    }

    fun createVideoExporter(): VideoExport {
        return VideoExport(this, makeSketchFilename("%s.mp4")).also {
            it.setFrameRate(60f)
            it.setDebugging(false)
        }
    }

}