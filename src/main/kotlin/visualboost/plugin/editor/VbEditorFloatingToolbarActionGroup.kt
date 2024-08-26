package visualboost.plugin.editor

import com.intellij.openapi.actionSystem.*

class VbEditorFloatingActionGroup : DefaultActionGroup() {
    companion object {
        const val ACTION_GROUP = "visualboost.editor.floating_toolbar"
    }

//    override fun update(e: AnActionEvent) {
//        val currentVirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
//        val fileIsModified = FileDocumentManager.getInstance().isFileModified(currentVirtualFile)
//
//        e.presentation.isVisible = fileIsModified
//    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

}
