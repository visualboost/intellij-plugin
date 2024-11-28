package visualboost.plugin.tasks.impl.pull.tasks

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import visualboost.plugin.Logger
import visualboost.plugin.api.API
import visualboost.plugin.api.models.project.AuthenticationServiceConfigResponseBody
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.tasks.abs.SequentialTask

class GetAuthenticationServiceConfigTask() : SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): AuthenticationServiceConfigResponseBody.Data.AuthenticationServiceConfig {
        indicator.text = "Fetch authentication service configuration ..."

        val projectId =
            VbProjectSettings.getInstance(project).projectId ?: throw NullPointerException("Missing projectId")

        val authServiceConfig = withContext(Dispatchers.IO) {
            API.getAuthenticationServiceConfig(projectId)
        }

        return authServiceConfig
    }
}