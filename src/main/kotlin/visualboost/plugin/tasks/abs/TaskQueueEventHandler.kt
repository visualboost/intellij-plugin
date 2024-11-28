package visualboost.plugin.tasks.abs

import java.lang.Exception

interface TaskQueueEventHandler {

    fun beforeTaskExecution(task: SuspendingTask)
    fun afterTaskExecution(task: SuspendingTask)
    fun onError(task: SuspendingTask, exception: Exception)
    fun onSuccess()

}