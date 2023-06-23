package midi

import io.github.pflouret.sketchbook.p5.ProcessingApp
import themidibus.MidiBus
import javax.sound.midi.MidiMessage

interface MidiDevice {
    val isKnob: Boolean
}

enum class MidiController(val usbName: String) {
    OP1("OP-1 Midi Device"),
    BEATSTEP("Arturia BeatStep"),
    MIXER_ONE("Mixer One"),
    FIGHTER("Midi Fighter Twister")
}

// https://teenage.engineering/guides/op-1/layout
// https://api.pcloud.com/getpubthumb?code=XZ7zNDXZVEUpS86wBJXVCHvFCdIXpXViDvA7&size=1336x1865
enum class Op1(val number: Int, override val isKnob: Boolean = false) : MidiDevice {
    BLUE(64), GREEN(65), WHITE(66), ORANGE(67),
    BLUE_KNOB(1, true), GREEN_KNOB(2, true), WHITE_KNOB(3, true), ORANGE_KNOB(4, true),
    HELP(5), TEMPO(6), SYNTH(7), DRUM(8), TAPE(9), MIXER(10),
    T1(11), T2(12), T3(13), T4(14),
    LIFT(15), DROP(16), SPLIT(17), MIC(48), COM(49),
    S1(50), S2(51), S3(52), S4(21), S5(22), S6(23), S7(24), S8(25),
    SEQUENCER(26), REC(38), PLAY(39), STOP(40), REW(41), FF(42),
    UNDEFINED(999);

    companion object {
        private val NUMBER_TO_KEY = values().associateBy(Op1::number)
        fun valueOf(number: Int): Op1 = NUMBER_TO_KEY[number] ?: UNDEFINED
    }
}

class MidiEcho : ProcessingApp() {
    companion object {
        fun run() = MidiEcho().runSketch()
    }

    //    val midi = MidiBus(this, "OP-1 Midi Device", "")
//    val midi = MidiBus(this, "Arturia BeatStep", "")
    val midi = MidiBus(this, MidiController.FIGHTER.usbName, "")

    override fun settings() {
        size(100, 100, P2D)
    }

    override fun setup() {
        super.setup()
        background(255)
        MidiBus.list()
    }

    fun noteOn(channel: Int, pitch: Int, velocity: Int) {
        println()
        println("Note On:")
        println("--------")
        println("Channel:$channel")
        println("Pitch:$pitch")
        println("Velocity:$velocity")
    }

    fun noteOff(channel: Int, pitch: Int, velocity: Int) {
        println()
        println("Note Off:")
        println("--------")
        println("Channel:$channel")
        println("Pitch:$pitch")
        println("Velocity:$velocity")
    }

    fun controllerChange(channel: Int, number: Int, value: Int) {
        println()
        println("Controller Change:")
        println("--------")
        println("Channel:$channel")
        println("Number:$number")
        val i = value or 0xFF
        println("Value:$value ${i.toByte()}")
    }

    fun midiMessage(message: MidiMessage) {
//        println()
//        println("MidiMessage Data:")
//        println("--------")
//        println("Status Byte/MIDI Command:" + message.status)
//        for (i in message.message.indices) {
//            println("Param $i: ${message.message[i]}")
//        }
    }
}

fun main() {
    MidiEcho.run();
}
