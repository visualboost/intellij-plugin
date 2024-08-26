package visualboost.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import openInEditor
import visualboost.plugin.VbWindowService
import visualboost.plugin.api.API
import visualboost.plugin.settings.VbAppSettings
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.util.EnvWriter
import kotlin.coroutines.CoroutineContext


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

                    settings.extensionDirectory = projectConfig.directories.routesDirs.extension
                }

            }
        }

        ProgressManager.getInstance().run(backgroundTask)
    }


}