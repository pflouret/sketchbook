package gui

import com.krab.lazy.LazyGui
import com.krab.lazy.nodes.FolderNode
import com.krab.lazy.stores.NodeTree
import processing.core.PVector

fun LazyGui.clearNodeTreeCache() {
    NodeTree::class.java.getDeclaredField("nodesByPath").let {
        it.trySetAccessible()
        (it.get(null) as HashMap<*, *>).clear()
    }
}

fun LazyGui.clearFolderChildren(path: String) {
    (NodeTree.findNode(path) as? FolderNode)?.children?.clear()
}

fun LazyGui.buttonSet(gui: LazyGui, path: String, value: Boolean) = button(path)
fun LazyGui.plotSetVector(gui: LazyGui, path: String, v: PVector) = plotSet(path, v)
