package visualboost.plugin.components.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget

class LoadProjectConfigStatusbarWidget(project: Project): StatusBarWidget {

    override fun ID(): String {
        return "VisualBoostLoadProjectConfigWidget"
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation? {
        return super.getPresentation()
    }


}