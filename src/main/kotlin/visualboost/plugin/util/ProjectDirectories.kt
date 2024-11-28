package visualboost.plugin.util

import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

object ProjectDirectories {

    fun getProjectDir(project: Project): File {
        return File(project.basePath ?: throw NullPointerException("Missing project dir"))
    }

    fun getBackendDir(project: Project): File {
        val rootDir = getProjectDir(project)
        return Paths.get(rootDir.absolutePath, "backend").toFile()
    }

    fun getDbDir(project: Project): File {
        val rootDir = getProjectDir(project)
        return Paths.get(rootDir.absolutePath, "db").toFile()
    }

    fun findEnvFile(project: Project): File? {
        val dbDir = getDbDir(project)
        val walk = dbDir.walk(FileWalkDirection.TOP_DOWN)
        return walk.find { it.name == ".env" }
    }

    fun findDockerComposeFile(project: Project): File? {
        val composeFiles = findDockerComposeFiles(project)
        return composeFiles.firstOrNull()
    }

    fun findDockerComposeFiles(project: Project): List<File> {
        val dbDir = getDbDir(project) ?: return emptyList()
        val walk = dbDir.walk(FileWalkDirection.TOP_DOWN)
        return walk.filter { it.name == "docker-compose.yml" }.toList()
    }

    /**
     * Return the docker-compose file that contains the mongodb services (./db/docker-compose.yml) or null if the file does not exist
     */
    fun getDbComposeFile(project: Project): File? {
        val projectDir = getProjectDir(project)
        val dbDir = Paths.get(projectDir.absolutePath, "db")
        val composeFile = Paths.get(dbDir.absolutePathString(), "docker-compose.yml").toFile()
        if (composeFile.exists().not()) return null
        return composeFile
    }

    fun getAuthServiceComposeFile(project: Project): File {
        val composeFile = Paths.get(getAuthServiceDir(project).absolutePath, "docker-compose.yml").toFile()
        return composeFile
    }

    /**
     * Return the ./db/.env file or null if the file does not exist
     */
    fun getDbEnvFile(project: Project): File? {
        val projectDir = getProjectDir(project)
        val dbDir = Paths.get(projectDir.absolutePath, "db")
        val composeFile = Paths.get(dbDir.absolutePathString(), ".env").toFile()
        if (composeFile.exists().not()) return null
        return composeFile
    }

    fun getAuthServiceEnvFile(project: Project): File {
        return Paths.get(getAuthServiceDir(project).absolutePath, ".env").toFile()
    }

    fun getPackageJsonFile(project: Project): File? {
        val backendDir = getBackendDir(project) ?: return null
        val walk = backendDir.walk(FileWalkDirection.TOP_DOWN)
        return walk.onEnter { it.name != "node_modules" }.find { it.name == "package.json" }
    }

    fun getIdeaDir(project: Project): File? {
        val rootDir = getProjectDir(project) ?: return null
        val walk = rootDir.walk(FileWalkDirection.TOP_DOWN)
        return walk.find { it.name == ".idea" }
    }

    fun getSourceDir(project: Project): File? {
        val backendDir = getBackendDir(project) ?: return null
        return Paths.get(backendDir.absolutePath, "src").toFile()
    }

    fun getExtensionDir(project: Project, extensionDir: String?): File? {
        val sourceDir = getSourceDir(project) ?: return null
        return Paths.get(sourceDir.absolutePath, extensionDir ?: return null).toFile()
    }

    /**
     * Returns the path of the "service" dir.
     * This dir contains .e.g. the files for the authentication service
     */
    fun getServiceDir(project: Project): File {
        val projectDir = getProjectDir(project)
        return Paths.get(projectDir.absolutePath, "service").toFile()
    }

    /**
     * Returns the dir "./service/authentication"
     */
    fun getAuthServiceDir(project: Project): File {
        return Paths.get(getServiceDir(project).absolutePath, "authentication").toFile()
    }

    fun get(project: Project): File {
        return Paths.get(getServiceDir(project).absolutePath, "authentication").toFile()
    }
}
