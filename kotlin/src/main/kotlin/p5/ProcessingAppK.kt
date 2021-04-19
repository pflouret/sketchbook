package p5

import com.sun.glass.ui.Size
import io.github.pflouret.sketchbook.p5.ProcessingApp
import javafx.scene.paint.Color
import midi.Op1
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
    KIKU4(858, 1156);

    constructor(width: Int, height: Int): this(Size(width, height))
}

open class ProcessingAppK : ProcessingApp() {
    var midi: MidiBus? = null
    var printControllerChanges = true

    companion object {
        const val OP1_DEVICE_NAME = "OP-1 Midi Device"
    }

    fun setupOp1(): MidiBus {
        val midibus = MidiBus(this, OP1_DEVICE_NAME, "")
        midi = midibus
        return midibus
    }

    open fun op1ControllerChangeAbs(cc: Op1, channel: Int, value: Int) {}
    open fun op1ControllerChangeRel(cc: Op1, channel: Int, value: Int) {
        when(cc) {
            Op1.REC -> record = true
            Op1.MIC -> exit()
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

    fun toRgbHex(color: Color) : Int {
        return ((color.opacity * 255).roundToInt() shl 24) +
                ((color.red * 255).roundToInt() shl 16) +
                ((color.green * 255).roundToInt() shl 8) +
                (color.blue * 255).roundToInt()
    }
}