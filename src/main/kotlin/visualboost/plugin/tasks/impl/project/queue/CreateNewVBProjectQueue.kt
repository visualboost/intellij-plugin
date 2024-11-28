package visualboost.plugin.tasks.impl.project.queue

import com.ibm.icu.text.SimpleDateFormat
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import visualboost.plugin.Logger
import visualboost.plugin.intellijProject.VBProjectGenerationSettings
import visualboost.plugin.tasks.abs.SequentialTaskQueue
import visualboost.plugin.tasks.abs.TaskQueueEventHandler
import visualboost.plugin.tasks.abs.SuspendingTask
import visualboost.plugin.tasks.impl.project.tasks.*
import visualboost.plugin.tasks.impl.project.tasks.git.InitGitRepoTask
import visualboost.plugin.tasks.impl.project.tasks.git.PullGitRepoTask
import visualboost.plugin.tasks.impl.project.tasks.git.github.ConnectGithubRepositoryWithVBTask
import visualboost.plugin.tasks.impl.project.tasks.git.github.CreateGithubRepositoryTask
import visualboost.plugin.tasks.impl.project.tasks.git.github.GetExistingGithubRepositoryTask
import visualboost.plugin.util.showError
import java.lang.Exception
import java.util.*

class CreateNewVBProjectQueue(
    project: Project,
    indicator: ProgressIndicator,
    val settings: VBProjectGenerationSettings,
    createNewGithubRepo: Boolean,
    useExistingGithubRepo: Boolean
) : SequentialTaskQueue(project, indicator) {

    init {
        initLogger()
        initEventHandler()
        registerTasks(createNewGithubRepo, useExistingGithubRepo)
    }

    private fun initLogger() {
        val sf = SimpleDateFormat("YYYY-MM-dd_hh-mm-ss")
        logger.setLogFile("vb_new_project${sf.format(Date())}.log")
        logger.clearLogFile()
    }

    private fun initEventHandler(){
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
            }
        }
    }

    private fun registerTasks(createNewGithubRepo: Boolean, useExistingGithubRepo: Boolean) {
        //Step 1: Create VB Project
        register(CreateVBProjectTask(settings))

        //Step 2: Create new github repo if user selected this option OR use an existing one
        register(CreateGithubRepositoryTask(settings, createNewGithubRepo))
        register(GetExistingGithubRepositoryTask(settings, useExistingGithubRepo))

        //Step 3: Connect the github repository with the created visualboost project
        register(ConnectGithubRepositoryWithVBTask(settings))

        //Step 4: Run an init build in VB, after connection the VB project is connected with the github repository
        register(RunVBInitTask(settings))

        //Step 5: Initialize local git repository and connect it with origin
        register(InitGitRepoTask(settings))

        //Step 6: Pull initialized project
        register(PullGitRepoTask())

        //Step 7: Initialize environment variables
        //TODO: add AUTH_TOKEN if auth service is enabled
        register(InitEnvTask(settings))

        //Step 8: Create replica set key file if it does not already exist
        register(CreateReplicaSetKeyTask())

        //Step 9: Convert the line separators of the script files to LF (Linux)
        //TODO: set line separators of auth service script files
        register(SetLinuxLineSeparatorsTask(settings))

        //Step 10: Install NPM dependencies
        register(InstallNpmDependenciesTask())

        //Step 11: Create Run configurations
        //TODO: Add run configuration for auth service
        register(CreateRunConfigurationsTask())

        //Step 12: Open the VB tool window
        register(OpenVbWindowTask())

        //Step 13: Show setup
        register(ShowSetupFinishedDialogTask())

        //Step 14: Load VB project configuration
        register(LoadVbProjectConfigurationTask())
    }


}