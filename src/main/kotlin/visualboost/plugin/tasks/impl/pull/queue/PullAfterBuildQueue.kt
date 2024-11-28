package visualboost.plugin.tasks.impl.pull.queue

import com.ibm.icu.text.SimpleDateFormat
import com.intellij.openapi.project.Project
import visualboost.plugin.tasks.abs.AsyncTaskQueue
import visualboost.plugin.tasks.abs.SuspendingTask
import visualboost.plugin.tasks.abs.TaskQueueEventHandler
import visualboost.plugin.tasks.impl.pull.tasks.AddAuthTokenToEnvironmentTask
import visualboost.plugin.tasks.impl.pull.tasks.CreateAuthServiceRunConfigurationsTask
import visualboost.plugin.tasks.impl.pull.tasks.GetAuthenticationServiceConfigTask
import visualboost.plugin.tasks.impl.pull.tasks.SetLinuxLineSeparatorsTask
import visualboost.plugin.tasks.impl.pull.tasks.git.FetchVersionTask
import visualboost.plugin.tasks.impl.pull.tasks.git.PullVersionTask
import visualboost.plugin.util.showError
import visualboost.plugin.util.showInfo
import java.util.*

class PullAfterBuildQueue(
    project: Project,
    val version: String,
    val branchToPull: String
) : AsyncTaskQueue(project) {

    init {
        initLogger()
        initEventHandler()
        registerTasks()
    }

    private fun initLogger() {
        val sf = SimpleDateFormat("YYYY-MM-dd")
        logger.setLogFile("vb_pull_${sf.format(Date())}.log")
        logger.clearLogFile()
    }

    private fun initEventHandler() {
        eventHandler = object : TaskQueueEventHandler {
            override fun beforeTaskExecution(task: SuspendingTask) {
                logger.debug("Start", task::class.java)
            }

            override fun afterTaskExecution(task: SuspendingTask) {
                logger.debug("Finished", task::class.java)
            }

            override fun onError(task: SuspendingTask, exception: Exception) {
                logger.error("Failed", task::class.java)
                logger.error(exception, task::class.java)
                project.showError("Error", exception.message ?: "")
            }

            override fun onSuccess() {
                project.showInfo(
                    "Updated",
                    "New version $version pulled."
                )
            }
        }
    }

    private fun registerTasks() {
        //Step 1: Pull OR Fetch new content
        register(PullVersionTask(version, branchToPull))
        register(FetchVersionTask(version, branchToPull))

        //Step 2: Get Auth service config
        register(GetAuthenticationServiceConfigTask())

        //Step 3: If auth service is enabled, add AUTH_TOKEN to .env file
        register(AddAuthTokenToEnvironmentTask())

        //Step 4: set line separators of auth service script files
        register(SetLinuxLineSeparatorsTask())

        //Step 5:
        register(CreateAuthServiceRunConfigurationsTask())

        //Step 6: Add .env to .gitignore and commit changes
        //Step 7: Stop tracking ./service/authentication/.env
    }


}