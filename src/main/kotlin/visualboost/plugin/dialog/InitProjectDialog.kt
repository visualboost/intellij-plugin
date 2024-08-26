package visualboost.plugin.dialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.AnimatedIcon
import create
import kotlinx.coroutines.*
import setLineSeparatorToLF
import visualboost.plugin.api.API
import visualboost.plugin.settings.VbAppSettings
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.util.EnvWriter
import visualboost.plugin.util.random
import java.io.File
import java.nio.file.Paths
import java.util.*
import javax.swing.*
import kotlin.io.path.absolutePathString


class InitProjectDialog(val project: Project, val rootDir: File) : DialogWrapper(null, true) {

    companion object {
        const val MONGO_REPLICA_KEY_FILE = "mongo_replica_key"
    }

    val backendDir = Paths.get(rootDir.absolutePath, "backend")
    val dbDir = Paths.get(rootDir.absolutePath, "db")

    var backendEnvCreated = false
    var dbEnvCreated = false
    var httpEnvCreated = false
    var replicaKeyCreated = false
    var lineSeparatorsSet = false
    var dependenciesInstalled = false

    val createMainEnvLabel = JLabel("Create ./backend/.env", AnimatedIcon.Default(), SwingConstants.LEFT)
    val createDbEnvLabel = JLabel("Create ./db/.env", AnimatedIcon.Default(), SwingConstants.LEFT)
    val createHttpEnvLabel =
        JLabel("Create ./backend/http/http-client.env.json", AnimatedIcon.Default(), SwingConstants.LEFT)
    val createReplicaKeyFileLabel =
        JLabel("Create Key-File for ReplicaSet", AnimatedIcon.Default(), SwingConstants.LEFT)
    val setLineSeparatorsLabel = JLabel("Set Line Separators", AnimatedIcon.Default(), SwingConstants.LEFT)
    val dependenciesInstalledLabel = JLabel("Install NPM dependencies", AnimatedIcon.Default(), SwingConstants.LEFT)

    init {
        title = "Initialize Project"
        init()

        okAction.isEnabled = false

        /*
         * 1. Create environment files
         * 2. Create replica key file
         * 3. Adapt line seprators
         * 4. Install npm dependencies
         */
        object : Task.Backgroundable(null, "Initialize Project", false) {
            override fun run(indicator: ProgressIndicator) {
                createEnvFiles()
                createReplicaSetKey(rootDir)
                setLinuxLineSeparatorsForInitScripts(rootDir)

                installNpmDependencies()

                updateOkButton()
            }
        }.queue()


    }

    private fun showAttention(label: JLabel, msg: String) {
        label.icon = AllIcons.General.Warning
        label.text = msg
        label.updateUI()
    }

    private fun showError(label: JLabel, msg: String) {
        label.icon = AllIcons.General.Error
        label.text = msg
        label.updateUI()
    }

    fun showMainEnvCreated() {
        backendEnvCreated = true
        createMainEnvLabel.icon = AllIcons.General.InspectionsOK
    }

    fun showDbEnvCreated() {
        dbEnvCreated = true
        createDbEnvLabel.icon = AllIcons.General.InspectionsOK
    }

    fun showHttpEnvCreated() {
        httpEnvCreated = true
        createHttpEnvLabel.icon = AllIcons.General.InspectionsOK
    }

    fun showNPMDepencenciesInstalled() {
        dependenciesInstalled = true
        dependenciesInstalledLabel.icon = AllIcons.General.InspectionsOK
    }

    fun replicaKeyFileCreated() {
        replicaKeyCreated = true
        createReplicaKeyFileLabel.icon = AllIcons.General.InspectionsOK
    }

    fun lineSeparatorsSet() {
        lineSeparatorsSet = true
        setLineSeparatorsLabel.icon = AllIcons.General.InspectionsOK
    }

    fun updateOkButton() {
        this.okAction.isEnabled =
            backendEnvCreated && dbEnvCreated && httpEnvCreated && replicaKeyCreated && lineSeparatorsSet && dependenciesInstalled
    }

    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel()
        dialogPanel.layout = BoxLayout(dialogPanel, BoxLayout.Y_AXIS)

        dialogPanel.add(createMainEnvLabel)
        dialogPanel.add(Box.createVerticalStrut(10))
        dialogPanel.add(createDbEnvLabel)
        dialogPanel.add(Box.createVerticalStrut(10))
        dialogPanel.add(createHttpEnvLabel)
        dialogPanel.add(Box.createVerticalStrut(10))
        dialogPanel.add(createReplicaKeyFileLabel)
        dialogPanel.add(Box.createVerticalStrut(10))
        dialogPanel.add(setLineSeparatorsLabel)
        dialogPanel.add(Box.createVerticalStrut(10))
        dialogPanel.add(dependenciesInstalledLabel)

