package visualboost.plugin.tasks.abs

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import visualboost.plugin.Logger

abstract class SuspendingTask{

    abstract suspend fun execute(project: Project, indicator: ProgressIndicator, logger: Logger, resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>): Any?

}