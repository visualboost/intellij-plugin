package visualboost.plugin.tasks.impl.project.tasks

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import setLineSeparatorToLF
import visualboost.plugin.Logger
import visualboost.plugin.intellijProject.VBProjectGenerationSettings
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.util.ProjectDirectories.getProjectDir
import java.nio.file.Paths

/**
 * The line separators of script (.sh) files can be converted by Git (depending on the settings).
 * If Git converts Linux line separators ('\n') into Windows line separators ('\r\n') the script files will fail.
 *
 * This task converts all line separators into Linux line separators (LF).
 */
class SetLinuxLineSeparatorsTask(val settings: VBProjectGenerationSettings): SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        indicator.text = "Adapt line separators of db config files"

        val projectDir = getProjectDir(project)!!
        val initReplicaKeyScriptFile = Paths.get(projectDir.absolutePath, "db", ".scripts", "init_replica_key.sh")
        initReplicaKeyScriptFile.toFile().setLineSeparatorToLF()

        val initReplicasetScriptKey = Paths.get(projectDir.absolutePath, "db", ".scripts", "init_replicaset.sh")
        initReplicasetScriptKey.toFile().setLineSeparatorToLF()

        return null
    }


}