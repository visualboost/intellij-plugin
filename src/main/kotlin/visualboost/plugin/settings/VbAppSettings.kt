package visualboost.plugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import visualboost.plugin.models.GenerationTarget
import visualboost.plugin.util.CredentialUtil

@Service(Service.Level.APP)
@State(
    name = "visualboost.intellij.settings.VbAppSettings",
    storages = [Storage("VisualBoostAppSettings.xml")]
)
class VbAppSettings: PersistentStateComponent<VbAppSettings> {

    var username: String? = null

    var useDefaultUrl: Boolean = true
    var defaultUrl: String = ""
    var mainUrl: String = ""
    var authUrl: String = ""

    var showIntroductionDialog: Boolean = true

    companion object {
        fun getInstance(): VbAppSettings {
            return ApplicationManager.getApplication().getService(VbAppSettings::class.java)
        }
    }

    override fun getState(): VbAppSettings {
        return this
    }

    override fun loadState(state: VbAppSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}