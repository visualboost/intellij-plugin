package visualboost.plugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import visualboost.plugin.events.GlobalEvents
import javax.swing.JComponent

class VbPluginSettingsConfigurable(val project: Project) : Configurable{

    lateinit var component: VbPluginSettingsComponent
    private val appSettings = VbAppSettings.getInstance()
    private val projectSettings = VbProjectSettings.getInstance(project)

    override fun createComponent(): JComponent {
        component = VbPluginSettingsComponent(project)
        return component.getPanel()
    }

    override fun isModified(): Boolean {
        val target = component.getTarget()

        val targetModified = target != projectSettings.target
        val useDefaultUrl = !component.useCustomUrlIsSelected()
        val useCustomUrlIsModified = appSettings.useDefaultUrl != useDefaultUrl

        return targetModified || projectIdIsModified() || useCustomUrlIsModified
    }

    override fun apply() {
        val targetModified = component.getTarget() != projectSettings.target
        if(targetModified){
            projectSettings.target = component.getTarget()
        }

        if(projectIdIsModified()){
            projectSettings.projectId = component.getProject()?._id

            if(projectSettings.projectId != null){
                GlobalEvents.onProjectChanged?.invoke(projectSettings.projectId!!)
            }
        }

        val useDefaultUrl = !component.useCustomUrlIsSelected()
        val useCustomUrlIsModified = appSettings.useDefaultUrl != useDefaultUrl
        if(useCustomUrlIsModified){
            appSettings.useDefaultUrl = useDefaultUrl

            if(!appSettings.useDefaultUrl){
                appSettings.defaultUrl = component.getDefaultUrl()
                appSettings.mainUrl = component.getMainUrl()
                appSettings.authUrl = component.getAuthUrl()
            }
        }

        GlobalEvents.onSettingsApply?.invoke(appSettings)
    }

    private fun projectIdIsModified(): Boolean{
        val projectId = component.getProject()?._id
        return projectId != projectSettings.projectId
    }

    override fun reset() {
        component.setTarget(projectSettings.target)
    }

    override fun getDisplayName(): String {
        return "Settings: VisualBoost"
    }
}