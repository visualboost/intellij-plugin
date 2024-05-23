package visualboost.plugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import visualboost.plugin.models.GenerationTarget

@State(
    name = "visualboost.intellij.settings.VbSettingsState",
    storages = [Storage("VisualBoostSettings.xml")]
)
class VbPluginSettingsState: PersistentStateComponent<VbPluginSettingsState> {

    var url: String = ""
    var target: GenerationTarget? = null
    var projectId: String = ""

    companion object {
//        const val DEFAULT_URL = "http://localhost:3000"
        const val DEFAULT_URL = "https://app.visualboost.de"

        fun getInstance(): VbPluginSettingsState{
            return ApplicationManager.getApplication().getService(VbPluginSettingsState::class.java)
        }
    }

    override fun getState(): VbPluginSettingsState {
        return this
    }

    override fun loadState(state: VbPluginSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun noStateLoaded() {
        this.url = DEFAULT_URL
    }

    fun isValid(): Boolean{
        return target != null && url.isNotEmpty()
    }
}