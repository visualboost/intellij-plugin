package visualboost.plugin.tasks.abs

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project

abstract class SequentialTask : SuspendingTask(){

    open fun skipTask(project: Project, indicator: ProgressIndicator,resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>): Boolean{
        return false
    }
}
