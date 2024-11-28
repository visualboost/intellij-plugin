package visualboost.plugin.tasks.impl.project.tasks

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import visualboost.plugin.Logger
import visualboost.plugin.api.API
import visualboost.plugin.api.models.project.VisualBoostProjectResponseBody
import visualboost.plugin.intellijProject.VBProjectGenerationSettings
import visualboost.plugin.models.GenerationTarget
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.util.CredentialUtil

class CreateVBProjectTask(val settings: VBProjectGenerationSettings): SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): VisualBoostProjectResponseBody.Data.VisualBoostProject {
        indicator.text = "Create new Project"

        val jwt = settings.getVbTokenOrThrowException()
        val projectName = settings.getProjectnameOrThrowException()

        val newVbProject = withContext(Dispatchers.IO) {
            API.createNewVisualBoostProject(jwt, projectName)
        }

        //Store credentials
        CredentialUtil.storeVisualBoostCredentials(settings.userData!!.email, settings.userData!!.password)
        val ideProjectSettings = VbProjectSettings.getInstance(project)

        //Set vb project id and target for the ide settings
        ideProjectSettings.projectId = newVbProject._id
        ideProjectSettings.target = GenerationTarget.SERVER

        return newVbProject
    }


}