package visualboost.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import kotlinx.coroutines.*
import openInEditor
import visualboost.plugin.VbWindowService
import visualboost.plugin.api.API
import visualboost.plugin.settings.VbAppSettings
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.util.EnvWriter
import kotlin.coroutines.CoroutineContext


class CreateEnvAction : AnAction(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vbWindow = project.getService(VbWindowService::class.java).vbWindow

        launch {
            val settings = VbProjectSettings.getInstance(project)
            val projectConfig = withContext(Dispatchers.IO){
                val jwt = API.fetchToken() ?: return@withContext null
                return@withContext API.getProjectConfig(jwt, settings.projectId ?: return@withContext null)
            } ?: return@launch

            val rootProject = vbWindow.getRootDir() ?: return@launch
            val mainEnvFile = EnvWriter.writeMainEnvFile(rootProject, projectConfig)
            val dbEnvFile = EnvWriter.writeDbEnvFile(rootProject, projectConfig)
            val httpEnvFile = EnvWriter.httpEnvFile(rootProject, projectConfig)

            /**
             * Open created files.
             * Focus backend env file
             */
            withContext(Dispatchers.Main){
                httpEnvFile?.openInEditor(project)
                dbEnvFile?.openInEditor(project)
                mainEnvFile?.openInEditor(project, true)
            }
        }
    }


}