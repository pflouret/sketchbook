package gui

import com.krab.lazy.LazyGui
import com.krab.lazy.nodes.ButtonNode
import com.krab.lazy.nodes.FolderNode
import com.krab.lazy.nodes.setValueBoolean
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

fun LazyGui.buttonSet(path: String, value: Boolean) {
    val fullPath: String = folder + path;
    if (NodeTree.isPathTakenByUnexpectedType(fullPath, ButtonNode::class.java)) {
        return
    }
    val node =
        (NodeTree.findNode(fullPath) as? ButtonNode) ?: NodeTree.findParentFolderLazyInitPath(path)
            .let {
                ButtonNode(path, it).also { node ->
                    NodeTree.insertNodeAtItsPath(node);
                }
            }

    node.setValueBoolean(value);
}

fun LazyGui.plotAdd(path: String, v: PVector) = plotSet(path, plotXY(path).add(v))
fun LazyGui.plotAdd(path: String, x: Float, y: Float) = plotSet(path, plotXY(path).add(x, y))
fun LazyGui.sliderIntAdd(path: String, value: Int) = sliderIntSet(path, sliderInt(path) + value)

