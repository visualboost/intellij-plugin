package visualboost.plugin.tasks.impl.project.tasks

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import visualboost.plugin.Logger
import visualboost.plugin.api.API
import visualboost.plugin.api.models.project.VisualBoostProjectResponseBody
import visualboost.plugin.intellijProject.VBProjectGenerationSettings
import visualboost.plugin.tasks.abs.SequentialTask

class RunVBInitTask(val settings: VBProjectGenerationSettings): SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        indicator.text = "Initialize Project ..."

        val vbProject = (resultOfPreexecutesTasks[CreateVBProjectTask::class.java] as? VisualBoostProjectResponseBody.Data.VisualBoostProject) ?: throw NullPointerException("Missing VB project")

        val projectName = settings.getProjectnameOrThrowException()
        val jwt = settings.getVbTokenOrThrowException()

        withContext(Dispatchers.IO) {
            API.Build.executeBackendBuildProcessAndWait(jwt, vbProject._id, """Init project "$projectName""")
        }

        return null
    }


}