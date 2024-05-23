package visualboost.plugin.events

import visualboost.plugin.settings.VbPluginSettingsState

object GlobalEvents {

    var onSettingsApply: ((settings: VbPluginSettingsState) -> Unit)? = null
    var onUrlChanged: ((url: String) -> Unit)? = null

}