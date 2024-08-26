package visualboost.plugin.actions

import com.intellij.openapi.actionSystem.*
import visualboost.plugin.VbWindowService
import visualboost.plugin.settings.VbAppSettings
import visualboost.plugin.settings.VbProjectSettings


class ZoomOutAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vbWindow = project.getService(VbWindowService::class.java).vbWindow

        vbWindow.zoomOut()
        val settings = VbProjectSettings.getInstance(project)
        settings.zoomLevel = vbWindow.getZoomLevel()
    }
}