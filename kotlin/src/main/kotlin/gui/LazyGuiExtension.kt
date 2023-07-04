package gui

import com.krab.lazy.LazyGui
import com.krab.lazy.nodes.FolderNode
import com.krab.lazy.stores.NodeTree

fun LazyGui.clearNodeTreeCache() {
    NodeTree::class.java.getDeclaredField("nodesByPath").let {
        it.trySetAccessible()
        (it.get(null) as HashMap<*, *>).clear()
    }
}

fun LazyGui.clearFolderChildren(path: String) {
    (NodeTree.findNode(path) as? FolderNode)?.children?.clear()
}
