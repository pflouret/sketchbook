package util

import com.krab.lazy.nodes.FolderNode
import com.krab.lazy.stores.NodeTree

fun clearNodeTreeCache() {
    NodeTree::class.java.getDeclaredField("nodesByPath").let {
        it.trySetAccessible()
        (it.get(null) as HashMap<*, *>).clear()
    }
}

fun clearFolderChildren(path: String) {
    (NodeTree.findNode("s") as? FolderNode)?.children?.clear()
}

