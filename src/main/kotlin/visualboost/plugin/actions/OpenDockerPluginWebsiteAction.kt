package visualboost.plugin.actions

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import visualboost.plugin.settings.VbPluginSettingsConfigurable


class OpenDockerPluginWebsiteAction : AnAction() {

    override fun actionPerformed(p0: AnActionEvent) {
        BrowserUtil.open("https://plugins.jetbrains.com/plugin/7724-docker")
    }


}