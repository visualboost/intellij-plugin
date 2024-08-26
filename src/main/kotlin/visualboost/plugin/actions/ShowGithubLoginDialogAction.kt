package visualboost.plugin.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import visualboost.plugin.dialog.GithubLoginDialog
import visualboost.plugin.settings.VbPluginSettingsConfigurable


class ShowGithubLoginDialogAction : NotificationAction("Open Settings") {

    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        //TODO: show github login dialog to allow relogin
//        val project = e.project ?: return
//        ShowSettingsUtil.getInstance().showSettingsDialog(project, VbPluginSettingsConfigurable::class.java)
    }

}