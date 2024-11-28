package visualboost.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import visualboost.plugin.api.API
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.util.showInfo

/**
 * Load the VB project config and set the [VbProjectSettings.extensionDirPath] in the [VbProjectSettings].
 */
class LoadProjectConfigurationAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        initProjectConfigIfNecessary(project)
    }

    private fun initProjectConfigIfNecessary(project: Project) {
        val backgroundTask = object : Task.Backgroundable(null, "Loading VisualBoost Configuration", false) {
            override fun run(indicator: ProgressIndicator) {

                runBlocking {
                    val settings = VbProjectSettings.getInstance(project)

                    //Check if user is logged in or returns null if no credentials are set
                    val jwt = withContext(Dispatchers.IO) {
                        API.fetchToken()
                    } ?: return@runBlocking

                    val projectConfig = withContext(Dispatchers.IO) {
                        val projectId = VbProjectSettings.getInstance(project).projectId ?: return@withContext null
                        API.getProjectConfig(jwt, projectId)
                    } ?: return@runBlocking

                    settings.extensionDirPath = projectConfig.directories.routesDirs.extension
                    project.showInfo("Configuration updated", "Successfully updated the VB project configuration")
                }

            }
        }

        ProgressManager.getInstance().run(backgroundTask)
    }


}