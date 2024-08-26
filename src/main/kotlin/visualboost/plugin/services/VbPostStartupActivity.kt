package visualboost.plugin.services

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import visualboost.plugin.dialog.IntroductionDialog
import visualboost.plugin.settings.VbAppSettings


class VbPostStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        loadProjectConfiguration()
        showIntroductionDialogIfNeeded()
    }

    private fun loadProjectConfiguration() {
        runInEdt {
            val action =
                ActionManager.getInstance().getAction("visualboost.plugin.actions.LoadProjectConfigurationAction")
            ActionManager.getInstance().tryToExecute(action, null, null, null, false)
        }
    }

    private fun showIntroductionDialogIfNeeded() {
        runInEdt {
            val showIntroductionDialog = VbAppSettings.getInstance().showIntroductionDialog
            if(!showIntroductionDialog) return@runInEdt

            val action = ActionManager.getInstance().getAction("visualboost.plugin.actions.ShowIntroductionDialogAction")
            ActionManager.getInstance().tryToExecute(action, null, null, null, false)
        }
    }


}