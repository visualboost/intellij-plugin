package visualboost.plugin.tasks.impl.project.tasks

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import visualboost.plugin.Logger
import visualboost.plugin.api.API
import visualboost.plugin.api.models.project.VisualBoostProjectResponseBody
import visualboost.plugin.intellijProject.VBProjectGenerationSettings
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.util.EnvWriter
import visualboost.plugin.util.ProjectDirectories.getProjectDir

class InitEnvTask(val settings: VBProjectGenerationSettings): SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        val projectDir = getProjectDir(project)

        indicator.text = "Setup environment variables"
        val jwt = settings.getVbTokenOrThrowException()

        val vbProject = (resultOfPreexecutesTasks[CreateVBProjectTask::class.java] as? VisualBoostProjectResponseBody.Data.VisualBoostProject) ?: throw NullPointerException("Missing VB project")

        val projectConfig = withContext(Dispatchers.IO) {
            return@withContext API.getProjectConfig(jwt, vbProject._id)
        }

        val mainEnvFile = EnvWriter.writeMainEnvFile(projectDir, projectConfig)
        if (mainEnvFile == null) {
            indicator.text2 = """Skipped: "./backend/.env" already exist."""
            delay(500)
        }

        val dbEnvFile = EnvWriter.writeDbEnvFile(projectDir, projectConfig)
        if (dbEnvFile == null) {
            if (mainEnvFile == null) {
                indicator.text2 = """Skipped: "./db/.env" already exist."""
                delay(500)
            }
        }

        val httpEnvFile = EnvWriter.httpEnvFile(projectDir, projectConfig)
        if (httpEnvFile == null) {
            indicator.text2 = """Skipped: "./backend/src/http/http-client.env.json" already exist."""
            delay(500)
        }

        indicator.text2 = null
        return null
    }


}