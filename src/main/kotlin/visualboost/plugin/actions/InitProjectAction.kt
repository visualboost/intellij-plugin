package visualboost.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import visualboost.plugin.VbWindowService
import visualboost.plugin.api.models.project.VisualBoostProjectResponseBody
import visualboost.plugin.dialog.FileExistDialog
import visualboost.plugin.dialog.InitProjectDialog
import visualboost.plugin.dialog.ProjectSetupDialog
import visualboost.plugin.util.createDockerConfiguration
import javax.swing.JLabel


class InitProjectAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vbWindow = project.getService(VbWindowService::class.java).vbWindow

        val rootDir = vbWindow.getRootDir() ?: return
        InitProjectDialog(project, rootDir).show()
    }




}