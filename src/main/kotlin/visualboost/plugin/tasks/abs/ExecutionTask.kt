package visualboost.plugin.tasks.abs

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project

abstract class ExecutionTask{

    abstract fun execute(project: Project, indicator: ProgressIndicator): Any?

}