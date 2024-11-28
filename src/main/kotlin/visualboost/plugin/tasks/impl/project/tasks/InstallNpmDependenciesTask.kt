package visualboost.plugin.tasks.impl.project.tasks

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.delay
import visualboost.plugin.Logger
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.util.ProjectDirectories.getProjectDir
import java.nio.file.Paths
import java.util.*

class InstallNpmDependenciesTask() : SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        indicator.text = "Install npm dependencies"

        val projectDir = getProjectDir(project)!!
        val backendDir = Paths.get(projectDir.absolutePath, "backend")

        val isWindows = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win");
        val npmCommand = if (isWindows) "npm.cmd" else "npm"

        val processBuilder = ProcessBuilder(npmCommand, "install")
        processBuilder.directory(backendDir.toFile())

        val result = processBuilder.start().waitFor()
        if (result != 0) {
            indicator.text2 = "Failed to install NPM dependencies."
            delay(1000)
        }

        //Refresh backendDir dir
        val virtualSourceDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(backendDir.toFile())
        virtualSourceDir?.refresh(true, false)

        return null
    }


}