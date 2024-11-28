package visualboost.plugin.tasks.abs

import com.intellij.openapi.project.Project
import visualboost.plugin.Logger

abstract class TaskQueue<T>(val project: Project, val tasks: MutableList<T> = mutableListOf()) {

    val logger = Logger()

    abstract fun execute()

    open fun register(task: T){
        tasks.add(task)
    }

    open fun register(tasks: List<T>){
        this.tasks.forEach { register(it) }
    }

}