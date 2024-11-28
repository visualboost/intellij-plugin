package visualboost.plugin.tasks.impl.pull.tasks.git

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import visualboost.plugin.Logger
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.util.currentBranch
import visualboost.plugin.util.pull
import java.io.File

class PullVersionTask(val version: String, val branch: String): SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        indicator.text = "Pull branch $branch (Version: ${version})"
        project.pull(branch)

        indicator.text = "Refresh local files ..."
        refreshSourceDir(project)

        return null
    }

    private fun refreshSourceDir(project: Project) {
        val sourceDir = getSourceDir(project) ?: return

        val virtualSourceDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(sourceDir) ?: return
        virtualSourceDir.refresh(true, true)
    }

    private fun getSourceDir(project: Project): File? {
        val rootDir = getRootDir(project) ?: return null
        val walk = rootDir.walk(FileWalkDirection.TOP_DOWN)
        return walk.onEnter { it.name != "node_modules" }.find {
            it.isDirectory && it.name == "src"
        }
    }

    fun getRootDir(project: Project): File? {
        val contentRoots = ProjectRootManager.getInstance(project).contentRoots
        val rootDirAsVf = contentRoots.firstOrNull() ?: return null
        return File(rootDirAsVf.path)
    }

    /**
     * Do not pull if the current branch is not equal to the branch set in VisualBoost
     */
    override fun skipTask(project: Project, indicator: ProgressIndicator, resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>): Boolean {
        return project.currentBranch() != branch
    }


}