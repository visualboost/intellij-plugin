package visualboost.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory


class VbWindowFactory: ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val vbWindow = project.getService(VbWindowService::class.java).vbWindow
        val component = toolWindow.component

        vbWindow.initBrowser(toolWindow.disposable)
        component.parent.add(vbWindow.content)
    }

}