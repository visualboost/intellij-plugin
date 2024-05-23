package visualboost.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiFile
import visualboost.plugin.VbWindowService
import java.io.File


class SyncAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vbWindow = project.getService(VbWindowService::class.java).vbWindow

        val file = getFile(e) ?: return
        val rootPath = project.basePath ?: return

        val fileName = file.relativeTo(File(rootPath)).path
        val fileContent = file.readText()

        vbWindow.triggerSynchronization(fileName, fileContent)

        //TODO: synchronize file - send the selected file content to main-backend (New route in main backend, build-backend && Analyser needs to be adapt so it can analyse a single file)
    }

    override fun update(e: AnActionEvent) {
        val file = getFile(e) ?: return
        val extension = file.extension

        //TODO: check if the file is in extension directory
        val isValidFile = extension == "js"

//        this.templatePresentation.isVisible = isValidFile
        e.presentation.isVisible = isValidFile
    }

    fun getFile(e: AnActionEvent): File? {
        val selectedElement: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return null
        val vFile = selectedElement.originalFile.virtualFile
        val path: String = vFile.path
        return File(path)
    }
}