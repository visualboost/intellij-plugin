package visualboost.plugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import visualboost.plugin.models.GenerationTarget
import visualboost.plugin.util.CredentialUtil

@State(
    name = "visualboost.intellij.settings.VbProjectSettings",
    storages = [Storage("VisualBoostProjectSettings.xml")]
)
@Service(Service.Level.PROJECT)
class VbProjectSettings: PersistentStateComponent<VbProjectSettings> {

    var target: GenerationTarget? = null
    var projectId: String? = null
    var extensionDirectory : String? = null
    var zoomLevel: Double = 0.8

    companion object {
        fun getInstance(project: Project): VbProjectSettings {
            return project.getService(VbProjectSettings::class.java)
        }
    }

    override fun getState(): VbProjectSettings {
        return this
    }

    override fun loadState(state: VbProjectSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun projectTargetIsSet(): Boolean {
        return target != null
    }

    fun projectSelected(): Boolean{
        return projectId != null
    }

    fun clearProjectId(){
        this.projectId = null
    }

    fun isValid(): Boolean{
        return this.projectId != null && this.target != null
    }
}