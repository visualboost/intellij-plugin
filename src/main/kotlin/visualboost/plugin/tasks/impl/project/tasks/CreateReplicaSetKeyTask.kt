package visualboost.plugin.tasks.impl.project.tasks

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.delay
import visualboost.plugin.Logger
import visualboost.plugin.dialog.InitProjectDialog.Companion.MONGO_REPLICA_KEY_FILE
import visualboost.plugin.tasks.abs.SequentialTask
import visualboost.plugin.util.ProjectDirectories.getProjectDir
import visualboost.plugin.util.random
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

class CreateReplicaSetKeyTask() : SequentialTask() {

    override suspend fun execute(
        project: Project,
        indicator: ProgressIndicator,
        logger: Logger,
        resultOfPreexecutesTasks: MutableMap<Class<*>, Any?>
    ): Any? {
        indicator.text = "Create key file for replica set"

        val projectDir = getProjectDir(project) ?: throw NullPointerException("Missing project path")
        val dbDir = Paths.get(projectDir.absolutePath, "db")
        val replicaKeyFile = Paths.get(dbDir.absolutePathString(), "key", MONGO_REPLICA_KEY_FILE).toFile()

        if (replicaKeyFile.exists()) {
            indicator.text2 = """Skipped: "${replicaKeyFile.invariantSeparatorsPath}" already exist."""
            delay(1000)
        } else {
            createReplicaSetKeyFile(project)
        }

        //Refresh db dir
        val virtualSourceDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dbDir.toFile())
        virtualSourceDir?.refresh(true, true)

        indicator.text2 = null
        return null
    }

    private fun createReplicaSetKeyFile(project: Project) {
        val projectDir = getProjectDir(project)
        val keyDir = Paths.get(projectDir.absolutePath, "db", "key")
        if(!keyDir.exists()){
            keyDir.toFile().mkdirs()
        }
        val replicaKeyFile = Paths.get(keyDir.absolutePathString(), MONGO_REPLICA_KEY_FILE).toFile()
        val replicaKey = createReplicaSetKey()

        replicaKeyFile.writeText(replicaKey)
    }

    private fun createReplicaSetKey(): String {
        val randomString = String.random()
        val replicaKey = Base64.getEncoder().encodeToString(randomString.toByteArray()).substring(0, 756)
        return replicaKey
    }

}