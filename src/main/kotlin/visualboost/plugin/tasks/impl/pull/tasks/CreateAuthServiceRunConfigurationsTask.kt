package visualboost.plugin.tasks.impl.pull.tasks

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import visualboost.plugin.Logger
import visualboost.plugin.api.models.project.AuthenticationServiceConfigResponseBody
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.util.*

class CreateAuthServiceRunConfigurationsTask : SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): RunnerAndConfigurationSettings? {
        val composeFile = ProjectDirectories.getAuthServiceComposeFile(project)
        val dotEnvFile = ProjectDirectories.getAuthServiceEnvFile(project)

        if(!composeFile.exists()) {
            project.showInfo("Attention", "Missing ./service/authentication/docker-compose.yml. Skip create run configuration.")
            return null
        }

        if(!dotEnvFile.exists()) {
            project.showInfo("Attention", "Missing ./service/authentication/.env. Skip create run configuration.")
            return null
        }

        return project.createDockerConfiguration(project.getStartAuthServiceConfigName(), composeFile, dotEnvFile)
    }

    /**
     * Skip task if run config does already exist
     */
    override fun skipTask(
        project: Project,
        indicator: ProgressIndicator,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Boolean {
        val authServiceConfig =
            resultOfPreexecutesTasks[GetAuthenticationServiceConfigTask::class.java] as? AuthenticationServiceConfigResponseBody.Data.AuthenticationServiceConfig
                ?: throw NullPointerException("Missing authentication service config")
        if(!authServiceConfig.isEnabled) return false

        val runAuthServiceRunConfig = project.getDockerRunConfiguration(project.getStartAuthServiceConfigName())
        return runAuthServiceRunConfig != null
    }
}