package visualboost.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory


class VbWindowFactory: ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val vbWindowService = project.getService(VbWindowService::class.java).vbWindow
        val component = toolWindow.component
        component.parent.add(vbWindowService.content)
    }

}