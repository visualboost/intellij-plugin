package visualboost.plugin.tasks.abs

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking

abstract class SequentialTaskQueue(project: Project, val indicator: ProgressIndicator) :
    TaskQueue<SequentialTask>(project) {

    val results: MutableMap<Class<*>, Any?> = mutableMapOf()
    var eventHandler: TaskQueueEventHandler? = null

    override fun execute() {

        runBlocking {

            tasks.forEach {
                try {
                    eventHandler?.beforeTaskExecution(it)
                    if(it.skipTask(project, indicator, results)) return@forEach

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

    override fun register(task: SequentialTask) {
        super.register(task)
        results[task::class.java] = null
    }



}