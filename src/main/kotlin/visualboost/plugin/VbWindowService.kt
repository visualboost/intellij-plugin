package visualboost.plugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

class VbWindowService(val project: Project) {
    val vbWindow = VbWindow(project)
}
