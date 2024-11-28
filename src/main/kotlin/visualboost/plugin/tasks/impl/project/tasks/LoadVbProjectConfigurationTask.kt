package visualboost.plugin.tasks.impl.project.tasks

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import visualboost.plugin.Logger
import visualboost.plugin.tasks.abs.SequentialTask

class LoadVbProjectConfigurationTask: SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        runInEdt {
            val action =
                ActionManager.getInstance().getAction("visualboost.plugin.actions.LoadProjectConfigurationAction")
            ActionManager.getInstance().tryToExecute(action, null, null, null, false)
        }

        return null
    }


}