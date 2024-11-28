package visualboost.plugin.tasks.impl.pull.tasks

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import setLineSeparatorToLF
import visualboost.plugin.Logger
import visualboost.plugin.api.models.project.AuthenticationServiceConfigResponseBody
import visualboost.plugin.intellijProject.VBProjectGenerationSettings
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.util.ProjectDirectories
import visualboost.plugin.util.ProjectDirectories.getProjectDir
import java.nio.file.Paths

/**
 * The line separators of script (.sh) files can be converted by Git (depending on the settings).
 * If Git converts Linux line separators ('\n') into Windows line separators ('\r\n') the script files will fail.
 *
 * This task converts all line separators into Linux line separators (LF).
 */
class SetLinuxLineSeparatorsTask(): SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        indicator.text = "Convert line separators to LF in script files"

        val initReplicaKeyScriptFile = Paths.get(ProjectDirectories.getAuthServiceDir(project).absolutePath ,".scripts", "init_replica_key.sh")
        initReplicaKeyScriptFile.toFile().setLineSeparatorToLF()

        val initReplicasetScriptKey = Paths.get(ProjectDirectories.getAuthServiceDir(project).absolutePath,".scripts", "init_replicaset.sh")
        initReplicasetScriptKey.toFile().setLineSeparatorToLF()

        return null
    }

    override fun skipTask(
        project: Project,
        indicator: ProgressIndicator,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Boolean {
        val authServiceConfig =
            resultOfPreexecutesTasks[GetAuthenticationServiceConfigTask::class.java] as? AuthenticationServiceConfigResponseBody.Data.AuthenticationServiceConfig
                ?: throw NullPointerException("Missing authentication service config")
        return !authServiceConfig.isEnabled
    }


}