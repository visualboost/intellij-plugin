package visualboost.plugin.actions

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import visualboost.plugin.api.models.AllProjectsResponseBody
import visualboost.plugin.settings.VbPluginSettingsConfigurable


class TestAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val currentVirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return


//        val fileIsModified = FileDocumentManager.getInstance().isFileModified(currentVirtualFile)

//        BrowserUtil.open("https://www.google.de")
    }

    private fun saveFile(vFile: VirtualFile){
        val document = vFile.findDocument() ?: return
        FileDocumentManager.getInstance().saveDocument(document)
    }




}