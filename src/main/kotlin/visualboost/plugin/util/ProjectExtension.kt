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
import com.intellij.remoteServer.configuration.RemoteServersManager
import visualboost.plugin.icons.IconRes
import java.nio.file.Paths
import javax.swing.Icon
import kotlin.io.path.absolutePathString


fun Project.showNotification(
    title: String,
    msg: String,
    type: NotificationType,
    icon: Icon? = null,
    actions: List<AnAction> = emptyList()
) {
    val notification = NotificationGroupManager.getInstance()
        .getNotificationGroup("visualboost.notification")
        .createNotification(title, msg, type)
        .setIcon(icon)

    actions.forEach { notification.addAction(it) }

    notification.notify(this)
}

fun Project.showError(title: String, msg: String, actions: List<AnAction> = emptyList()) {
    showNotification(title, msg, NotificationType.ERROR, actions = actions)
}

fun Project.showInfo(title: String, msg: String) {
    showNotification(title, msg, NotificationType.INFORMATION, IconRes.CheckIcon)
}

fun Project.createDockerConfiguration() {
    try {
        val dockerComposeFile = ProjectDirectories.getDockerComposeFile(this)
        if (dockerComposeFile == null) {
            showError(
                "Missing File",
                "<html>Can't create <b>Docker Configuration</b>.<br/>Missing file: <b>docker-compose.yml</b> in project directory.</html>"
            )
            return
        }

        val dbEnvFile = ProjectDirectories.getDbEnvFile(this)
        if (dbEnvFile == null) {
            showError(
                "Missing Environment Variables",
                "<html>Can't create <b>Docker Configuration</b>.<br/>Missing file: <b>.env</b> in project directory.</html>"
            )
            return
        }

        val deploymentSource = DockerComposeDeploymentSourceType.getInstance().singletonSource
        val dockerDeploymentConfiguration = DockerDeploymentConfiguration()
        dockerDeploymentConfiguration.envFilePath = dbEnvFile.absolutePath
        dockerDeploymentConfiguration.sourceFilePath = dockerComposeFile.absolutePath

        val server = RemoteServersManager.getInstance().servers.firstOrNull { it.name == "Docker" }
        if (server == null) {
            showError(
                "Missing Plugin",
                "<html>Can't create a new <b>Docker Configuration</b>.<br/>Install the Docker Plugin first.</html>",
                listOf(ActionManager.getInstance().getAction("visualboost.plugin.actions.OpenDockerPluginWebsiteAction"))
            )
            return
        }

        val configuration: RunnerAndConfigurationSettings = DockerRunConfigurationCreator(this).createConfiguration(
            deploymentSource,
            dockerDeploymentConfiguration,
            server
        )
        configuration.name = this.getStartDatabaseConfigurationName()

        val runManager = RunManager.getInstance(this)
        runManager.addConfiguration(configuration)
        runManager.selectedConfiguration = configuration
    } catch (e: Exception) {
        showError("Error", "Unexpected error during database run configuration creation.")
    }
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

fun Project.createNpmRunConfig(name: String, command: String) {
    try {
        val packageJsonPath = ProjectDirectories.getPackageJsonFile(this)
        if(packageJsonPath == null){
            showError(
                "Missing package.json",
                "<html>Can't create a new <b>Run Configuration</b>.<br/>Missing file: <b>package.json</b> in project directory.</html>",
            )
            return
        }

        val runManager = RunManager.getInstance(this)
        val configurationType = ConfigurationTypeUtil.findConfigurationType(NpmConfigurationType::class.java)
        val configuration: RunnerAndConfigurationSettings = runManager.createConfiguration("Run", configurationType.configurationFactories[0])

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