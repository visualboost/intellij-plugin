package visualboost.plugin.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.writeText
import kotlinx.coroutines.*
import toFile
import visualboost.plugin.dialog.GithubLoginDialog
import visualboost.plugin.settings.VbPluginSettingsConfigurable
import kotlin.coroutines.CoroutineContext


class ShowGithubLoginDialogAction : AnAction(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()

    override fun actionPerformed(e: AnActionEvent) {
        //TODO: show github login dialog
//        val currentVirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
//        val file = currentVirtualFile.toFile()

//        launch {
//            (0..50).forEach {
////                delay(100)
//
//                runInEdt {
//                    ApplicationManager.getApplication().runWriteAction {
//                        val currentText = currentVirtualFile.readText()
//                        currentVirtualFile.writeText(currentText + "New number $it\n")
//                    }
//                }
//
//                currentVirtualFile.refresh(true, false)
//
//            }
//        }

    }
}