package visualboost.plugin.editor

import com.intellij.openapi.vfs.VirtualFile


object GlobalEditorToolbarHandler {
    var synchronizationTriggered: ((vFile: VirtualFile) -> Unit)? = null
}

