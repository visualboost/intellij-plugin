package visualboost.plugin.tasks.impl.project.tasks.git.github

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import visualboost.plugin.Logger
import visualboost.plugin.intellijProject.VBProjectGenerationSettings
import visualboost.plugin.tasks.abs.SequentialTask

class GetExistingGithubRepositoryTask(val settings: VBProjectGenerationSettings, val taskIsEnabled: Boolean) :
    SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        if (!taskIsEnabled) return null

        val githubRepository = settings.getGithubRepoOrThrowException()
        indicator.text = """Take existing Github repository: "${githubRepository.name}" ..."""

        return githubRepository
    }


}