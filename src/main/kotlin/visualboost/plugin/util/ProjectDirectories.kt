package visualboost.plugin.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import toFile
import visualboost.plugin.api.models.project.ProjectConfigResponseBody
import java.io.File
import java.nio.file.Paths

object ProjectDirectories {

    fun getProjectDir(project: Project): File? {
        return File(project.basePath ?: return null)
    }

    fun getBackendDir(project: Project): File? {
        val rootDir = getProjectDir(project) ?: return null
        return Paths.get(rootDir.absolutePath, "backend").toFile()
    }

    fun getDbDir(project: Project): File? {
        val rootDir = getProjectDir(project) ?: return null
        return Paths.get(rootDir.absolutePath, "db").toFile()
    }

    fun getDbEnvFile(project: Project): File? {
        val dbDir = getDbDir(project) ?: return null
        val walk = dbDir.walk(FileWalkDirection.TOP_DOWN)
        return walk.find { it.name == ".env" }
    }

    fun getDockerComposeFile(project: Project): File? {
        val dbDir = getDbDir(project) ?: return null
        val walk = dbDir.walk(FileWalkDirection.TOP_DOWN)
        return walk.find { it.name == "docker-compose.yml" }
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
}
