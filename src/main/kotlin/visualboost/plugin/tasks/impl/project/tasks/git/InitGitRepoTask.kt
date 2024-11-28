package visualboost.plugin.tasks.impl.project.tasks.git

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import visualboost.plugin.Logger
import visualboost.plugin.api.models.intellij.project.github.GithubRepository
import visualboost.plugin.intellijProject.VBProjectGenerationSettings
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.tasks.impl.project.tasks.git.github.ConnectGithubRepositoryWithVBTask
import visualboost.plugin.util.addGitOrigin
import visualboost.plugin.util.initGitRepository
import java.io.File

class InitGitRepoTask(val settings: VBProjectGenerationSettings) : SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        indicator.text = "Init local repository ..."
        project.initGitRepository()

        indicator.text = "Add git origin ..."

        val githubRepository =
            (resultOfPreexecutesTasks[ConnectGithubRepositoryWithVBTask::class.java] as? GithubRepository)
                ?: throw NullPointerException("Missing Github repository")
        val githubUrl = githubRepository.url ?: throw NullPointerException("Missing Github repository URL")
        val githubAccessToken =
            settings.githubData?.githubAccessToken ?: throw NullPointerException("Missing access token (Github)")
        val githubRepositoryUrl = "https://${githubAccessToken}@${githubUrl.removePrefix("https://")}"
        project.addGitOrigin(githubRepositoryUrl)

        indicator.text = "Refresh project ..."
        val virtualSourceDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(project.basePath!!))
        virtualSourceDir?.refresh(false, false)

        return null
    }

}