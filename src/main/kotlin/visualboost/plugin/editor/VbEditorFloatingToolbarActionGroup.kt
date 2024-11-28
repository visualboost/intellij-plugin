package visualboost.plugin.editor

import com.intellij.openapi.actionSystem.*

class VbEditorFloatingActionGroup : DefaultActionGroup() {
    companion object {
        const val ACTION_GROUP = "visualboost.editor.floating_toolbar"
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT


}
