package visualboost.plugin.tasks.impl.project.tasks.git

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import visualboost.plugin.Logger
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.util.pull

class PullGitRepoTask: SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        indicator.text = "Pull main branch"
        project.pull("main")

        return null
    }


}