package visualboost.plugin.tasks.impl.pull.tasks.git

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import visualboost.plugin.Logger
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.util.currentBranch
import visualboost.plugin.util.fetch

class FetchVersionTask(val version: String, val branch: String): SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        indicator.text = "Fetch branch $branch (Version: ${version}). Ceckout branch $branch to access the new content."
        project.fetch(branch)
        return null
    }

    /**
     * Skip this task because we can pull the new content if the branch from the vb settings and the current local branch are equal
     */
    override fun skipTask(project: Project, indicator: ProgressIndicator,resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>): Boolean {
        return project.currentBranch() == branch
    }


}