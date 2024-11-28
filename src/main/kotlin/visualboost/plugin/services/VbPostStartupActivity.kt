package visualboost.plugin.services

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import visualboost.plugin.api.API
import visualboost.plugin.dialog.IntroductionDialog
import visualboost.plugin.dialog.ProjectSetupDialog
import visualboost.plugin.settings.VbAppSettings
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.util.showInfo
import java.net.URI


class VbPostStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        loadProjectConfiguration()
        showIntroductionDialogIfNeeded(project)
    }

    private fun loadProjectConfiguration() {
        runInEdt {
            val action =
                ActionManager.getInstance().getAction("visualboost.plugin.actions.LoadProjectConfigurationAction")
            ActionManager.getInstance().tryToExecute(action, null, null, null, false)
        }
    }

    private fun showIntroductionDialogIfNeeded(project: Project) {
        val showIntroductionDialog = VbAppSettings.getInstance().showIntroductionDialog
        if (!showIntroductionDialog) return

        val action = ActionManager.getInstance().getAction("visualboost.plugin.actions.ShowIntroductionDialogAction")
        project.showInfo(
            "VisualBoost installed",
            "For an overview of the plugin, please take a look at the Introduction.",
            listOf(action)
        )
        VbAppSettings.getInstance().showIntroductionDialog = false
    }


}