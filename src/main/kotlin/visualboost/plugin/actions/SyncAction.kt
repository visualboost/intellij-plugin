package visualboost.plugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile
import kotlinx.coroutines.*
import toFile
import visualboost.plugin.VbWindowService
import visualboost.plugin.editor.GlobalEditorToolbarHandler
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.util.ProjectDirectories
import java.io.File
import kotlin.coroutines.CoroutineContext


class SyncAction : AnAction(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        launch {
            val currentVirtualFile = getVirtualFileFromAction(e) ?: return@launch
            if (!fileIsExtension(project, currentVirtualFile)) return@launch

            saveFile(currentVirtualFile)

            //Open toolwindow
            withContext(Dispatchers.EDT) {
                ToolWindowManager.getInstance(project).getToolWindow("VisualBoost")?.show()
            }

            val currentFile = currentVirtualFile.toFile()
            val vbWindow = project.getService(VbWindowService::class.java).vbWindow

            val rootPath = project.basePath ?: return@launch
            val fileName = currentFile.relativeTo(File(rootPath)).path

            val fileContent = runReadAction {
                currentVirtualFile.findDocument()?.text
            } ?: return@launch

            vbWindow.triggerSynchronization(fileName, fileContent)

            //Update editor (hide sync button in editor)
            GlobalEditorToolbarHandler.synchronizationTriggered?.invoke(currentVirtualFile)
        }
    }

    private fun fileIsExtension(project: Project, vFile: VirtualFile): Boolean {
        val extensionDirPath = VbProjectSettings.getInstance(project).extensionDirectory ?: return false
        val extensionDir = ProjectDirectories.getExtensionDir(project, extensionDirPath) ?: return false
        val currentFile = vFile.toFile()
        return currentFile.absolutePath.contains(extensionDir.absolutePath)
    }

    override fun update(e: AnActionEvent) {
        launch {
            val project = e.project ?: return@launch

            val vFile = getVirtualFileFromAction(e) ?: return@launch
            val extension = vFile.extension

            val isJavascriptFile = extension == "js"
            if (!isJavascriptFile) {
                e.presentation.isVisible = false
                return@launch
            }

            e.presentation.isVisible = fileIsExtension(project, vFile)
        }
    }

    fun getVirtualFileFromAction(e: AnActionEvent): VirtualFile? {
        return runReadAction {
            val selectedElement: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return@runReadAction null
            return@runReadAction selectedElement.originalFile.virtualFile
        }
    }

    private suspend fun saveFile(vFile: VirtualFile){
        withContext(Dispatchers.EDT){
            val document = vFile.findDocument() ?: return@withContext
            FileDocumentManager.getInstance().saveDocument(document)
        }

    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}