package util

import com.krab.lazy.LazyGui
import p5.ProcessingAppK


class GuiTest : ProcessingAppK() {
    companion object {
        fun run() = GuiTest().runSketch()
    }

    private lateinit var gui: LazyGui

    override fun settings() {
        size(500, 500, P2D)
    }

    override fun setup() {
        super.setup()
        background(255)
        gui = LazyGui(this)
    }

    override fun drawInternal() {
        background(gui.colorPicker("background").hex);
    }
}

fun main() {
    GuiTest.run();
}
