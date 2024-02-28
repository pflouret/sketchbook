package p5

import com.hamoid.VideoExport
import com.krab.lazy.LazyGui
import com.krab.lazy.LazyGuiSettings
import com.krab.lazy.nodes.FolderNode
import com.krab.lazy.stores.LayoutStore
import com.krab.lazy.stores.NodeTree
import com.sun.glass.ui.Size
import io.github.pflouret.sketchbook.p5.ProcessingApp
import javafx.scene.paint.Color
import midi.MidiController
import processing.core.PApplet
import processing.core.PVector
import processing.event.KeyEvent
import processing.event.MouseEvent
import themidibus.MidiBus
import java.util.Random
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
    var loadLatestSaveOnStartup = false
    var exportNextFrameSvg = false
    var w2 = 0f
    var h2 = 0f

    var midi: MidiBus? = null
    var video: VideoExport? = null
    lateinit var gui: LazyGui

    companion object {
    }

    open fun reset() {}

    override fun settings() {
        size(800, 800, P2D)
    }

    override fun setup() {
        super.setup()

        noFill()
        hint(ENABLE_STROKE_PURE)
        resetSeed(true)
        registerMethod("pre", this)
        registerMethod("post", this)

        surface.setResizable(true)
        surface.setTitle(javaClass.simpleName.lowercase())
        w2 = width / 2f
        h2 = height / 2f

        if (saveVideo) {
            video = createVideoExporter().also { it.startMovie() }
        }
    }

    open fun initGui() {
        gui = LazyGui(
            this,
            LazyGuiSettings()
                .setMainFontSize(10)
                .setSideFontSize(10)
                .setAutosaveLockGuardEnabled(false)
                .setLoadLatestSaveOnStartup(loadLatestSaveOnStartup)
        )
        LayoutStore.setFolderRowClickClosesWindowIfOpen(true)
        LayoutStore.setResizeRectangleSize(11f)
    }

    fun pre() {
        if (::gui.isInitialized) {
            hideGuiInternalFolders()
        }

        if (exportNextFrameSvg) {
            LayoutStore.setIsGuiHidden(true)
            makeSketchFilename("%s_####.svg").also {
                beginRecord(SVG, it)
                println(it)
            }
        }

    }

    private fun hideGuiInternalFolders() {
        NodeTree.getAllNodesAsList()
            .filter { it is FolderNode && it.path.replace("/?$", "/").contains("internal") }
            .forEach { gui.hide(it.path) }
    }

    fun post() {
        exportNextFrameSvg = false
        try { endRecord() } catch (_: RuntimeException) { recorder = null }
        video?.saveFrame()
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

    open fun pr(vararg args: Any?) = println(args.mapNotNull { it }.joinToString(" "))
    open fun toggleLoop() = if (isLooping) noLoop() else loop()
    open fun screenshot() = saveFrame(makeSketchFilename("%s_####.png"))

    open fun random() = random(1f)
    open fun random(high: Number) = random(high.toFloat())
    open fun random(low: Number, high: Number) = random(low.toFloat(), high.toFloat())
    open fun randomVector() = randomVector(width.toFloat(), height.toFloat())
    open fun randomVector(highX: Number, highY: Number) = PVector(random(highX), random(highY))
    open fun prob(probability: Float) = random(1) < probability
    open fun map(value: Number, start1: Number, stop1: Number, start2: Number, stop2: Number) =
        PApplet.map(
            value.toFloat(),
            start1.toFloat(),
            stop1.toFloat(),
            start2.toFloat(),
            stop2.toFloat()
        )

    open fun point(v: PVector) = point(v.x, v.y)
    open fun vertex(v: PVector) = vertex(v.x, v.y)
    open fun curveVertex(v: PVector) = curveVertex(v.x, v.y)
    open fun noise(v: PVector) = noise(v.x, v.y, v.z)
    open fun translate(v: PVector) = translate(v.x, v.y)
    fun inViewport(v: PVector) = v.within(width, height)

    fun withShape(closeMode: Int = OPEN, block: () -> Unit) {
        beginShape()
        block()
        endShape(closeMode)
    }

    fun withPush(block: () -> Unit) {
        push()
        block()
        pop()
    }

    fun createVideoExporter(): VideoExport {
        return VideoExport(this, makeSketchFilename("%s.mp4")).also {
            it.setFrameRate(60f)
            it.setDebugging(false)
        }
    }

    override fun keyTyped(event: KeyEvent) {
        when (event.key) {
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

    override fun handleMouseEvent(event: MouseEvent?) {
        super.handleMouseEvent(event)
        if (::gui.isInitialized /*&& !gui.isMouseOutsideGui*/) {
            redraw()
        }
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
        }
        redraw()
    }
    open fun controllerChangeRel(channel: Int, cc: Int, value: Int) {}

    open fun noteOn(channel: Int, pitch: Int, velocity: Int) {}
    open fun noteOff(channel: Int, pitch: Int, velocity: Int) {}
}

fun PApplet.getRandom() = this.javaClass.getDeclaredField("internalRandom").let {
    it.trySetAccessible()
    it.get(this) as Random
}