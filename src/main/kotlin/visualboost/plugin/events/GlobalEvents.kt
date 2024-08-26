package visualboost.plugin.events

import visualboost.plugin.settings.VbAppSettings

object GlobalEvents {

    var onSettingsApply: ((settings: VbAppSettings) -> Unit)? = null
    var onProjectChanged: ((projectId: String) -> Unit)? = null

}