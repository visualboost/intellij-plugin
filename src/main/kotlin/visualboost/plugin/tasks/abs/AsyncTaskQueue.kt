package visualboost.plugin.tasks.abs

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import visualboost.plugin.Logger
import visualboost.plugin.util.showError

open class AsyncTaskQueue(project: Project) :
    TaskQueue<SequentialTask>(project) {

    val results: MutableMap<Class<*>, Any?> = mutableMapOf()
    var eventHandler: TaskQueueEventHandler? = null

    override fun execute() {

        val backgroundTask = object : Task.Backgroundable(project, "", true) {
            override fun run(indicator: ProgressIndicator) {

                runBlocking {

                    tasks.forEach {
                        try {
                            eventHandler?.beforeTaskExecution(it)
                            if (it.skipTask(project, indicator, results)) return@forEach

                            results[it::class.java] = it.execute(project, indicator, logger, results)
                            eventHandler?.afterTaskExecution(it)
                        } catch (e: Exception) {
                            eventHandler?.onError(it, e)
                            return@runBlocking
                        }
                    }

                    eventHandler?.onSuccess()
                }
            }
        }

        val progressWindow = ProgressWindow(false, true, project)
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(backgroundTask, progressWindow)

    }

    override fun register(task: SequentialTask) {
        super.register(task)
        results[task::class.java] = null
    }

    open fun register(execute: (project: Project, indicator: ProgressIndicator, logger: Logger, resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>) -> Any?) {
        register(object : SequentialTask() {
            override suspend fun execute(
                project: Project,
                indicator: ProgressIndicator,
                logger: Logger,
                resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
            ): Any? {
                return execute(project, indicator, logger, resultOfPreexecutesTasks)
            }
        })
    }

    fun useDefaultEventHandler(): AsyncTaskQueue {
        logger.useDefaultLogFile()

        eventHandler = object : TaskQueueEventHandler {
            override fun beforeTaskExecution(task: SuspendingTask) {
                logger.debug("Start", task::class.java)
            }

            override fun afterTaskExecution(task: SuspendingTask) {
                logger.debug("Finished", task::class.java)
            }

            override fun onError(task: SuspendingTask, exception: java.lang.Exception) {
                logger.error("Failed", task::class.java)
                logger.error(exception, task::class.java)
            }

            override fun onSuccess() {
            }
        }

        return this
    }


}