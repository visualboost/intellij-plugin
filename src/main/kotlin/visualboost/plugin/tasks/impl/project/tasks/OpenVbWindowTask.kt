package visualboost.plugin.tasks.impl.project.tasks

import com.intellij.openapi.application.EDT
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import visualboost.plugin.Logger
import visualboost.plugin.tasks.abs.SequentialTask

class OpenVbWindowTask(): SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        withContext(Dispatchers.EDT) {
            ToolWindowManager.getInstance(project).getToolWindow("VisualBoost")?.show()
        }

        return null
    }


}