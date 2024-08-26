package visualboost.plugin.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.options.ShowSettingsUtil
import visualboost.plugin.VbWindowService
import visualboost.plugin.settings.VbPluginSettingsConfigurable
import javax.swing.JComponent


class ReloadVbAction : AnAction("Reload VisualBoost") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vbWindow = project.getService(VbWindowService::class.java).vbWindow

        vbWindow.reloadUrl()
    }


}