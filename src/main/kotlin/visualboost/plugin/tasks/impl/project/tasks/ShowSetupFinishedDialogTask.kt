package visualboost.plugin.tasks.impl.project.tasks

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import visualboost.plugin.Logger
import visualboost.plugin.api.models.intellij.project.github.GithubRepository
import visualboost.plugin.api.models.project.VisualBoostProjectResponseBody
import visualboost.plugin.dialog.ProjectSetupDialog
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.tasks.impl.project.tasks.git.github.ConnectGithubRepositoryWithVBTask

class ShowSetupFinishedDialogTask(): SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        val vbProject = (resultOfPreexecutesTasks[CreateVBProjectTask::class.java] as? VisualBoostProjectResponseBody.Data.VisualBoostProject) ?: throw NullPointerException("Missing VB project")
        val githubRepository =
            (resultOfPreexecutesTasks[ConnectGithubRepositoryWithVBTask::class.java] as? GithubRepository)
                ?: throw NullPointerException("Missing Github repository")

        runInEdt {
            val projectSetupDialog =
                ProjectSetupDialog(project, project.name, vbProject._id, githubRepository.url!!)
            projectSetupDialog.show()
        }

        return null
    }


}