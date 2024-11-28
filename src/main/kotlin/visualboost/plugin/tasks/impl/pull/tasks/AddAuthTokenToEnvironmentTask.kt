package visualboost.plugin.tasks.impl.pull.tasks

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import visualboost.plugin.Logger
import visualboost.plugin.api.models.project.AuthenticationServiceConfigResponseBody
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.util.ProjectDirectories
import visualboost.plugin.util.ProjectDirectories.getProjectDir
import java.nio.file.Paths

class AddAuthTokenToEnvironmentTask() : SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        indicator.text = "Add AUTH_TOKEN to ./backend/.env file"

        val projectDir = getProjectDir(project)!!
        val backendEnvironmentFile = Paths.get(projectDir.absolutePath, "backend", ".env").toFile()

        if (!backendEnvironmentFile.exists()) {
            throw NullPointerException("Missing ./backend/.env")
        }

        val authTokenFromEnvFile = backendEnvironmentFile.readLines().find {
            it.contains("AUTH_TOKEN=")
        }

        if (authTokenFromEnvFile != null) {
            logger.debug("AUTH_TOKEN already exists in .env file ($authTokenFromEnvFile). Skip adding it again.")
            return null
        }

        val authServiceEnvFile = ProjectDirectories.getAuthServiceEnvFile(project)
        if (!authServiceEnvFile.exists()) {
            throw NullPointerException("Missing ./service/authentication/.env")
        }

        val authTokenSecretEntry = authServiceEnvFile.readLines().find { it.contains("AUTH_TOKEN_SECRET=") } ?: throw NullPointerException("Missing variable AUTH_TOKEN_SECRET")
        val authToken = authTokenSecretEntry.split("=")[1].trim()

        backendEnvironmentFile.appendText("\nAUTH_TOKEN=$authToken")
        return null
    }

    override fun skipTask(
        project: Project,
        indicator: ProgressIndicator,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Boolean {
        val authServiceConfig =
            resultOfPreexecutesTasks[GetAuthenticationServiceConfigTask::class.java] as? AuthenticationServiceConfigResponseBody.Data.AuthenticationServiceConfig
                ?: throw NullPointerException("Missing authentication service config")
        return !authServiceConfig.isEnabled
    }
}