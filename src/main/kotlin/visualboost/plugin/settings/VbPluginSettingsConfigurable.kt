package visualboost.plugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import visualboost.plugin.events.GlobalEvents
import javax.swing.JComponent

class VbPluginSettingsConfigurable(val project: Project) : Configurable{

    lateinit var component: VbPluginSettingsComponent

    override fun createComponent(): JComponent {
        component = VbPluginSettingsComponent(project)
        return component.getPanel()
    }

    override fun isModified(): Boolean {
        val url = component.getUrl()
        val target = component.getTarget()
        val projectId = component.getProjectId()

        val settings = VbPluginSettingsState.getInstance()
        val urlModified = url != settings.url
        val targetModified = target != settings.target
        val projectIdModified = projectId != settings.projectId

        val paramsAreModified = urlModified || targetModified || projectIdModified
        val paramsAreValid = url.isNotBlank() && target != null

        return paramsAreModified && paramsAreValid
    }

    override fun apply() {
        val settings = VbPluginSettingsState.getInstance()

        val urlModified = component.getUrl() != settings.url
        if(urlModified){
            settings.url = component.getUrl()
            GlobalEvents.onUrlChanged?.invoke(settings.url)
        }

        val targetModified = component.getTarget() != settings.target
        if(targetModified){
            settings.target = component.getTarget()
        }

        val projectIdModified = component.getProjectId() != settings.projectId
        if(projectIdModified){
            settings.projectId = component.getProjectId()
        }

        GlobalEvents.onSettingsApply?.invoke(settings)
    }

    override fun reset() {
        val settings = VbPluginSettingsState.getInstance()
        component.setUrl(settings.url)
        component.setTarget(settings.target)
        component.setProjectId(settings.projectId)
    }

    override fun getDisplayName(): String {
        return "Settings: VisualBoost"
    }
}