package visualboost.plugin.tasks.impl.project.tasks.git.github

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import visualboost.plugin.Logger
import visualboost.plugin.api.API
import visualboost.plugin.intellijProject.VBProjectGenerationSettings
import visualboost.plugin.tasks.abs.SequentialTask

class CreateGithubRepositoryTask(val settings: VBProjectGenerationSettings, val taskIsEnabled: Boolean) :
    SequentialTask() {

        override suspend fun execute(
            project: Project,
            indicator: ProgressIndicator, logger: Logger,

            resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        if (!taskIsEnabled) return null

        val repoName = settings.getGithubRepoNameOrThrowException()
        indicator.text = """Create new Github repository: "$repoName" ..."""

        val createdGithubRepository = withContext(Dispatchers.IO) {
            API.Github.createGithubRepository(
                settings.getVbTokenOrThrowException(),
                settings.getGithubAccessTokenOrThrowException(),
                repoName
            )
        }

        return createdGithubRepository
    }


}