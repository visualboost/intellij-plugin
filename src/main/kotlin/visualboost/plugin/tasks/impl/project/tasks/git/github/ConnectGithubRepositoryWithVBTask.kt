package visualboost.plugin.tasks.impl.project.tasks.git.github

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import visualboost.plugin.Logger
import visualboost.plugin.api.API
import visualboost.plugin.api.models.intellij.project.github.GithubRepository
import visualboost.plugin.api.models.project.VisualBoostProjectResponseBody
import visualboost.plugin.api.models.project.git.GitRepositoryType
import visualboost.plugin.intellijProject.VBProjectGenerationSettings
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.tasks.impl.project.tasks.CreateVBProjectTask

class ConnectGithubRepositoryWithVBTask(val settings: VBProjectGenerationSettings) : SequentialTask() {
    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): GithubRepository {
        indicator.text = "Connect repository with VisualBoost"

        val vbProject =
            (resultOfPreexecutesTasks[CreateVBProjectTask::class.java] as? VisualBoostProjectResponseBody.Data.VisualBoostProject)
                ?: throw NullPointerException("Missing VB project")

        val githubRepository = (resultOfPreexecutesTasks[CreateGithubRepositoryTask::class.java]
            ?: resultOfPreexecutesTasks[GetExistingGithubRepositoryTask::class.java]) as? GithubRepository
        if (githubRepository == null) {
            throw NullPointerException("Missing Github repository")
        }

        if (githubRepository.url == null) {
            throw NullPointerException("Missing Github repository URL")
        }

        val githubData = settings.githubData!!

        withContext(Dispatchers.IO) {
            API.connectGitRepository(
                settings.getVbTokenOrThrowException(),
                vbProject._id,
                GitRepositoryType.TARGET,
                githubRepository.url,
                githubData.githubUsername,
                githubData.githubEmail,
                githubData.githubAccessToken
            )
        }

        return githubRepository
    }


}