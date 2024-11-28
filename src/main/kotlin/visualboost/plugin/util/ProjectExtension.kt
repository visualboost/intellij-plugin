package visualboost.plugin.util

import com.intellij.docker.DockerDeploymentConfiguration
import com.intellij.docker.DockerRunConfigurationCreator
import com.intellij.docker.deploymentSource.DockerComposeDeploymentSourceType
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.lang.javascript.buildTools.npm.rc.NpmCommand
import com.intellij.lang.javascript.buildTools.npm.rc.NpmConfigurationType
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunConfiguration
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunSettings
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.remoteServer.configuration.RemoteServersManager
import visualboost.plugin.icons.IconRes
import java.io.File
import javax.swing.Icon
import kotlin.io.path.invariantSeparatorsPathString


fun Project.showNotification(
    title: String,
    msg: String,
    type: NotificationType,
    icon: Icon? = null,
    actions: List<AnAction> = emptyList()
) {
    val notificationGroup = NotificationGroupManager.getInstance()
        .getNotificationGroup("visualboost.notification")
    if(notificationGroup == null) return

    val notification = notificationGroup.createNotification(title, msg, type)
        .setIcon(icon)

    actions.forEach { notification.addAction(it) }
    notification.notify(this)
}

fun Project.showError(title: String, msg: String, actions: List<AnAction> = emptyList()) {
    showNotification(title, msg, NotificationType.ERROR, actions = actions)
}

fun Project.showInfo(title: String, msg: String, actions: List<AnAction> = emptyList()) {
    showNotification(title, msg, NotificationType.INFORMATION, IconRes.CheckIcon, actions)
}

fun Project.showWarning(title: String, msg: String, actions: List<AnAction> = emptyList()) {
    showNotification(title, msg, NotificationType.WARNING, actions = actions)
}

/**
 * Create a docker run configuration
 *
 * @param name: Name of the run configuration
 * @param composeFile: The docker-compose.yml file
 * @param dotEnvFile: The .env file
 */
fun Project.createDockerConfiguration(name: String, composeFile: File, dotEnvFile: File): RunnerAndConfigurationSettings? {
    try {
        val deploymentSource = DockerComposeDeploymentSourceType.getInstance().singletonSource
        val dockerDeploymentConfiguration = DockerDeploymentConfiguration()
        dockerDeploymentConfiguration.envFilePath = dotEnvFile.absolutePath
        dockerDeploymentConfiguration.sourceFilePath = composeFile.absolutePath

        val server = RemoteServersManager.getInstance().servers.firstOrNull { it.name == "Docker" }
        if (server == null) {
            showInfo(
                "Missing Plugin",
                "<html>Can't create a new <b>Docker Configuration</b>.<br/>Install the Docker Plugin first.</html>",
                listOf(
                    ActionManager.getInstance().getAction("visualboost.plugin.actions.OpenDockerPluginWebsiteAction")
                )
            )
            return null
        }

        val configuration: RunnerAndConfigurationSettings = DockerRunConfigurationCreator(this).createConfiguration(
            deploymentSource,
            dockerDeploymentConfiguration,
            server
        )
        configuration.name = name

        val runManager = RunManager.getInstance(this)
        runManager.addConfiguration(configuration)
        return configuration
    } catch (e: Exception) {
        showError("Error", "Unexpected error during database run configuration creation.")
        return null
    }
}

fun Project.selectRunConfig(name: String){
    val runConfigToSelect = this.getDockerRunConfiguration(name) ?: return
    val runManager = RunManager.getInstance(this)
    runManager.selectedConfiguration = runConfigToSelect
}

fun Project.getDockerRunConfiguration(name: String): RunnerAndConfigurationSettings? {
    val runManager = RunManager.getInstance(this)
    return runManager.findConfigurationByName(name)
}

fun Project.getStartDatabaseConfigurationName(): String {
    return "Start Database"
}

fun Project.getStartApplicationOnceRunConfigName(): String {
    return "Start Application"
}

fun Project.getStartApplicationInDevModeRunConfigName(): String {
    return "Start Application (Deamon)"
}

fun Project.getStartAuthServiceConfigName(): String {
    return "Run Authentication Service"
}

fun Project.createNpmRunConfig(name: String, command: String) {
    try {
        val packageJsonPath = ProjectDirectories.getPackageJsonFile(this)
        if (packageJsonPath == null) {
            showError(
                "Missing package.json",
                "<html>Can't create a new <b>Run Configuration</b>.<br/>Missing file: <b>package.json</b> in project directory.</html>",
            )
            return
        }

        val runManager = RunManager.getInstance(this)
        val configurationType = ConfigurationTypeUtil.findConfigurationType(NpmConfigurationType::class.java)
        val configuration: RunnerAndConfigurationSettings =
            runManager.createConfiguration("Run", configurationType.configurationFactories[0])

        val runConfiguration = configuration.configuration as NpmRunConfiguration
        runConfiguration.name = name
        runConfiguration.runSettings = NpmRunSettings.builder()
            .setCommand(NpmCommand.RUN_SCRIPT)
            .setScriptNames(listOf(command))
            .setPackageJsonPath(packageJsonPath.absolutePath)
            .build()

        runManager.addConfiguration(configuration)
        runManager.selectedConfiguration = configuration
    } catch (e: Exception) {
        showError("Error", "Unexpected error during npm run configuration creation.")
    }
}

fun Project.isVbignoreFile(vFile: VirtualFile): Boolean {
    val projectDir = ProjectDirectories.getProjectDir(this) ?: return false
    val vbIgnoreFile = File(projectDir.absolutePath, ".vbignore")

    val vbIgnoreFilePath = vbIgnoreFile.invariantSeparatorsPath
    val vFilePath = vFile.toNioPath().invariantSeparatorsPathString

    return vbIgnoreFilePath == vFilePath
}

fun Project.getOrCreateVbIgnoreFile(): File {
    val projectDir = ProjectDirectories.getProjectDir(this)
        val vbIgnoreFile = File(projectDir.absolutePath, ".vbignore")
        if (!vbIgnoreFile.exists()) {
            vbIgnoreFile.createNewFile()
        }

        return vbIgnoreFile
}