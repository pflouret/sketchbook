package etc

import com.krab.lazy.PickerColor
import gui.LazyGuiControlDelegate
import p5.ProcessingAppK

class GuiTest : ProcessingAppK() {
    companion object {
        fun run() = GuiTest().runSketch()
    }

    private var theCamelCase: PickerColor by LazyGuiControlDelegate("colorPicker", "", 1)

    override fun settings() {
        super.settings()
        size(500, 500, P2D)
    }

    override fun drawInternal() {
        background(theCamelCase.hex);
    }
}

fun main() {
    GuiTest.run();
}