        return dialogPanel
    }

    private fun setLinuxLineSeparatorsForInitScripts(rootDir: File) {
        val initReplicaKeyScriptFile = Paths.get(rootDir.absolutePath, "db", ".scripts", "init_replica_key.sh")
        initReplicaKeyScriptFile.toFile().setLineSeparatorToLF()

        val initReplicasetScriptKey = Paths.get(rootDir.absolutePath, "db", ".scripts", "init_replicaset.sh")
        initReplicasetScriptKey.toFile().setLineSeparatorToLF()
        lineSeparatorsSet()
    }

    private fun createReplicaSetKey(rootDir: File): File? {
        val dbDir = Paths.get(rootDir.absolutePath, "db")
        val replicaKeyFile = Paths.get(dbDir.absolutePathString(), "key", MONGO_REPLICA_KEY_FILE).toFile()

        var createdReplicaKeyFile: File? = null
        if (replicaKeyFile.exists()) {
            showAttention(
                createReplicaKeyFileLabel,
                """Skipped: "${replicaKeyFile.invariantSeparatorsPath}" already exist."""
            )
            replicaKeyCreated = true
        } else {
            createdReplicaKeyFile = initReplicaKeyFile(replicaKeyFile)
            replicaKeyFileCreated()
        }

        //Refresh db dir
        val virtualSourceDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dbDir.toFile())
        virtualSourceDir?.refresh(true, true)

        return createdReplicaKeyFile
    }

    private fun initReplicaKeyFile(replicaKeyFile: File): File {
        val createdReplicaKeyFile = replicaKeyFile.create()

        val randomString = String.random()
        val replicaKey = Base64.getEncoder().encodeToString(randomString.toByteArray()).substring(0, 756)

        createdReplicaKeyFile.writeText(replicaKey)
        return createdReplicaKeyFile
    }

    private fun createEnvFiles() {
        runBlocking {
            val settings = VbProjectSettings.getInstance(project)
            val projectConfig = withContext(Dispatchers.IO) {
                val jwt = API.fetchToken() ?: return@withContext null
                return@withContext API.getProjectConfig(jwt, settings.projectId ?: return@withContext null)
            } ?: return@runBlocking

            val rootProject = rootDir ?: return@runBlocking

            delay(500)
            val mainEnvFile = EnvWriter.writeMainEnvFile(rootProject, projectConfig)
            if (mainEnvFile == null) {
                showAttention(createMainEnvLabel, """Skipped: "./backend/.env" already exist.""")
                backendEnvCreated = true
            } else {
                showMainEnvCreated()
            }

            delay(500)
            val dbEnvFile = EnvWriter.writeDbEnvFile(rootProject, projectConfig)
            if (dbEnvFile == null) {
                showAttention(createDbEnvLabel, """Skipped: "./db/.env" already exist.""")
                dbEnvCreated = true
            } else {
                showDbEnvCreated()
            }

            delay(500)
            val httpEnvFile = EnvWriter.httpEnvFile(rootProject, projectConfig)
            if (httpEnvFile == null) {
                showAttention(createHttpEnvLabel, """Skipped: "./backend/http/http-client.env.json" already exist.""")
                httpEnvCreated = true
            } else {
                showHttpEnvCreated()
            }
        }
    }

    private fun installNpmDependencies() {
        runBlocking {
            val isWindows = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win");
            val npmCommand = if(isWindows) "npm.cmd" else "npm"

            val processBuilder = ProcessBuilder(npmCommand, "install")
            processBuilder.directory(backendDir.toFile())

            val result = processBuilder.start().waitFor()
            if(result == 0){
                showNPMDepencenciesInstalled()
            }else{
                showError(dependenciesInstalledLabel, "Failed to install NPM dependencies.")
            }

            //Refresh backendDir dir
            val virtualSourceDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(backendDir.toFile())
            virtualSourceDir?.refresh(true, false)
        }
    }

    private fun startDatabase() {
        runBlocking {
            val processBuilder = ProcessBuilder("docker-compose", "up")
            processBuilder.directory(dbDir.toFile())

            val result = processBuilder.start().waitFor()
            if(result == 0){
                showNPMDepencenciesInstalled()
            }else{
                showError(dependenciesInstalledLabel, "Failed to install NPM dependencies.")
            }

            //Refresh backendDir dir
            val virtualSourceDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(backendDir.toFile())
            virtualSourceDir?.refresh(true, false)
        }
    }

}