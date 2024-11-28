package visualboost.plugin.tasks.impl.project.tasks

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import visualboost.plugin.Logger
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.util.*

class CreateRunConfigurationsTask : SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        indicator.text = "Create run configurations"

        project.createNpmRunConfig(project.getStartApplicationOnceRunConfigName(), "start")
        project.createNpmRunConfig(project.getStartApplicationInDevModeRunConfigName(), "dev")
        createRunDbConfig(project)

        return null
    }

    private fun createRunDbConfig(project: Project){
        val dockerComposeFile = ProjectDirectories.getDbComposeFile(project) ?: throw NullPointerException("./db/docker-compose.yml does not exist")
        val envFile = ProjectDirectories.getDbEnvFile(project) ?: throw NullPointerException("./db/.env does not exist")
        project.createDockerConfiguration(project.getStartDatabaseConfigurationName(), dockerComposeFile, envFile)
    }


}